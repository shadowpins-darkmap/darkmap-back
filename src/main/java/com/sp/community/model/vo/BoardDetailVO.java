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
 * 게시글 상세 응답 VO (이미지 한 개 첨부 지원)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoardDetailVO {

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
     * 이미지 첨부 여부
     */
    private Boolean hasImage;

    /**
     * 이미지 URL
     */
    private String imageUrl;

    /**
     * 이미지 파일명
     */
    private String imageFileName;

    /**
     * 이미지 파일 크기 (bytes)
     */
    private Long imageFileSize;

    /**
     * 공지사항 여부
     */
    private Boolean isNotice;

    /**
     * 신고된 게시글 여부
     */
    private Boolean isReported;

    /**
     * 댓글 허용 여부
     */
    @Builder.Default
    private Boolean allowComments = true;

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
     * 게시글 상태 열거형
     */
    public enum BoardStatus {
        ACTIVE("활성"),
        REPORTED("신고됨"),
        DELETED("삭제됨"),
        DRAFT("임시저장");

        private final String description;

        BoardStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ============ 이미지 관련 메서드 ============

    /**
     * 이미지 첨부 여부 확인
     */
    public boolean hasImage() {
        return Boolean.TRUE.equals(hasImage) && imageUrl != null && !imageUrl.trim().isEmpty();
    }

    /**
     * 이미지 URL 반환 (안전한 접근)
     */
    public String getImageUrl() {
        return hasImage() ? imageUrl : null;
    }

    /**
     * 이미지 파일명 반환 (안전한 접근)
     */
    public String getImageFileName() {
        return hasImage() ? imageFileName : null;
    }

    /**
     * 이미지 파일 크기 반환 (안전한 접근)
     */
    public Long getImageFileSize() {
        return hasImage() ? imageFileSize : null;
    }

    /**
     * 이미지 파일 크기를 읽기 쉬운 형태로 반환
     */
    public String getFormattedImageFileSize() {
        if (!hasImage() || imageFileSize == null) {
            return null;
        }

        if (imageFileSize < 1024) {
            return imageFileSize + " B";
        } else if (imageFileSize < 1024 * 1024) {
            return String.format("%.1f KB", imageFileSize / 1024.0);
        } else {
            return String.format("%.1f MB", imageFileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * 이미지 정보 요약
     */
    public String getImageSummary() {
        if (!hasImage()) {
            return "첨부된 이미지 없음";
        }

        return String.format("%s (%s)",
                getImageFileName(),
                getFormattedImageFileSize());
    }

    // ============ 콘텐츠 관련 메서드 ============

    /**
     * 게시글 내용 길이 반환 (HTML 태그 제외)
     */
    public int getContentLength() {
        if (content == null) {
            return 0;
        }

        // HTML 태그 제거 후 길이 계산
        String plainText = content.replaceAll("<[^>]*>", "");
        return plainText.trim().length();
    }

    /**
     * 게시글 내용 미리보기 (HTML 태그 제거, 최대 200자)
     */
    public String getContentPreview() {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String plainText = content.replaceAll("<[^>]*>", "").trim();
        if (plainText.length() <= 200) {
            return plainText;
        }

        return plainText.substring(0, 200) + "...";
    }

    /**
     * 게시글이 긴 글인지 확인 (1000자 이상)
     */
    public boolean isLongContent() {
        return getContentLength() >= 1000;
    }

    // ============ 상태 관련 메서드 ============

    /**
     * 게시글 상태 자동 결정
     */
    public BoardStatus determineStatus() {
        if (Boolean.TRUE.equals(isReported)) {
            return BoardStatus.REPORTED;
        }
        return BoardStatus.ACTIVE;
    }

    /**
     * 게시글 상태 설정 (자동 결정)
     */
    public void setAutoStatus() {
        this.status = determineStatus();
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == BoardStatus.ACTIVE;
    }

    /**
     * 신고된 상태인지 확인
     */
    public boolean isReportedStatus() {
        return status == BoardStatus.REPORTED || Boolean.TRUE.equals(isReported);
    }

    // ============ 권한 관련 메서드 ============

    /**
     * 게시글 접근 권한 확인
     */
    public boolean canAccess(Long currentUserId, boolean isAdmin) {
        // 관리자는 모든 게시글 접근 가능
        if (isAdmin) {
            return true;
        }

        // 신고된 게시글은 작성자만 접근 가능
        if (isReportedStatus()) {
            return isAuthorOf(currentUserId);
        }

        return true;
    }

    /**
     * 게시글 수정 권한 확인
     */
    public boolean canEdit(Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }

        return isAuthorOf(currentUserId);
    }

    /**
     * 게시글 삭제 권한 확인
     */
    public boolean canDelete(Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }

        return isAuthorOf(currentUserId);
    }

    /**
     * 좋아요 가능 여부 확인
     */
    public boolean canLike(Long currentUserId) {
        // 로그인한 사용자만 좋아요 가능
        if (currentUserId == null) {
            return false;
        }

        // 자신의 게시글에는 좋아요 불가
        return !isAuthorOf(currentUserId);
    }

    /**
     * 댓글 작성 가능 여부 확인
     */
    public boolean canComment(Long currentUserId) {
        // 댓글이 허용되지 않은 경우
        if (!Boolean.TRUE.equals(allowComments)) {
            return false;
        }

        // 로그인한 사용자만 댓글 작성 가능
        if (currentUserId == null) {
            return false;
        }

        // 신고된 게시글에는 댓글 작성 불가 (작성자 제외)
        if (isReportedStatus() && !isAuthorOf(currentUserId)) {
            return false;
        }

        return true;
    }

    /**
     * 특정 사용자가 작성자인지 확인
     */
    public boolean isAuthorOf(Long userId) {
        return authorId != null && authorId.equals(userId);
    }


    // ============ 통계 관련 메서드 ============

    /**
     * 인기 게시글 여부 확인 (좋아요 10개 이상 또는 조회수 100 이상)
     */
    public boolean isPopular() {
        return (likeCount != null && likeCount >= 10) ||
                (viewCount != null && viewCount >= 100);
    }

    /**
     * 최근 게시글 여부 확인 (24시간 이내)
     */
    public boolean isRecent() {
        if (createdAt == null) {
            return false;
        }

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return createdAt.isAfter(oneDayAgo);
    }

    /**
     * 수정된 게시글 여부 확인
     */
    public boolean isEdited() {
        return updatedAt != null &&
                createdAt != null &&
                !updatedAt.equals(createdAt);
    }

    // ============ 디스플레이 관련 메서드 ============

    /**
     * 작성자 표시명 반환 (닉네임 우선, 없으면 ID)
     */
    public String getDisplayAuthorName() {
        if (authorNickname != null && !authorNickname.trim().isEmpty()) {
            return authorNickname;
        }
        return authorId != null ? authorId.toString() : "알 수 없음";
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


    // 카테고리 표시명 반환
    public String getDisplayCategory() {
        if (category == null || category.trim().isEmpty()) {
            return "일반";
        }

        return switch (category.toLowerCase()) {
            case "notice" -> "공지사항";
            case "memory" -> "기억";
            case "worry" -> "고민";
            case "ask" -> "질문";
            case "incidentreport" -> "사건제보";  // 추가
            case "etc" -> "미분류";
            default -> category;
        };
    }

    /**
     * 게시글 요약 정보 (목록 표시용)
     */
    public String getSummaryInfo() {
        return String.format("조회 %d | 좋아요 %d | 댓글 %d%s",
                viewCount != null ? viewCount : 0,
                likeCount != null ? likeCount : 0,
                commentCount != null ? commentCount : 0,
                hasImage() ? " | 이미지" : "");
    }

    // ============ Deprecated 메서드 (하위 호환성) ============

    /**
     * @deprecated hasImage() 사용 권장
     */
    @Deprecated
    public boolean hasFiles() {
        return hasImage();
    }

    /**
     * @deprecated hasImage() 사용 권장
     */
    @Deprecated
    public boolean hasImages() {
        return hasImage();
    }

    /**
     * @deprecated getImageUrl() 사용 권장
     */
    @Deprecated
    public String getFirstImageUrl() {
        return getImageUrl();
    }
}
