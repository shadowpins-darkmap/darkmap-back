package com.sp.community.persistent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 댓글 좋아요 엔티티
 */
@Entity
@Table(name = "comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_comment_likes_comment_id", columnList = "comment_id"),
                @Index(name = "idx_comment_likes_user_id", columnList = "user_id"),
                @Index(name = "idx_comment_likes_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    /**
     * 댓글과의 연관관계 (다대일)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommentEntity comment;

    /**
     * 좋아요를 누른 사용자 ID
     */
    @Column(name = "user_id", nullable = false, length = 50)
    private Long userId;

    /**
     * 좋아요를 누른 사용자 닉네임
     */
    //@Column(name = "user_nickname", nullable = false, length = 50)
    //private String userNickname;

    /**
     * 좋아요 생성 일시
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 좋아요 취소 여부 (소프트 삭제)
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 좋아요 취소 일시
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 엔티티 저장 전 실행
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    /**
     * 좋아요 취소 (소프트 삭제)
     */
    public void cancelLike() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 좋아요 복구
     */
    public void restoreLike() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    /**
     * 좋아요 활성 상태 확인
     */
    public boolean isActive() {
        return !this.isDeleted;
    }

    /**
     * 사용자가 좋아요를 눌렀는지 확인
     */
    public boolean isLikedByUser(Long userId) {
        return this.userId.equals(userId) && this.isActive();
    }

    /**
     * 편의 메서드: 댓글과 연관관계 설정
     */
    public void setComment(CommentEntity comment) {
        this.comment = comment;
        if (comment != null && !comment.getLikes().contains(this)) {
            comment.getLikes().add(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentLikeEntity)) return false;

        CommentLikeEntity that = (CommentLikeEntity) o;
        return likeId != null && likeId.equals(that.likeId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CommentLikeEntity{" +
                "likeId=" + likeId +
                ", userId='" + userId + '\'' +
                ", createdAt=" + createdAt +
                ", isDeleted=" + isDeleted +
                '}';
    }
}
