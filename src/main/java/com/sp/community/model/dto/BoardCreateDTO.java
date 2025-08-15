package com.sp.community.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 게시글 생성 요청 DTO (이미지 한 개 첨부 지원)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "imageFile") // 파일 객체는 toString에서 제외
public class BoardCreateDTO {

    /**
     * 게시글 제목
     */
    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
    private String title;

    /**
     * 게시글 내용
     */
    @NotBlank(message = "내용은 필수입니다.")
    @Size(min = 1, max = 10000, message = "내용은 1자 이상 10,000자 이하로 입력해주세요.")
    private String content;

    /**
     * 작성자 ID
     */
    @Size(max = 50, message = "")
    private String authorId;

    /**
     * 작성자 닉네임
     */
    @Size(max = 50, message = "")
    private String authorNickname;

    /**
     * 첨부 이미지 파일 (한 개만 가능)
     */
    private MultipartFile imageFile;

    /**
     * 게시글 카테고리 (선택사항)
     */
    @Size(max = 50, message = "카테고리는 50자 이하로 입력해주세요.")
    private String category;

    /**
     * 게시글 태그 (선택사항)
     */
    private List<String> tags;

    /**
     * 공지사항 여부 (관리자용)
     */
    @Builder.Default
    private Boolean isNotice = false;

    /**
     * 임시저장 여부
     */
    @Builder.Default
    private Boolean isDraft = false;

    /**
     * 댓글 허용 여부
     */
    @Builder.Default
    private Boolean allowComments = true;

    // ============ 이미지 파일 관련 메서드 ============

    /**
     * 이미지 파일 첨부 여부 확인
     */
    public boolean hasImage() {
        return imageFile != null && !imageFile.isEmpty();
    }

    /**
     * 이미지 파일 반환
     */
    public MultipartFile getImageFile() {
        return imageFile;
    }

    /**
     * 이미지 파일 유효성 확인
     */
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
     * 제목 정리 (앞뒤 공백 제거)
     */
    public String getTrimmedTitle() {
        return title != null ? title.trim() : "";
    }

    /**
     * 내용 정리 (앞뒤 공백 제거)
     */
    public String getTrimmedContent() {
        return content != null ? content.trim() : "";
    }

    /**
     * 카테고리 정리 (앞뒤 공백 제거, 소문자 변환)
     */
    public String getNormalizedCategory() {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        return category.trim().toLowerCase();
    }

    /**
     * 태그 정리 (공백 제거, 중복 제거, 소문자 변환)
     */
    public List<String> getNormalizedTags() {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim().toLowerCase())
                .distinct()
                .toList();
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

    // ============ 검증 메서드 ============

    /**
     * DTO 검증
     */
    public void validate() {
        // 기본 필드 검증 (Bean Validation이 처리)

        // 이미지 파일 검증
        if (hasImage() && !isValidImageFile()) {
            throw new IllegalArgumentException("유효하지 않은 이미지 파일입니다. " +
                    "JPG, JPEG, PNG, GIF, WEBP 형식의 10MB 이하 파일만 업로드 가능합니다.");
        }

        // 태그 개수 제한
        if (tags != null && tags.size() > 10) {
            throw new IllegalArgumentException("태그는 최대 10개까지 입력 가능합니다.");
        }

        // 태그 길이 제한
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && tag.trim().length() > 20) {
                    throw new IllegalArgumentException("각 태그는 20자 이하로 입력해주세요.");
                }
            }
        }

        // 권한 검증 (공지사항 등록은 관리자만 가능)
        if (isNotice != null && isNotice) {
            validateNoticePermission();
        }
    }

    /**
     * 공지사항 등록 권한 검증 (기본 구현)
     */
    private void validateNoticePermission() {
        // 실제 구현에서는 사용자의 권한을 확인해야 함
        // 여기서는 기본적인 검증만 수행
        if (authorId == null || authorId.trim().isEmpty()) {
            throw new IllegalArgumentException("공지사항 등록 권한이 없습니다.");
        }

        // TODO: 실제 관리자 권한 확인 로직 구현
        // 예: SecurityContext에서 사용자 권한 확인
        // if (!SecurityUtils.hasRole("ADMIN")) {
        //     throw new UnauthorizedException("공지사항 등록 권한이 없습니다.");
        // }
    }

    // ============ 디버깅/로깅용 메서드 ============

    /**
     * DTO 요약 정보 (로깅용)
     */
    public String getSummary() {
        return String.format("BoardCreateDTO{title='%s', authorId='%s', category='%s', hasImage=%s, isNotice=%s}",
                getTrimmedTitle(),
                authorId,
                getNormalizedCategory(),
                hasImage(),
                isNotice);
    }

    /**
     * 첨부 파일 포함 여부를 문자열로 반환
     */
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
    @Deprecated
    public boolean hasFiles() {
        return hasImage();
    }

    /**
     * @deprecated 이미지 한 개만 지원하므로 getImageFile() 사용 권장
     */
    @Deprecated
    public List<MultipartFile> getValidFiles() {
        if (hasImage()) {
            return List.of(imageFile);
        }
        return List.of();
    }
}