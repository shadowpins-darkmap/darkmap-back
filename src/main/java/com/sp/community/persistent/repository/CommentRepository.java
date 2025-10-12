package com.sp.community.persistent.repository;

import com.sp.community.model.vo.CommentVO;
import com.sp.community.persistent.entity.CommentEntity;
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
 * 댓글 Repository
 */
@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    /**
     * 특정 사용자의 게시글에 달린 새 댓글 수 조회 (특정 시간 이후)
     */
    @Query("""
        SELECT COUNT(c) 
        FROM CommentEntity c 
        JOIN c.board b 
        WHERE b.authorId = :authorId 
        AND c.createdAt >= :since 
        AND c.isDeleted = false 
        AND b.isDeleted = false
        """)
    Long countNewCommentsOnUserBoards(@Param("authorId") Long authorId,
                                      @Param("since") LocalDateTime since);

    /**
     * 특정 사용자의 게시글에 달린 새 댓글 목록 조회 (특정 시간 이후)
     */
    @Query("""
        SELECT c 
        FROM CommentEntity c 
        JOIN FETCH c.board b 
        WHERE b.authorId = :authorId 
        AND c.createdAt >= :since 
        AND c.isDeleted = false 
        AND b.isDeleted = false 
        ORDER BY c.createdAt DESC
        """)
    Page<CommentEntity> findNewCommentsOnUserBoards(@Param("authorId") Long authorId,
                                                    @Param("since") LocalDateTime since,
                                                    Pageable pageable);

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    @Query("SELECT c FROM CommentEntity c WHERE c.board.boardId = :boardId AND c.isDeleted = false AND c.isHidden = false ORDER BY c.createdAt DESC")
    Page<CommentEntity> findByBoardIdAndVisible(@Param("boardId") Long boardId, Pageable pageable);

    /**
     * 작성자별 댓글 목록 조회
     */
    @Query("SELECT c FROM CommentEntity c WHERE c.authorId = :authorId AND c.isDeleted = false AND c.isHidden = false ORDER BY c.createdAt DESC")
    Page<CommentEntity> findByAuthorIdAndVisible(@Param("authorId") Long authorId, Pageable pageable);

    /**
     * 특정 게시글의 댓글 수 조회 (보이는 댓글만)
     */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.board.boardId = :boardId AND c.isDeleted = false AND c.isHidden = false")
    Long countByBoardIdAndVisible(@Param("boardId") Long boardId);

    /**
     * 특정 기간 내 작성된 댓글 조회
     */
    @Query("SELECT c FROM CommentEntity c WHERE c.createdAt BETWEEN :startDate AND :endDate AND c.isDeleted = false AND c.isHidden = false ORDER BY c.createdAt DESC")
    Page<CommentEntity> findByCreatedAtBetweenAndVisible(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);


    /**
     * 인기 댓글 조회 (좋아요 수 기준)
     */
    @Query("SELECT c FROM CommentEntity c WHERE c.isDeleted = false AND c.isHidden = false AND c.likeCount >= :minLikes ORDER BY c.likeCount DESC, c.createdAt DESC")
    Page<CommentEntity> findPopularComments(@Param("minLikes") Integer minLikes, Pageable pageable);

    /**
     * 좋아요 수 증가
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.likeCount = c.likeCount + 1 WHERE c.commentId = :commentId")
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 좋아요 수 감소
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.likeCount = c.likeCount - 1 WHERE c.commentId = :commentId AND c.likeCount > 0")
    int decrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 신고 여부 업데이트
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.isReported = :isReported WHERE c.commentId = :commentId")
    int updateReportStatus(@Param("commentId") Long commentId, @Param("isReported") Boolean isReported);

    /**
     * 댓글 숨김 처리
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.isHidden = true, c.hiddenAt = CURRENT_TIMESTAMP, c.hiddenReason = :reason WHERE c.commentId = :commentId")
    int hideComment(@Param("commentId") Long commentId, @Param("reason") String reason);

    /**
     * 댓글 숨김 해제
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.isHidden = false, c.hiddenAt = null, c.hiddenReason = null WHERE c.commentId = :commentId")
    int unhideComment(@Param("commentId") Long commentId);

    /**
     * 작성자별 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.authorId = :authorId AND c.isDeleted = false")
    Long countByAuthorIdAndNotDeleted(@Param("authorId") Long authorId);

    /**
     * 오늘 작성된 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE DATE(c.createdAt) = CURRENT_DATE AND c.isDeleted = false")
    Long countTodayComments();

    /**
     * 최근 댓글 목록 조회
     */
    @Query("SELECT c FROM CommentEntity c WHERE c.isDeleted = false AND c.isHidden = false ORDER BY c.createdAt DESC")
    List<CommentEntity> findRecentComments(Pageable pageable);

    /**
     * 특정 게시글의 모든 댓글 소프트 삭제
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.isDeleted = true, c.deletedAt = CURRENT_TIMESTAMP WHERE c.board.boardId = :boardId AND c.isDeleted = false")
    int deleteAllByBoardId(@Param("boardId") Long boardId);

    /**
     * 특정 댓글 소프트 삭제
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.isDeleted = true, c.deletedAt = CURRENT_TIMESTAMP WHERE c.commentId = :commentId AND c.isDeleted = false")
    int deleteByCommentId(@Param("commentId") Long commentId);
}
