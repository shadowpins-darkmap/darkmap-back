package com.sp.community.persistent.repository;

import com.sp.community.persistent.entity.CommentReportEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sp.community.persistent.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 댓글 신고 Repository
 */
@Repository
public interface CommentReportRepository extends JpaRepository<CommentReportEntity, Long> {

    /**
     * 특정 댓글의 특정 사용자 신고 존재 여부 확인
     */
    @Query("SELECT COUNT(cr) > 0 FROM CommentReportEntity cr WHERE cr.comment.commentId = :commentId AND cr.reporterId = :reporterId")
    boolean existsByCommentIdAndReporterId(@Param("commentId") Long commentId, @Param("reporterId") Long reporterId);

    /**
     * 특정 댓글의 신고 목록 조회
     */
    @Query("SELECT cr FROM CommentReportEntity cr WHERE cr.comment.commentId = :commentId ORDER BY cr.createdAt DESC")
    Page<CommentReportEntity> findByCommentId(@Param("commentId") Long commentId, Pageable pageable);

    /**
     * 특정 사용자가 신고한 목록 조회
     */
    @Query("SELECT cr FROM CommentReportEntity cr WHERE cr.reporterId = :reporterId ORDER BY cr.createdAt DESC")
    Page<CommentReportEntity> findByReporterId(@Param("reporterId") Long reporterId, Pageable pageable);

    /**
     * 신고 상태별 조회
     */
    @Query("SELECT cr FROM CommentReportEntity cr WHERE cr.status = :status ORDER BY cr.createdAt DESC")
    Page<CommentReportEntity> findByStatus(@Param("status") CommentReportEntity.ReportStatus status, Pageable pageable);

    /**
     * 처리 대기 중인 신고 조회
     */
    @Query("SELECT cr FROM CommentReportEntity cr WHERE cr.status = 'PENDING' ORDER BY cr.createdAt ASC")
    Page<CommentReportEntity> findPendingReports(Pageable pageable);

    /**
     * 관리자 대시보드용 최근 신고 목록 조회
     */
    @Query("SELECT cr FROM CommentReportEntity cr WHERE cr.status IN ('PENDING', 'REVIEWING') ORDER BY cr.createdAt DESC")
    List<CommentReportEntity> findRecentReportsForAdmin(Pageable pageable);

    /**
     * 처리 대기 신고 수 조회
     */
    @Query("SELECT COUNT(cr) FROM CommentReportEntity cr WHERE cr.status = 'PENDING'")
    Long countPendingReports();

    /**
     * 검토 중인 신고 수 조회
     */
    @Query("SELECT COUNT(cr) FROM CommentReportEntity cr WHERE cr.status = 'REVIEWING'")
    Long countReviewingReports();

    /**
     * 처리 완료된 신고 수 조회
     */
    @Query("SELECT COUNT(cr) FROM CommentReportEntity cr WHERE cr.status IN ('APPROVED', 'REJECTED', 'CANCELLED')")
    Long countProcessedReports();

    /**
     * 오늘 신고 수 조회
     */
    @Query("SELECT COUNT(cr) FROM CommentReportEntity cr WHERE DATE(cr.createdAt) = CURRENT_DATE")
    Long countTodayReports();

    /**
     * 특정 댓글의 신고 수 조회
     */
    @Query("SELECT COUNT(cr) FROM CommentReportEntity cr WHERE cr.comment.commentId = :commentId")
    Long countByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 댓글의 승인된 신고 수 조회
     */
    @Query("SELECT COUNT(cr) FROM CommentReportEntity cr WHERE cr.comment.commentId = :commentId AND cr.status = 'APPROVED'")
    Long countApprovedReportsByCommentId(@Param("commentId") Long commentId);

    /**
     * 신고 유형별 통계
     */
    @Query("SELECT cr.reportType, COUNT(cr) FROM CommentReportEntity cr GROUP BY cr.reportType ORDER BY COUNT(cr) DESC")
    List<Object[]> findReportTypeStatistics();

    /**
     * 신고 상태별 통계
     */
    @Query("SELECT cr.status, COUNT(cr) FROM CommentReportEntity cr GROUP BY cr.status")
    List<Object[]> findReportStatusStatistics();
}
