package com.sp.community.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sp.community.persistent.entity.CommentReportEntity;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 댓글 신고 응답 VO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CommentReportVO {

    /**
     * 신고 ID
     */
    private Long reportId;

    /**
     * 댓글 ID
     */
    private Long commentId;

    /**
     * 댓글 내용 (미리보기)
     */
    private String commentContent;

    /**
     * 댓글 작성자 ID
     */
    private Long commentAuthorId;

    /**
     * 댓글 작성자 닉네임
     */
    private String commentAuthorNickname;

    /**
     * 게시글 ID
     */
    private Long boardId;

    /**
     * 게시글 제목
     */
    private String boardTitle;

    /**
     * 신고자 ID
     */
    private Long reporterId;

    /**
     * 신고 분류
     */
    private CommentReportEntity.ReportType reportType;

    /**
     * 신고 분류 설명
     */
    private String reportTypeDescription;

    /**
     * 신고 사유
     */
    private String reason;

    /**
     * 신고 상태
     */
    private CommentReportEntity.ReportStatus status;

    /**
     * 신고 상태 설명
     */
    private String statusDescription;

    /**
     * 신고 처리 결과
     */
    private String result;

    /**
     * 신고 처리자 ID
     */
    private String processorId;

    /**
     * 신고 생성 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 신고 수정 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 신고 처리 완료 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    /**
     * 현재 사용자가 신고자인지 여부
     */
    private Boolean isReporter;

    /**
     * 처리 소요 시간 (시간 단위)
     */
    private Long processingHours;

    /**
     * 첨부파일 원본 이름
     */
    private String attachmentOriginalName;

    /**
     * 첨부파일 저장된 이름
     */
    private String attachmentStoredName;

    /**
     * 첨부파일 크기
     */
    private Long attachmentSize;

    /**
     * 첨부파일 MIME 타입
     */
    private String attachmentContentType;

    /**
     * 첨부파일 다운로드 URL
     */
    private String attachmentUrl;

    /**
     * 첨부파일 존재 여부
     */
    public boolean hasAttachment() {
        return attachmentStoredName != null && !attachmentStoredName.trim().isEmpty();
    }

    /**
     * 신고 처리 완료 여부
     */
    public boolean isProcessed() {
        return status == CommentReportEntity.ReportStatus.APPROVED ||
                status == CommentReportEntity.ReportStatus.REJECTED ||
                status == CommentReportEntity.ReportStatus.CANCELLED;
    }

    /**
     * 신고 처리 대기 중인지 확인
     */
    public boolean isPending() {
        return status == CommentReportEntity.ReportStatus.PENDING;
    }

    /**
     * 신고 검토 중인지 확인
     */
    public boolean isReviewing() {
        return status == CommentReportEntity.ReportStatus.REVIEWING;
    }

    /**
     * 승인된 신고인지 확인
     */
    public boolean isApproved() {
        return status == CommentReportEntity.ReportStatus.APPROVED;
    }

    /**
     * 거부된 신고인지 확인
     */
    public boolean isRejected() {
        return status == CommentReportEntity.ReportStatus.REJECTED;
    }

    /**
     * 취소된 신고인지 확인
     */
    public boolean isCancelled() {
        return status == CommentReportEntity.ReportStatus.CANCELLED;
    }
}