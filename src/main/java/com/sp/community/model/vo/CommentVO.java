package com.sp.community.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 댓글 응답 VO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CommentVO {

    /**
     * 댓글 ID
     */
    private Long commentId;

    /**
     * 게시글 ID
     */
    private Long boardId;

    /**
     * 댓글 내용
     */
    private String content;

    /**
     * 작성자 ID
     */
    private Long authorId;

    /**
     * 작성자 닉네임
     */
    private String authorNickname;

    /**
     * 좋아요 수
     */
    private Integer likeCount;

    /**
     * 댓글 생성 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 댓글 수정 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 현재 사용자의 좋아요 여부
     */
    private Boolean isLiked;

    /**
     * 현재 사용자가 작성자인지 여부
     */
    private Boolean isAuthor;

    /**
     * 댓글 신고 여부
     */
    private Boolean isReported;

    /**
     * 댓글 숨김 여부
     */
    private Boolean isHidden;

    /**
     * 댓글 상태
     */
    private CommentStatus status;

    /**
     * 댓글 상태 열거형
     */
    public enum CommentStatus {
        ACTIVE("활성"),
        REPORTED("신고됨"),
        HIDDEN("숨김"),
        DELETED("삭제됨");

        private final String description;

        CommentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 작성자 표시명 반환 (닉네임 우선)
     */
    public String getDisplayAuthorName() {
        if (authorNickname != null && !authorNickname.trim().isEmpty()) {
            return authorNickname;
        }
        return authorId != null ? authorId.toString() : "알 수 없음";
    }

    /**
     * 수정된 댓글인지 확인
     */
    public boolean isEdited() {
        return updatedAt != null &&
                createdAt != null &&
                !updatedAt.equals(createdAt);
    }

    /**
     * 최근 댓글인지 확인 (1시간 이내)
     */
    public boolean isRecent() {
        if (createdAt == null) {
            return false;
        }
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return createdAt.isAfter(oneHourAgo);
    }
}