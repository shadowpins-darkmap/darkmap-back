package com.sp.community.persistent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 게시글 신고 엔티티
 */
@Entity
@Table(name = "board_reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"board_id", "reporter_id"})
        },
        indexes = {
                @Index(name = "idx_board_reports_board_id", columnList = "board_id"),
                @Index(name = "idx_board_reports_reporter_id", columnList = "reporter_id"),
                @Index(name = "idx_board_reports_status", columnList = "status"),
                @Index(name = "idx_board_reports_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    /**
     * 게시글과의 연관관계 (다대일)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    /**
     * 신고한 사용자 ID
     */
    @Column(name = "reporter_id", nullable = false, length = 50)
    private String reporterId;

    /**
     * 신고한 사용자 닉네임
     */
    @Column(name = "reporter_nickname", nullable = false, length = 50)
    private String reporterNickname;

    /**
     * 신고 분류
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    /**
     * 신고 사유
     */
    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    /**
     * 신고 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /**
     * 신고 처리 결과
     */
    @Column(name = "result", length = 500)
    private String result;

    /**
     * 신고 처리자 ID
     */
    @Column(name = "processor_id", length = 50)
    private String processorId;

    /**
     * 신고 생성 일시
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 신고 수정 일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 신고 처리 완료 일시
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * 신고 분류 Enum
     */
    public enum ReportType {
        SPAM("스팸"),
        INAPPROPRIATE("부적절한 내용"),
        HARASSMENT("괴롭힘"),
        COPYRIGHT("저작권 침해"),
        FRAUD("사기"),
        HATE_SPEECH("혐오 표현"),
        VIOLENCE("폭력적 내용"),
        ADULT_CONTENT("성인 콘텐츠"),
        FAKE_NEWS("허위 정보"),
        OTHER("기타");

        private final String description;

        ReportType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 신고 상태 Enum
     */
    public enum ReportStatus {
        PENDING("처리 대기"),
        REVIEWING("검토 중"),
        APPROVED("승인됨"),
        REJECTED("거부됨"),
        CANCELLED("취소됨");

        private final String description;

        ReportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 엔티티 저장 전 실행
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReportStatus.PENDING;
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
     * 신고 승인 처리
     */
    public void approve(String processorId, String result) {
        this.status = ReportStatus.APPROVED;
        this.processorId = processorId;
        this.result = result;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 신고 거부 처리
     */
    public void reject(String processorId, String result) {
        this.status = ReportStatus.REJECTED;
        this.processorId = processorId;
        this.result = result;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 신고 취소
     */
    public void cancel() {
        this.status = ReportStatus.CANCELLED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 신고 검토 상태로 변경
     */
    public void startReview(String processorId) {
        this.status = ReportStatus.REVIEWING;
        this.processorId = processorId;
    }

    /**
     * 신고 처리 완료 여부 확인
     */
    public boolean isProcessed() {
        return this.status == ReportStatus.APPROVED ||
                this.status == ReportStatus.REJECTED ||
                this.status == ReportStatus.CANCELLED;
    }

    /**
     * 신고 처리 대기 중인지 확인
     */
    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }

    /**
     * 신고 검토 중인지 확인
     */
    public boolean isReviewing() {
        return this.status == ReportStatus.REVIEWING;
    }

    /**
     * 편의 메서드: 게시글과 연관관계 설정
     */
    public void setBoard(BoardEntity board) {
        this.board = board;
        if (board != null && !board.getReports().contains(this)) {
            board.getReports().add(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardReportEntity)) return false;

        BoardReportEntity that = (BoardReportEntity) o;
        return reportId != null && reportId.equals(that.reportId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BoardReportEntity{" +
                "reportId=" + reportId +
                ", reporterId='" + reporterId + '\'' +
                ", reporterNickname='" + reporterNickname + '\'' +
                ", reportType=" + reportType +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}