package com.sp.community.persistent.repository;

import com.sp.community.persistent.entity.BoardEntity;
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
 * 게시글 Repository
 */
@Repository
public interface BoardRepository extends JpaRepository<BoardEntity, Long> {

    /**
     * 삭제되지 않은 전체 게시글 수 조회
     */
    @Query("SELECT COUNT(b) FROM BoardEntity b WHERE b.isDeleted = false")
    Long countAllNotDeleted();

    /**
     * 특정 카테고리의 삭제되지 않은 게시글 수 조회
     */
    @Query("SELECT COUNT(b) FROM BoardEntity b WHERE b.category = :category AND b.isDeleted = false")
    Long countByCategoryAndNotDeleted(@Param("category") String category);

    /**
     * 삭제되지 않은 게시글 조회 (ID로)
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.boardId = :boardId AND b.isDeleted = false")
    Optional<BoardEntity> findByIdAndNotDeleted(@Param("boardId") Long boardId);

    /**
     * 삭제되지 않은 게시글 목록 조회 (페이징)
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<BoardEntity> findAllNotDeleted(Pageable pageable);

    /**
     * 작성자별 게시글 목록 조회
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.authorId = :authorId AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<BoardEntity> findByAuthorIdAndNotDeleted(@Param("authorId") Long authorId, Pageable pageable);

    /**
     * 제목으로 게시글 검색
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.title LIKE %:keyword% AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<BoardEntity> findByTitleContainingAndNotDeleted(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 내용으로 게시글 검색
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.content LIKE %:keyword% AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<BoardEntity> findByContentContainingAndNotDeleted(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 제목 또는 내용으로 게시글 검색
     */
    @Query("SELECT b FROM BoardEntity b WHERE (b.title LIKE %:keyword% OR b.content LIKE %:keyword%) AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<BoardEntity> findByTitleOrContentContainingAndNotDeleted(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 작성자 닉네임으로 게시글 검색 (Member 테이블 조인)
     */
    @Query("SELECT b FROM BoardEntity b " +
            "JOIN Member m ON b.authorId = m.id " +
            "WHERE m.nickname LIKE %:nickname% " +
            "AND b.isDeleted = false " +
            "ORDER BY b.createdAt DESC")
    Page<BoardEntity> findByAuthorNicknameContainingAndNotDeleted(@Param("nickname") String nickname, Pageable pageable);

    /**
     * 인기 게시글 조회 (좋아요 수 기준)
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isDeleted = false AND b.likeCount >= :minLikes ORDER BY b.likeCount DESC, b.createdAt DESC")
    Page<BoardEntity> findPopularBoards(@Param("minLikes") Integer minLikes, Pageable pageable);

    /**
     * 최근 인기 게시글 조회 (특정 기간 내 좋아요 수 기준)
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isDeleted = false AND b.createdAt >= :fromDate AND b.likeCount >= :minLikes ORDER BY b.likeCount DESC, b.createdAt DESC")
    Page<BoardEntity> findRecentPopularBoards(@Param("fromDate") LocalDateTime fromDate, @Param("minLikes") Integer minLikes, Pageable pageable);

    /**
     * 조회수가 높은 게시글 조회
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isDeleted = false ORDER BY b.viewCount DESC, b.createdAt DESC")
    Page<BoardEntity> findMostViewedBoards(Pageable pageable);

    /**
     * 신고된 게시글 조회
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isReported = true AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<BoardEntity> findReportedBoards(Pageable pageable);

    /**
     * 특정 기간 내 작성된 게시글 조회
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.createdAt BETWEEN :startDate AND :endDate AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<BoardEntity> findByCreatedAtBetweenAndNotDeleted(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * 조회수 증가
     */
    @Modifying
    @Query("UPDATE BoardEntity b SET b.viewCount = b.viewCount + 1 WHERE b.boardId = :boardId")
    int incrementViewCount(@Param("boardId") Long boardId);

    /**
     * 좋아요 수 증가
     */
    @Modifying
    @Query("UPDATE BoardEntity b SET b.likeCount = b.likeCount + 1 WHERE b.boardId = :boardId")
    int incrementLikeCount(@Param("boardId") Long boardId);

    /**
     * 좋아요 수 감소
     */
    @Modifying
    @Query("UPDATE BoardEntity b SET b.likeCount = b.likeCount - 1 WHERE b.boardId = :boardId AND b.likeCount > 0")
    int decrementLikeCount(@Param("boardId") Long boardId);

    /**
     * 댓글 수 증가
     */
    @Modifying
    @Query("UPDATE BoardEntity b SET b.commentCount = b.commentCount + 1 WHERE b.boardId = :boardId")
    int incrementCommentCount(@Param("boardId") Long boardId);

    /**
     * 댓글 수 감소
     */
    @Modifying
    @Query("UPDATE BoardEntity b SET b.commentCount = b.commentCount - 1 WHERE b.boardId = :boardId AND b.commentCount > 0")
    int decrementCommentCount(@Param("boardId") Long boardId);

    /**
     * 신고 여부 업데이트
     */
    @Modifying
    @Query("UPDATE BoardEntity b SET b.isReported = :isReported WHERE b.boardId = :boardId")
    int updateReportStatus(@Param("boardId") Long boardId, @Param("isReported") Boolean isReported);

    /**
     * 작성자별 게시글 수 조회
     */
    @Query("SELECT COUNT(b) FROM BoardEntity b WHERE b.authorId = :authorId AND b.isDeleted = false")
    Long countByAuthorIdAndNotDeleted(@Param("authorId") Long authorId);

    /**
     * 특정 유저가 작성한 승인된 제보글 개수 조회
     */
    @Query("SELECT COUNT(b) FROM BoardEntity b " +
            "WHERE b.authorId = :authorId " +
            "AND b.category = :category " +
            "AND b.reportApproved = true " +
            "AND b.isDeleted = false")
    Long countApprovedReportsByAuthor(@Param("authorId") Long authorId,
                                      @Param("category") String category);

    /**
     * 오늘 작성된 게시글 수 조회
     */
    @Query("SELECT COUNT(b) FROM BoardEntity b WHERE DATE(b.createdAt) = CURRENT_DATE AND b.isDeleted = false")
    Long countTodayBoards();

    /**
     * 특정 기간 내 작성된 게시글 수 조회
     */
    @Query("SELECT COUNT(b) FROM BoardEntity b WHERE b.createdAt BETWEEN :startDate AND :endDate AND b.isDeleted = false")
    Long countByCreatedAtBetweenAndNotDeleted(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 사용자가 좋아요한 게시글 목록 조회
     */
    @Query("SELECT DISTINCT b FROM BoardEntity b " +
            "JOIN BoardLikeEntity bl ON b.boardId = bl.board.boardId " +
            "WHERE bl.userId = :userId " +
            "AND bl.isDeleted = false " +
            "AND b.isDeleted = false " +
            "ORDER BY bl.createdAt DESC")
    Page<BoardEntity> findBoardsLikedByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 좋아요가 많은 상위 게시글 조회 (제한된 개수)
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isDeleted = false ORDER BY b.likeCount DESC, b.createdAt DESC")
    List<BoardEntity> findTopLikedBoards(Pageable pageable);

    /**
     * 최근 일주일 내 인기 게시글 조회
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isDeleted = false AND b.createdAt >= :oneWeekAgo ORDER BY b.likeCount DESC, b.viewCount DESC, b.createdAt DESC")
    Page<BoardEntity> findWeeklyPopularBoards(@Param("oneWeekAgo") LocalDateTime oneWeekAgo, Pageable pageable);

    /**
     * 댓글이 많은 게시글 조회
     */
    @Query("SELECT b FROM BoardEntity b WHERE b.isDeleted = false ORDER BY b.commentCount DESC, b.createdAt DESC")
    Page<BoardEntity> findMostCommentedBoards(Pageable pageable);

    /**
     * 검색 조건에 따른 게시글 조회 (복합 검색)
     */
    @Query("SELECT b FROM BoardEntity b WHERE " +
            "(:keyword IS NULL OR b.title LIKE %:keyword% OR b.content LIKE %:keyword%) AND " +
            "(:authorId IS NULL OR b.authorId = :authorId) AND " +
            "(:startDate IS NULL OR b.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR b.createdAt <= :endDate) AND " +
            "b.isDeleted = false " +
            "ORDER BY b.createdAt DESC")
    Page<BoardEntity> findBySearchConditions(
            @Param("keyword") String keyword,
            @Param("authorId") Long authorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 게시글 존재 여부 확인 (삭제되지 않은 게시글)
     */
    @Query("SELECT COUNT(b) > 0 FROM BoardEntity b WHERE b.boardId = :boardId AND b.isDeleted = false")
    boolean existsByIdAndNotDeleted(@Param("boardId") Long boardId);

    /**
     * 전체검색용
     * @param keyword
     * @return
     */
    @Query("SELECT b FROM BoardEntity b WHERE " +
            "(b.title LIKE %:keyword% OR b.content LIKE %:keyword%) " +
            "AND b.isDeleted = false " +
            "AND (b.category <> 'INCIDENTREPORT' OR b.reportApproved = true)")
    List<BoardEntity> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 최근 게시글 조회 (사건제보는 승인된 것만 포함)
     */
    @Query("SELECT b FROM BoardEntity b WHERE " +
            "b.isDeleted = false " +
            "AND (:category IS NULL OR b.category = :category) " +
            "AND (b.category <> 'INCIDENTREPORT' OR b.reportApproved = true) " +
            "ORDER BY b.createdAt DESC")
    Page<BoardEntity> findRecentBoards(@Param("category") String category, Pageable pageable);
}