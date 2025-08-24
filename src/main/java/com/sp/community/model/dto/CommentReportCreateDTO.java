package com.sp.community.model.dto;

import com.sp.community.persistent.entity.CommentReportEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

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
     * 신고 첨부파일
     */
    private MultipartFile attachmentFile;

    /**
     * 첨부파일 존재 여부 확인
     */
    public boolean hasAttachment() {
        return attachmentFile != null && !attachmentFile.isEmpty();
    }

    /**
     * 첨부파일 유효성 검증
     */
    public boolean isValidAttachmentFile() {
        if (!hasAttachment()) {
            return true; // 첨부파일이 없어도 유효 (선택사항)
        }

        // 파일 크기 확인 (5MB 이하)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (attachmentFile.getSize() > maxSize) {
            return false;
        }

        // 파일 확장자 확인
        String originalFilename = attachmentFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx");
        return allowedExtensions.contains(extension);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

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

        // 첨부파일 검증 추가
        if (hasAttachment() && !isValidAttachmentFile()) {
            throw new IllegalArgumentException("유효하지 않은 첨부파일입니다. " +
                    "JPG, PNG, PDF, DOC, DOCX 형식의 5MB 이하 파일만 업로드 가능합니다.");
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