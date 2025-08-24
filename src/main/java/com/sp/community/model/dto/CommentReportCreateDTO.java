package com.sp.community.model.dto;

import com.sp.community.persistent.entity.CommentReportEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 댓글 신고 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CommentReportCreateDTO {

    /**
     * 신고할 댓글 ID
     */
    @NotNull(message = "댓글 ID는 필수입니다.")
    private Long commentId;

    /**
     * 신고자 ID
     */
    @Size(max = 50, message = "신고자 ID는 50자 이하로 입력해주세요.")
    private String reporterId;

    /**
     * 신고 분류
     */
    @NotNull(message = "신고 분류는 필수입니다.")
    private CommentReportEntity.ReportType reportType;

    /**
     * 신고 사유
     */
    @NotBlank(message = "신고 사유는 필수입니다.")
    @Size(min = 10, max = 1000, message = "신고 사유는 10자 이상 1000자 이하로 입력해주세요.")
    private String reason;

    /**
     * 추가 설명
     */
    @Size(max = 500, message = "추가 설명은 500자 이하로 입력해주세요.")
    private String additionalInfo;

    /**
     * DTO 검증
     */
    public void validate() {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 댓글 ID입니다.");
        }

        if (reason != null) {
            String trimmedReason = reason.trim();
            if (trimmedReason.length() < 10) {
                throw new IllegalArgumentException("신고 사유는 최소 10자 이상 입력해주세요.");
            }
        }

        if (reportType == null) {
            throw new IllegalArgumentException("신고 분류를 선택해주세요.");
        }
    }

    /**
     * 정리된 신고 사유 반환
     */
    public String getTrimmedReason() {
        return reason != null ? reason.trim() : "";
    }

    /**
     * 정리된 추가 설명 반환
     */
    public String getTrimmedAdditionalInfo() {
        return additionalInfo != null ? additionalInfo.trim() : null;
    }

    /**
     * 최종 신고 사유 (추가 설명 포함)
     */
    public String getFullReason() {
        StringBuilder fullReason = new StringBuilder(getTrimmedReason());

        String trimmedAdditional = getTrimmedAdditionalInfo();
        if (trimmedAdditional != null && !trimmedAdditional.isEmpty()) {
            fullReason.append("\n\n[추가 설명]\n").append(trimmedAdditional);
        }

        return fullReason.toString();
    }
}