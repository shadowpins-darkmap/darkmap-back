package com.sp.community.persistent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 엔티티
 */
@Entity
@Table(name = "comments",
        indexes = {
                @Index(name = "idx_comments_board_id", columnList = "board_id"),
                @Index(name = "idx_comments_author_id", columnList = "author_id"),
                @Index(name = "idx_comments_created_at", columnList = "created_at"),
                @Index(name = "idx_comments_is_deleted", columnList = "is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    /**
     * 게시글과의 연관관계 (다대일)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    /**
     * 댓글 내용
     */
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /**
     * 댓글 작성자 ID
     */
    @Column(name = "author_id", nullable = false, length = 50)
    private String authorId;

    /**
     * 댓글 작성자 닉네임
     */
    @Column(name = "author_nickname", nullable = false, length = 50)
    private String authorNickname;

    /**
     * 추천 수
     */
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 댓글 좋아요 목록
     */
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentLikeEntity> likes = new ArrayList<>();

    /**
     * 댓글 신고 목록
     */
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentReportEntity> reports = new ArrayList<>();

    /**
     * 댓글 생성 일시
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 댓글 수정 일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 댓글 삭제 여부 (소프트 삭제)
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 댓글 삭제 일시
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 댓글 신고 여부
     */
    @Column(name = "is_reported", nullable = false)
    @Builder.Default
    private Boolean isReported = false;

    /**
     * 댓글 숨김 여부 (관리자에 의한 숨김)
     */
    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    /**
     * 댓글 숨김 일시
     */
    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    /**
     * 댓글 숨김 사유
     */
    @Column(name = "hidden_reason", length = 255)
    private String hiddenReason;

    /**
     * 엔티티 저장 전 실행
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.isReported == null) {
            this.isReported = false;
        }
        if (this.isHidden == null) {
            this.isHidden = false;
        }
        if (this.likeCount == null) {
            this.likeCount = 0;
        }
    }

    /**
     * 엔티티 수정 전 실행
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    public void deleteComment() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.content = "삭제된 댓글입니다.";
    }

    /**
     * 댓글 복구
     */
    public void restoreComment(String originalContent) {
        this.isDeleted = false;
        this.deletedAt = null;
        this.content = originalContent;
    }

    /**
     * 댓글 숨김 처리
     */
    public void hideComment(String reason) {
        this.isHidden = true;
        this.hiddenAt = LocalDateTime.now();
        this.hiddenReason = reason;
    }

    /**
     * 댓글 숨김 해제
     */
    public void unhideComment() {
        this.isHidden = false;
        this.hiddenAt = null;
        this.hiddenReason = null;
    }

    /**
     * 추천 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 추천 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 신고 여부 설정
     */
    public void setReported(boolean reported) {
        this.isReported = reported;
    }

    /**
     * 댓글 수정
     */
    public void updateContent(String newContent) {
        this.content = newContent;
    }

    /**
     * 댓글이 활성 상태인지 확인 (삭제되지 않고 숨겨지지 않음)
     */
    public boolean isActive() {
        return !this.isDeleted && !this.isHidden;
    }

    /**
     * 댓글을 볼 수 있는지 확인
     */
    public boolean isVisible() {
        return !this.isDeleted && !this.isHidden;
    }

    /**
     * 작성자가 본인인지 확인
     */
    public boolean isAuthor(String userId) {
        return this.authorId.equals(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentEntity)) return false;

        CommentEntity that = (CommentEntity) o;
        return commentId != null && commentId.equals(that.commentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CommentEntity{" +
                "commentId=" + commentId +
                ", content='" + content + '\'' +
                ", authorId='" + authorId + '\'' +
                ", authorNickname='" + authorNickname + '\'' +
                ", likeCount=" + likeCount +
                ", createdAt=" + createdAt +
                ", isDeleted=" + isDeleted +
                ", isHidden=" + isHidden +
                '}';
    }
}