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
 * 게시글 수정 요청 DTO (이미지 한 개 첨부 지원)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "newImageFile") // 파일 객체는 toString에서 제외
public class BoardUpdateDTO {

    /**
     * 게시글 ID
     */
    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long boardId;

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
     * 수정자 ID (작성자와 동일해야 함)
     */
    private Long editorId;

    /**
     * 새로 추가할 이미지 파일 (기존 이미지를 교체)
     */
    private MultipartFile newImageFile;

    /**
     * 기존 이미지 삭제 여부
     */
    @Builder.Default
    private Boolean deleteImage = false;

    /**
     * 게시글 카테고리 (선택사항)
     */
    @Size(max = 50, message = "카테고리는 50자 이하로 입력해주세요.")
    private String category;

    /**
     * 공지사항 여부 (관리자용)
     */
    private Boolean isNotice;

    /**
     * 임시저장 여부
     */
    private Boolean isDraft;

    /**
     * 댓글 허용 여부
     */
    private Boolean allowComments;

    /**
     * 비밀 게시글 여부
     */
    private Boolean isPrivate;

    /**
     * 비밀글 비밀번호 (비밀 게시글인 경우)
     */
    @Size(max = 20, message = "비밀번호는 20자 이하로 입력해주세요.")
    private String password;

    /**
     * 수정 사유 (선택사항)
     */
    @Size(max = 500, message = "수정 사유는 500자 이하로 입력해주세요.")
    private String editReason;

    /**
     * 게시글 태그 (선택사항)
     */
    private List<String> tags;

    /**
     * 제보 유형 (INCIDENTREPORT 카테고리일 때만 사용)
     */
    @Size(max = 50, message = "제보 유형은 50자 이하로 입력해주세요.")
    private String reportType;

    /**
     * 제보 위치 (INCIDENTREPORT 카테고리일 때만 사용)
     */
    @Size(max = 50, message = "제보 위치는 50자 이하로 입력해주세요.")
    private String reportLocation;

    // ============ 이미지 파일 관련 메서드 ============

    /**
     * 새 이미지 파일 첨부 여부 확인
     */
    public boolean hasNewImage() {
        return newImageFile != null && !newImageFile.isEmpty();
    }

    /**
     * 기존 이미지 삭제 여부 확인
     */
    public boolean isDeleteImage() {
        return deleteImage != null && deleteImage;
    }

    /**
     * 이미지 변경 사항 존재 여부 확인
     */
    public boolean hasImageChanges() {
        return hasNewImage() || isDeleteImage();
    }

    /**
     * 새 이미지 파일 반환
     */
    public MultipartFile getNewImageFile() {
        return newImageFile;
    }

    /**
     * 새 이미지 파일 유효성 확인
     */
    public boolean isValidNewImageFile() {
        if (!hasNewImage()) {
            return false;
        }

        // 파일 크기 확인 (10MB 이하)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (newImageFile.getSize() > maxSize) {
            return false;
        }

        // 파일 확장자 확인
        String originalFilename = newImageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        if (!allowedExtensions.contains(extension)) {
            return false;
        }

        // MIME 타입 확인
        String contentType = newImageFile.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 이미지 파일 정보 요약
     */
    public String getImageFileInfo() {
        if (!hasNewImage()) {
            return isDeleteImage() ? "기존 이미지 삭제" : "이미지 변경 없음";
        }

        String info = String.format("새 이미지: %s (%.2fMB, %s)",
                newImageFile.getOriginalFilename(),
                newImageFile.getSize() / (1024.0 * 1024.0),
                newImageFile.getContentType());

        if (isDeleteImage()) {
            info += " + 기존 이미지 삭제";
        }

        return info;
    }

    // ============ 기존 기능 관련 메서드 ============

    /**
     * 비밀글 여부 확인
     */
    public boolean isSecretPost() {
        return isPrivate != null && isPrivate && password != null && !password.trim().isEmpty();
    }

    /**
     * 내용 미리보기 (최대 100자)
     */
    public String getContentPreview() {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String plainText = content.replaceAll("<[^>]*>", "");
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
     * 수정 사유 정리
     */
    public String getTrimmedEditReason() {
        return editReason != null ? editReason.trim() : null;
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

    public String getTrimmedReportType() {
        return reportType != null ? reportType.trim() : null;
    }

    public String getTrimmedReportLocation() {
        return reportLocation != null ? reportLocation.trim() : null;
    }

    public boolean isIncidentReportCategory() {
        return "INCIDENTREPORT".equals(getNormalizedCategory());
    }

    // ============ 검증 메서드 ============

    /**
     * DTO 검증
     */
    public void validate() {
        // 게시글 ID 검증
        if (boardId == null || boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글 ID입니다.");
        }

        // 비밀글 검증
        if (isSecretPost() && (password == null || password.trim().length() < 4)) {
            throw new IllegalArgumentException("비밀글의 경우 4자 이상의 비밀번호가 필요합니다.");
        }

        // 이미지 파일 검증
        if (hasNewImage() && !isValidNewImageFile()) {
            throw new IllegalArgumentException("유효하지 않은 이미지 파일입니다. " +
                    "JPG, JPEG, PNG, GIF, WEBP 형식의 10MB 이하 파일만 업로드 가능합니다.");
        }

        // 태그 검증
        if (tags != null && tags.size() > 10) {
            throw new IllegalArgumentException("태그는 최대 10개까지 입력 가능합니다.");
        }

        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && tag.trim().length() > 20) {
                    throw new IllegalArgumentException("각 태그는 20자 이하로 입력해주세요.");
                }
            }
        }

        // 카테고리 유효성 검증
        validateCategory();

        // 권한 검증 (공지사항 수정은 관리자만 가능)
        if (isNotice != null && isNotice) {
            validateNoticePermission();
        }

        // 논리적 일관성 검증
        if (isDeleteImage() && hasNewImage()) {
            // 기존 이미지 삭제 + 새 이미지 업로드는 허용 (교체)
            // 별도 검증 불필요
        }
    }

    /**
     * 카테고리 유효성 검증
     */
    private void validateCategory() {
        if (category == null || category.trim().isEmpty()) {
            return;
        }

        // 허용된 카테고리 목록
        List<String> allowedCategories = Arrays.asList(
                "general", "notice", "qna", "tech", "free", "review"
        );

        String normalizedCategory = getNormalizedCategory();
        if (!allowedCategories.contains(normalizedCategory)) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다. " +
                    "허용된 카테고리: " + String.join(", ", allowedCategories));
        }
    }

    /**
     * 공지사항 수정 권한 검증
     */
    private void validateNoticePermission() {
        if (editorId == null) {
            throw new IllegalArgumentException("공지사항 수정 권한이 없습니다.");
        }

        // TODO: 실제 관리자 권한 확인 로직 구현
        // 예: SecurityContext에서 사용자 권한 확인
    }

    // ============ 디버깅/로깅용 메서드 ============

    /**
     * DTO 요약 정보 (로깅용)
     */
    public String getSummary() {
        return String.format("BoardUpdateDTO{boardId=%d, title='%s', editorId='%s', category='%s', " +
                        "hasNewImage=%s, deleteImage=%s, isNotice=%s}",
                boardId,
                getTrimmedTitle(),
                editorId,
                getNormalizedCategory(),
                hasNewImage(),
                isDeleteImage(),
                isNotice);
    }

    /**
     * 이미지 변경 상태 문자열 반환
     */
    public String getImageChangeStatus() {
        if (!hasImageChanges()) {
            return "이미지 변경 없음";
        }

        if (hasNewImage() && isDeleteImage()) {
            return String.format("이미지 교체 (%s)", newImageFile.getOriginalFilename());
        } else if (hasNewImage()) {
            return String.format("이미지 추가 (%s)", newImageFile.getOriginalFilename());
        } else if (isDeleteImage()) {
            return "이미지 삭제";
        }

        return "이미지 변경 없음";
    }

    /**
     * 변경 사항 요약
     */
    public String getChangesSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("변경사항: ");

        if (hasImageChanges()) {
            summary.append("이미지 ").append(getImageChangeStatus()).append(", ");
        }

        if (category != null) {
            summary.append("카테고리 변경, ");
        }

        if (isNotice != null) {
            summary.append("공지사항 여부 변경, ");
        }

        if (tags != null && !tags.isEmpty()) {
            summary.append("태그 변경, ");
        }

        // 마지막 콤마 제거
        String result = summary.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }

        return result.equals("변경사항: ") ? "기본 정보만 변경" : result;
    }

    // ============ Deprecated 메서드 (하위 호환성) ============

    /**
     * @deprecated 이미지 한 개만 지원하므로 hasImageChanges() 사용 권장
     */
    @Deprecated
    public boolean hasFileChanges() {
        return hasImageChanges();
    }

    /**
     * @deprecated 이미지 한 개만 지원하므로 hasNewImage() 사용 권장
     */
    @Deprecated
    public boolean hasNewFiles() {
        return hasNewImage();
    }

    /**
     * @deprecated 이미지 한 개만 지원하므로 isDeleteImage() 사용 권장
     */
    @Deprecated
    public boolean hasDeleteFiles() {
        return isDeleteImage();
    }

    /**
     * @deprecated 이미지 한 개만 지원하므로 getNewImageFile() 사용 권장
     */
    @Deprecated
    public List<MultipartFile> getValidNewFiles() {
        if (hasNewImage()) {
            return List.of(newImageFile);
        }
        return List.of();
    }

    /**
     * @deprecated 이미지 한 개만 지원하므로 isDeleteImage() 사용 권장
     */
    @Deprecated
    public List<Long> getValidDeleteFileIds() {
        // 더미 구현 (하위 호환성을 위해)
        return List.of();
    }
}