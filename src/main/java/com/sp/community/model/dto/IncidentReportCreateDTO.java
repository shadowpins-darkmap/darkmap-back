package com.sp.community.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 제보글 생성 요청 DTO (이미지 한 개 첨부 지원)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "imageFile") // 파일 객체는 toString에서 제외
public class IncidentReportCreateDTO {


    @Schema(description = "제보 유형")
    @Size(max = 65535, message = "제보 유형은 65,535자 이하로 입력해주세요.")
    private String reportType;

    @Schema(description = "제보 위치")
    @Size(max = 65535, message = "제보 위치는 65,535자 이하로 입력해주세요.")
    private String reportLocation;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(min = 1, max = 16777215, message = "내용은 1자 이상 16,777,215자 이하로 입력해주세요.")
    private String content;

    @Schema(description = "첨부 이미지 파일")
    private MultipartFile imageFile;

    @Schema(description = "뉴스기사 URL")
    @Size(max = 65535)
    private String url;

    @Schema(hidden = true)
    private Long reporterId;


    // ============ 이미지 파일 관련 메서드 ============

    public boolean hasAttachment() {
        return hasImage();
    }

    public MultipartFile getAttachmentFile() {
        return imageFile;
    }

    /**
     * 이미지 파일 첨부 여부 확인
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasImage() {
        return imageFile != null && !imageFile.isEmpty();
    }

    /**
     * 이미지 파일 반환
     */
    @JsonIgnore
    @Schema(hidden = true)
    public MultipartFile getImageFile() {
        return imageFile;
    }

    /**
     * 이미지 파일 유효성 확인
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidImageFile() {
        if (!hasImage()) {
            return false;
        }

        // 파일 크기 확인 (10MB 이하)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (imageFile.getSize() > maxSize) {
            return false;
        }

        // 파일 확장자 확인
        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        if (!allowedExtensions.contains(extension)) {
            return false;
        }

        // MIME 타입 확인
        String contentType = imageFile.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 이미지 파일 정보 요약
     */
    @JsonIgnore
    @Schema(hidden = true)
    public String getImageFileInfo() {
        if (!hasImage()) {
            return "첨부된 이미지 없음";
        }

        return String.format("파일명: %s, 크기: %.2fMB, 타입: %s",
                imageFile.getOriginalFilename(),
                imageFile.getSize() / (1024.0 * 1024.0),
                imageFile.getContentType());
    }

    // ============ 유틸리티 메서드 ============

    /**
     * 내용 미리보기 (최대 100자)
     */
    @JsonIgnore
    @Schema(hidden = true)
    public String getContentPreview() {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String plainText = content.replaceAll("<[^>]*>", ""); // HTML 태그 제거
        if (plainText.length() <= 100) {
            return plainText;
        }

        return plainText.substring(0, 100) + "...";
    }

    /**
     * 파일 확장자 추출
     */
    @JsonIgnore
    @Schema(hidden = true)
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // ============ 검증 메서드 ============

    /**
     * DTO 검증
     */
    @JsonIgnore
    @Schema(hidden = true)
    public void validate() {
        // 기본 필드 검증 (Bean Validation이 처리)

        // 이미지 파일 검증
        if (hasImage() && !isValidImageFile()) {
            throw new IllegalArgumentException("유효하지 않은 이미지 파일입니다. " +
                    "JPG, JPEG, PNG, GIF, WEBP 형식의 10MB 이하 파일만 업로드 가능합니다.");
        }

        if (reportType == null || reportType.trim().isEmpty()) {
            throw new IllegalArgumentException("제보 유형은 필수입니다.");
        }
        if (reportLocation == null || reportLocation.trim().isEmpty()) {
            throw new IllegalArgumentException("제보 위치는 필수입니다.");
        }
        if (reportType.length() > 65535) {
            throw new IllegalArgumentException("제보 유형은 65,535자 이하로 입력해주세요.");
        }
        if (reportLocation.length() > 65535) {
            throw new IllegalArgumentException("제보 위치는 65,535자 이하로 입력해주세요.");
        }
    }

    // ============ 디버깅/로깅용 메서드 ============

    /**
     * DTO 요약 정보 (로깅용)
     */
    @JsonIgnore
    @Schema(hidden = true)
    public String getSummary() {
        return String.format("IncidentReportCreateDTO{reporterId='%s', hasImage=%s}",
                reporterId,
                hasImage());
    }

    /**
     * 첨부 파일 포함 여부를 문자열로 반환
     */
    @JsonIgnore
    @Schema(hidden = true)
    public String getAttachmentStatus() {
        if (!hasImage()) {
            return "첨부 파일 없음";
        }
        return String.format("이미지 첨부됨 (%s)", imageFile.getOriginalFilename());
    }

    // ============ Deprecated 메서드 (하위 호환성) ============

    /**
     * @deprecated 이미지 한 개만 지원하므로 hasImage() 사용 권장
     */
    @JsonIgnore
    @Schema(hidden = true)
    @Deprecated
    public boolean hasFiles() {
        return hasImage();
    }

    /**
     * @deprecated 이미지 한 개만 지원하므로 getImageFile() 사용 권장
     */
    @JsonIgnore
    @Schema(hidden = true)
    @Deprecated
    public List<MultipartFile> getValidFiles() {
        if (hasImage()) {
            return List.of(imageFile);
        }
        return List.of();
    }
}