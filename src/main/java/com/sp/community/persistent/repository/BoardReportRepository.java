package com.sp.community.persistent.repository;

import com.sp.community.persistent.entity.BoardReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 게시글 신고 Repository
 */
@Repository
public interface BoardReportRepository extends JpaRepository<BoardReportEntity, Long> {

    /**
     * 특정 게시글의 특정 사용자 신고 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.board.boardId = :boardId AND br.reporterId = :reporterId")
    Optional<BoardReportEntity> findByBoardIdAndReporterId(@Param("boardId") Long boardId, @Param("reporterId") String reporterId);

    /**
     * 특정 게시글의 신고 목록 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.board.boardId = :boardId ORDER BY br.createdAt DESC")
    Page<BoardReportEntity> findByBoardId(@Param("boardId") Long boardId, Pageable pageable);

    /**
     * 특정 사용자가 신고한 목록 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.reporterId = :reporterId ORDER BY br.createdAt DESC")
    Page<BoardReportEntity> findByReporterId(@Param("reporterId") String reporterId, Pageable pageable);

    /**
     * 신고 상태별 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.status = :status ORDER BY br.createdAt DESC")
    Page<BoardReportEntity> findByStatus(@Param("status") BoardReportEntity.ReportStatus status, Pageable pageable);

    /**
     * 신고 유형별 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.reportType = :reportType ORDER BY br.createdAt DESC")
    Page<BoardReportEntity> findByReportType(@Param("reportType") BoardReportEntity.ReportType reportType, Pageable pageable);

    /**
     * 처리 대기 중인 신고 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.status = 'PENDING' ORDER BY br.createdAt ASC")
    Page<BoardReportEntity> findPendingReports(Pageable pageable);

    /**
     * 검토 중인 신고 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.status = 'REVIEWING' ORDER BY br.createdAt ASC")
    Page<BoardReportEntity> findReviewingReports(Pageable pageable);

    /**
     * 처리 완료된 신고 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.status IN ('APPROVED', 'REJECTED', 'CANCELLED') ORDER BY br.processedAt DESC")
    Page<BoardReportEntity> findProcessedReports(Pageable pageable);

    /**
     * 특정 처리자가 처리한 신고 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.processorId = :processorId ORDER BY br.processedAt DESC")
    Page<BoardReportEntity> findByProcessorId(@Param("processorId") String processorId, Pageable pageable);

    /**
     * 특정 게시글의 신고 수 조회
     */
    @Query("SELECT COUNT(br) FROM BoardReportEntity br WHERE br.board.boardId = :boardId")
    Long countByBoardId(@Param("boardId") Long boardId);

    /**
     * 특정 게시글의 승인된 신고 수 조회
     */
    @Query("SELECT COUNT(br) FROM BoardReportEntity br WHERE br.board.boardId = :boardId AND br.status = 'APPROVED'")
    Long countApprovedReportsByBoardId(@Param("boardId") Long boardId);

    /**
     * 사용자가 이미 신고했는지 확인
     */
    @Query("SELECT COUNT(br) > 0 FROM BoardReportEntity br WHERE br.board.boardId = :boardId AND br.reporterId = :reporterId")
    boolean existsByBoardIdAndReporterId(@Param("boardId") Long boardId, @Param("reporterId") String reporterId);

    /**
     * 특정 기간 내 신고 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.createdAt BETWEEN :startDate AND :endDate ORDER BY br.createdAt DESC")
    Page<BoardReportEntity> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * 가장 많이 신고된 게시글 조회
     */
    @Query("SELECT br.board.boardId, COUNT(br) FROM BoardReportEntity br GROUP BY br.board.boardId ORDER BY COUNT(br) DESC")
    List<Object[]> findMostReportedBoards(Pageable pageable);

    /**
     * 신고 유형별 통계
     */
    @Query("SELECT br.reportType, COUNT(br) FROM BoardReportEntity br GROUP BY br.reportType ORDER BY COUNT(br) DESC")
    List<Object[]> findReportTypeStatistics();

    /**
     * 신고 상태별 통계
     */
    @Query("SELECT br.status, COUNT(br) FROM BoardReportEntity br GROUP BY br.status")
    List<Object[]> findReportStatusStatistics();

    /**
     * 오늘 신고 수 조회
     */
    @Query("SELECT COUNT(br) FROM BoardReportEntity br WHERE DATE(br.createdAt) = CURRENT_DATE")
    Long countTodayReports();

    /**
     * 특정 기간 내 신고 수 조회
     */
    @Query("SELECT COUNT(br) FROM BoardReportEntity br WHERE br.createdAt BETWEEN :startDate AND :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 처리 대기 시간이 긴 신고 조회 (3일 이상)
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.status = 'PENDING' AND br.createdAt <= :threeDaysAgo ORDER BY br.createdAt ASC")
    List<BoardReportEntity> findLongPendingReports(@Param("threeDaysAgo") LocalDateTime threeDaysAgo);

    /**
     * 신고 상태 일괄 업데이트
     */
    @Modifying
    @Query("UPDATE BoardReportEntity br SET br.status = :newStatus WHERE br.reportId IN :reportIds")
    int updateStatusByReportIds(@Param("reportIds") List<Long> reportIds, @Param("newStatus") BoardReportEntity.ReportStatus newStatus);

    /**
     * 신고 처리자 할당
     */
    @Modifying
    @Query("UPDATE BoardReportEntity br SET br.status = 'REVIEWING', br.processorId = :processorId WHERE br.reportId = :reportId")
    int assignProcessor(@Param("reportId") Long reportId, @Param("processorId") String processorId);

    /**
     * 특정 게시글의 모든 신고 취소
     */
    @Modifying
    @Query("UPDATE BoardReportEntity br SET br.status = 'CANCELLED' WHERE br.board.boardId = :boardId AND br.status = 'PENDING'")
    int cancelAllReportsByBoardId(@Param("boardId") Long boardId);

    /**
     * 복합 검색 조건으로 신고 조회
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE " +
            "(:reportType IS NULL OR br.reportType = :reportType) AND " +
            "(:status IS NULL OR br.status = :status) AND " +
            "(:reporterId IS NULL OR br.reporterId = :reporterId) AND " +
            "(:processorId IS NULL OR br.processorId = :processorId) AND " +
            "(:startDate IS NULL OR br.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR br.createdAt <= :endDate) " +
            "ORDER BY br.createdAt DESC")
    Page<BoardReportEntity> findBySearchConditions(
            @Param("reportType") BoardReportEntity.ReportType reportType,
            @Param("status") BoardReportEntity.ReportStatus status,
            @Param("reporterId") String reporterId,
            @Param("processorId") String processorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 월별 신고 통계
     */
    @Query("SELECT YEAR(br.createdAt), MONTH(br.createdAt), COUNT(br) FROM BoardReportEntity br " +
            "WHERE br.createdAt >= :startDate GROUP BY YEAR(br.createdAt), MONTH(br.createdAt) ORDER BY YEAR(br.createdAt), MONTH(br.createdAt)")
    List<Object[]> findMonthlyReportStatistics(@Param("startDate") LocalDateTime startDate);

    /**
     * 처리자별 처리 통계
     */
    @Query("SELECT br.processorId, br.status, COUNT(br) FROM BoardReportEntity br " +
            "WHERE br.processorId IS NOT NULL GROUP BY br.processorId, br.status ORDER BY br.processorId")
    List<Object[]> findProcessorStatistics();

    /**
     * 처리 대기 신고 수 조회
     */
    @Query("SELECT COUNT(br) FROM BoardReportEntity br WHERE br.status = 'PENDING'")
    Long countPendingReports();

    /**
     * 검토 중인 신고 수 조회
     */
    @Query("SELECT COUNT(br) FROM BoardReportEntity br WHERE br.status = 'REVIEWING'")
    Long countReviewingReports();

    /**
     * 처리 완료된 신고 수 조회
     */
    @Query("SELECT COUNT(br) FROM BoardReportEntity br WHERE br.status IN ('APPROVED', 'REJECTED', 'CANCELLED')")
    Long countProcessedReports();

    /**
     * 특정 신고자의 신고 통계
     */
    @Query("SELECT br.status, COUNT(br) FROM BoardReportEntity br WHERE br.reporterId = :reporterId GROUP BY br.status")
    List<Object[]> findReporterStatistics(@Param("reporterId") String reporterId);

    /**
     * 최근 신고 목록 조회 (관리자 대시보드용)
     */
    @Query("SELECT br FROM BoardReportEntity br WHERE br.status IN ('PENDING', 'REVIEWING') ORDER BY br.createdAt DESC")
    List<BoardReportEntity> findRecentReportsForAdmin(Pageable pageable);

    /**
     * 특정 게시글의 신고 유형별 통계
     */
    @Query("SELECT br.reportType, COUNT(br) FROM BoardReportEntity br WHERE br.board.boardId = :boardId GROUP BY br.reportType ORDER BY COUNT(br) DESC")
    List<Object[]> findReportTypeStatisticsByBoardId(@Param("boardId") Long boardId);
}