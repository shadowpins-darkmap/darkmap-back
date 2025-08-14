package com.sp.community.persistent.repository;

import com.sp.community.persistent.entity.CommentLikeEntity;
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
 * 댓글 좋아요 Repository
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, Long> {

    /**
     * 특정 댓글의 특정 사용자 좋아요 조회 (활성 상태)
     */
    @Query("SELECT cl FROM CommentLikeEntity cl WHERE cl.comment.commentId = :commentId AND cl.userId = :userId AND cl.isDeleted = false")
    Optional<CommentLikeEntity> findByCommentIdAndUserIdAndNotDeleted(@Param("commentId") Long commentId, @Param("userId") String userId);

    /**
     * 특정 댓글의 특정 사용자 좋아요 조회 (삭제된 것 포함)
     */
    @Query("SELECT cl FROM CommentLikeEntity cl WHERE cl.comment.commentId = :commentId AND cl.userId = :userId")
    Optional<CommentLikeEntity> findByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") String userId);

    /**
     * 특정 댓글의 좋아요 수 조회 (활성 상태만)
     */
    @Query("SELECT COUNT(cl) FROM CommentLikeEntity cl WHERE cl.comment.commentId = :commentId AND cl.isDeleted = false")
    Long countByCommentIdAndNotDeleted(@Param("commentId") Long commentId);

    /**
     * 특정 댓글의 좋아요 목록 조회 (활성 상태, 페이징)
     */
    @Query("SELECT cl FROM CommentLikeEntity cl WHERE cl.comment.commentId = :commentId AND cl.isDeleted = false ORDER BY cl.createdAt DESC")
    Page<CommentLikeEntity> findByCommentIdAndNotDeleted(@Param("commentId") Long commentId, Pageable pageable);

    /**
     * 특정 사용자가 좋아요한 댓글 목록 조회 (활성 상태)
     */
    @Query("SELECT cl FROM CommentLikeEntity cl WHERE cl.userId = :userId AND cl.isDeleted = false ORDER BY cl.createdAt DESC")
    Page<CommentLikeEntity> findByUserIdAndNotDeleted(@Param("userId") String userId, Pageable pageable);
}