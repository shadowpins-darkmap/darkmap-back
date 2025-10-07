package com.sp.community.persistent.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_board")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"files", "likes", "reports", "comments"}) // 순환참조 방지
public class BoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false, length = 50)
    private String authorId;

    @Column(name = "author_nickname", nullable = false, length = 50)
    private String authorNickname;

    @Column(name = "category", nullable = false, length = 50)
    private String category;
    //공지 NOTICE
    //기억 MEMORY
    //고민 WORRY
    //질문 ASK
    //사건제보 INCIDENTREPORT
    //미분류 ETC

    // INCIDENTREPORT 카테고리 전용 필드들
    @Column(name = "report_type", length = 50)
    private String reportType; // 제보 유형 (INCIDENTREPORT 카테고리일 때만 사용)

    @Column(name = "report_location", length = 50)
    private String reportLocation; // 제보 위치 (INCIDENTREPORT 카테고리일 때만 사용)

    @Column(name = "report_url", length = 255)
    private String reportUrl; // 뉴스기사 (INCIDENTREPORT 카테고리일 때만 사용)

    @Builder.Default
    @Column(name = "report_approved", nullable = false, columnDefinition = "boolean default false")
    private Boolean reportApproved = false; // 제보 승인 여부 (INCIDENTREPORT 카테고리일 때만 사용)

    @Column(name = "report_approved_at")
    private LocalDateTime reportApprovedAt; // 제보 승인 시간 (INCIDENTREPORT 카테고리일 때만 사용)

    @Builder.Default
    @Column(name = "view_count", nullable = false, columnDefinition = "int default 0")
    private Integer viewCount = 0;

    @Builder.Default
    @Column(name = "like_count", nullable = false, columnDefinition = "int default 0")
    private Integer likeCount = 0;

    @Builder.Default
    @Column(name = "comment_count", nullable = false, columnDefinition = "int default 0")
    private Integer commentCount = 0;

    @Builder.Default
    @Column(name = "is_notice", nullable = false, columnDefinition = "boolean default false")
    private Boolean isNotice = false;

    @Builder.Default
    @Column(name = "is_reported", nullable = false, columnDefinition = "boolean default false")
    private Boolean isReported = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 연관관계 매핑
    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BoardFileEntity> files = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BoardLikeEntity> likes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BoardReportEntity> reports = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommentEntity> comments = new ArrayList<>();

    // 상수 정의 (카테고리)
    public static final String CATEGORY_NOTICE = "NOTICE";
    public static final String CATEGORY_MEMORY = "MEMORY";
    public static final String CATEGORY_WORRY = "WORRY";
    public static final String CATEGORY_ASK = "ASK";
    public static final String CATEGORY_INCIDENTREPORT = "INCIDENTREPORT";
    public static final String CATEGORY_ETC = "ETC";

    // JPA 콜백 메서드
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        if (this.likeCount == null) {
            this.likeCount = 0;
        }
        if (this.commentCount == null) {
            this.commentCount = 0;
        }
        if (this.isNotice == null) {
            this.isNotice = false;
        }
        if (this.isReported == null) {
            this.isReported = false;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.reportApproved == null) {
            this.reportApproved = false;
        }

        // INCIDENTREPORT 카테고리가 아닌 경우 제보 관련 필드 null 처리
        if (!CATEGORY_INCIDENTREPORT.equals(this.category)) {
            this.reportType = null;
            this.reportLocation = null;
            this.reportApproved = false;
            this.reportApprovedAt = null;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();

        // INCIDENTREPORT 카테고리가 아닌 경우 제보 관련 필드 null 처리
        if (!CATEGORY_INCIDENTREPORT.equals(this.category)) {
            this.reportType = null;
            this.reportLocation = null;
            this.reportApproved = false;
            this.reportApprovedAt = null;
        }
    }

    // 비즈니스 로직 메서드

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 댓글 수 증가
     */
    public void incrementCommentCount() {
        this.commentCount++;
    }

    /**
     * 댓글 수 감소
     */
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    /**
     * 게시글 전체 정보 수정 (제목, 내용, 카테고리)
     */
    public void updateBoard(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.updatedAt = LocalDateTime.now();

        // INCIDENTREPORT 카테고리가 아닌 경우 제보 관련 필드 null 처리
        if (!CATEGORY_INCIDENTREPORT.equals(category)) {
            this.reportType = null;
            this.reportLocation = null;
            this.reportApproved = false;
            this.reportApprovedAt = null;
        }
    }

    /**
     * 제보 게시글 전체 정보 수정 (제목, 내용, 카테고리, 제보 유형, 제보 위치)
     */
    public void updateReportBoard(String title, String content, String category, String reportType, String reportLocation) {
        this.title = title;
        this.content = content;
        this.category = category;

        if (CATEGORY_INCIDENTREPORT.equals(category)) {
            this.reportType = reportType;
            this.reportLocation = reportLocation;
        } else {
            this.reportType = null;
            this.reportLocation = null;
            this.reportApproved = false;
            this.reportApprovedAt = null;
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 게시글 내용 수정
     */
    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 게시글 카테고리 변경
     */
    public void updateCategory(String category) {
        this.category = category;
        this.updatedAt = LocalDateTime.now();

        // INCIDENTREPORT 카테고리가 아닌 경우 제보 관련 필드 null 처리
        if (!CATEGORY_INCIDENTREPORT.equals(category)) {
            this.reportType = null;
            this.reportLocation = null;
            this.reportApproved = false;
            this.reportApprovedAt = null;
        }
    }

    /**
     * 제보 정보 수정 (INCIDENTREPORT 카테고리일 때만)
     */
    public void updateReportInfo(String reportType, String reportLocation) {
        if (CATEGORY_INCIDENTREPORT.equals(this.category)) {
            this.reportType = reportType;
            this.reportLocation = reportLocation;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 제보 승인 (INCIDENTREPORT 카테고리일 때만)
     */
    public void approveReport(String approvedBy) {
        if (CATEGORY_INCIDENTREPORT.equals(this.category)) {
            this.reportApproved = true;
            this.reportApprovedAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 제보 승인 취소 (INCIDENTREPORT 카테고리일 때만)
     */
    public void disapproveReport() {
        if (CATEGORY_INCIDENTREPORT.equals(this.category)) {
            this.reportApproved = false;
            this.reportApprovedAt = null;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 신고 처리
     */
    public void markAsReported() {
        this.isReported = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 신고 해제
     */
    public void unmarkAsReported() {
        this.isReported = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 공지글로 설정
     */
    public void markAsNotice() {
        this.isNotice = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 공지글 해제
     */
    public void unmarkAsNotice() {
        this.isNotice = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 삭제 복원
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    // 연관관계 편의 메서드

    /**
     * 파일 추가
     */
    public void addFile(BoardFileEntity file) {
        if (files == null) {
            files = new ArrayList<>();
        }
        files.add(file);
        file.setBoard(this);
    }

    /**
     * 파일 제거
     */
    public void removeFile(BoardFileEntity file) {
        if (files != null) {
            files.remove(file);
            file.setBoard(null);
        }
    }

    /**
     * 좋아요 추가
     */
    public void addLike(BoardLikeEntity like) {
        if (likes == null) {
            likes = new ArrayList<>();
        }
        likes.add(like);
        like.setBoard(this);
        incrementLikeCount();
    }

    /**
     * 좋아요 제거
     */
    public void removeLike(BoardLikeEntity like) {
        if (likes != null) {
            likes.remove(like);
            like.setBoard(null);
            decrementLikeCount();
        }
    }

    /**
     * 신고 추가
     */
    public void addReport(BoardReportEntity report) {
        if (reports == null) {
            reports = new ArrayList<>();
        }
        reports.add(report);
        report.setBoard(this);
    }

    /**
     * 댓글 추가
     */
    public void addComment(CommentEntity comment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(comment);
        comment.setBoard(this);
        incrementCommentCount();
    }

    /**
     * 댓글 제거
     */
    public void removeComment(CommentEntity comment) {
        if (comments != null) {
            comments.remove(comment);
            comment.setBoard(null);
            decrementCommentCount();
        }
    }

    // 유틸리티 메서드

    /**
     * 게시글이 삭제되었는지 확인
     */
    public boolean isDeleted() {
        return this.isDeleted != null && this.isDeleted;
    }

    /**
     * 게시글이 공지글인지 확인
     */
    public boolean isNotice() {
        return this.isNotice != null && this.isNotice;
    }

    /**
     * 게시글이 신고된 상태인지 확인
     */
    public boolean isReported() {
        return this.isReported != null && this.isReported;
    }

    /**
     * 제보가 승인되었는지 확인
     */
    public boolean isReportApproved() {
        return this.reportApproved != null && this.reportApproved;
    }

    /**
     * 특정 사용자가 작성한 게시글인지 확인
     */
    public boolean isAuthor(String userId) {
        return this.authorId != null && this.authorId.equals(userId);
    }

    /**
     * 사건제보 카테고리인지 확인
     */
    public boolean isReportCategory() {
        return CATEGORY_INCIDENTREPORT.equals(this.category);
    }

    /**
     * 제보 정보가 완전한지 확인 (제보 카테고리이면서 유형과 위치가 모두 있는지)
     */
    public boolean hasCompleteReportInfo() {
        return isReportCategory() &&
                this.reportType != null && !this.reportType.trim().isEmpty() &&
                this.reportLocation != null && !this.reportLocation.trim().isEmpty();
    }

    /**
     * 제보가 승인 대기 상태인지 확인 (제보 카테고리이면서 아직 승인되지 않음)
     */
    public boolean isPendingApproval() {
        return isReportCategory() && !isReportApproved();
    }

    /**
     * 제보 승인 정보가 완전한지 확인 (승인되었고 승인시간이 있는지)
     */
    public boolean hasCompleteApprovalInfo() {
        return isReportApproved() && this.reportApprovedAt != null;
    }
}