package com.sp.community.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 기본 응답 VO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoardVO {

    /**
     * 게시글 ID
     */
    private Long boardId;

    /**
     * 게시글 제목
     */
    private String title;

    /**
     * 게시글 전체 내용
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
     * 작성자 탈퇴 여부
     */
    private Boolean authorDeleted;

    /**
     * 카테고리
     */
    private String category;

    /**
     * 조회수
     */
    private Integer viewCount;

    /**
     * 좋아요 수
     */
    private Integer likeCount;

    /**
     * 댓글 수
     */
    private Integer commentCount;

    /**
     * 공지사항 여부
     */
    private Boolean isNotice;

    /**
     * 신고된 게시글 여부
     */
    private Boolean isReported;

    private Boolean hasImage;
    private String imageUrl;

    /**
     * 게시글 생성 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 게시글 수정 일시
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
     * 게시글 상태
     */
    private BoardStatus status;

    /**
     * 게시글 상태 Enum
     */
    public enum BoardStatus {
        ACTIVE("활성"),
        DRAFT("임시저장"),
        REPORTED("신고됨"),
        HIDDEN("숨김"),
        DELETED("삭제됨");

        private final String description;

        BoardStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 제보 유형 (INCIDENTREPORT 카테고리인 경우)
     */
    private String reportType;

    /**
     * 제보 위치 (INCIDENTREPORT 카테고리인 경우)
     */
    private String reportLocation;

    /**
     * 뉴스 기사 주소 (INCIDENTREPORT 카테고리인 경우)
     */
    private String reportUrl;

    /**
     * INCIDENTREPORT 카테고리인지 확인
     */
    public boolean isIncidentReportCategory() {
        return "INCIDENTREPORT".equals(category);
    }

    /**
     * 제보 정보가 완전한지 확인
     */
    public boolean hasCompleteReportInfo() {
        return isIncidentReportCategory() &&
                reportType != null && !reportType.trim().isEmpty() &&
                reportLocation != null && !reportLocation.trim().isEmpty() &&
                reportUrl != null && !reportUrl.trim().isEmpty();
    }
}
