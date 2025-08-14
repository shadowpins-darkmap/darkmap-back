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
 * 게시글 첨부파일 정보 VO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoardFileVO {

    /**
     * 파일 ID
     */
    private Long fileId;

    /**
     * 원본 파일명
     */
    private String originalFileName;

    /**
     * 저장된 파일명 (보안상 클라이언트에 노출하지 않을 수도 있음)
     */
    private String storedFileName;

    /**
     * 파일 확장자
     */
    private String fileExtension;

    /**
     * 파일 크기 (바이트)
     */
    private Long fileSize;

    /**
     * 사람이 읽기 쉬운 파일 크기
     */
    private String formattedFileSize;

    /**
     * 파일 MIME 타입
     */
    private String mimeType;

    /**
     * 파일 접근 URL
     */
    private String fileUrl;

    /**
     * 파일 다운로드 URL
     */
    private String downloadUrl;

    /**
     * 파일 타입
     */
    private FileType fileType;

    /**
     * 썸네일 URL (이미지 파일인 경우)
     */
    private String thumbnailUrl;

    /**
     * 이미지 너비 (이미지 파일인 경우)
     */
    private Integer imageWidth;

    /**
     * 이미지 높이 (이미지 파일인 경우)
     */
    private Integer imageHeight;

    /**
     * 파일 순서
     */
    private Integer sortOrder;

    /**
     * 다운로드 횟수
     */
    private Integer downloadCount;

    /**
     * 파일 업로드 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 파일 수정 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 현재 사용자가 다운로드 권한이 있는지 여부
     */
    private Boolean canDownload;

    /**
     * 현재 사용자가 삭제 권한이 있는지 여부
     */
    private Boolean canDelete;

    /**
     * 파일 설명 (선택사항)
     */
    private String description;

    /**
     * 파일이 메인 이미지인지 여부 (대표 이미지)
     */
    private Boolean isMainImage;

    /**
     * 파일 상태
     */
    private FileStatus status;

    /**
     * 파일 타입 Enum
     */
    public enum FileType {
        IMAGE("이미지"),
        DOCUMENT("문서"),
        VIDEO("동영상"),
        AUDIO("음성"),
        ARCHIVE("압축파일"),
        OTHER("기타");

        private final String description;

        FileType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * CSS 클래스명 반환 (아이콘 표시용)
         */
        public String getCssClass() {
            return switch (this) {
                case IMAGE -> "file-image";
                case DOCUMENT -> "file-document";
                case VIDEO -> "file-video";
                case AUDIO -> "file-audio";
                case ARCHIVE -> "file-archive";
                default -> "file-other";
            };
        }

        /**
         * Font Awesome 아이콘 클래스 반환
         */
        public String getIconClass() {
            return switch (this) {
                case IMAGE -> "fas fa-file-image";
                case DOCUMENT -> "fas fa-file-alt";
                case VIDEO -> "fas fa-file-video";
                case AUDIO -> "fas fa-file-audio";
                case ARCHIVE -> "fas fa-file-archive";
                default -> "fas fa-file";
            };
        }
    }

    /**
     * 파일 상태 Enum
     */
    public enum FileStatus {
        ACTIVE("활성"),
        PROCESSING("처리중"),
        ERROR("오류"),
        DELETED("삭제됨");

        private final String description;

        FileStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 이미지 파일인지 확인
     */
    public boolean isImage() {
        return fileType == FileType.IMAGE;
    }

    /**
     * 비디오 파일인지 확인
     */
    public boolean isVideo() {
        return fileType == FileType.VIDEO;
    }

    /**
     * 오디오 파일인지 확인
     */
    public boolean isAudio() {
        return fileType == FileType.AUDIO;
    }

    /**
     * 문서 파일인지 확인
     */
    public boolean isDocument() {
        return fileType == FileType.DOCUMENT;
    }

    /**
     * 압축 파일인지 확인
     */
    public boolean isArchive() {
        return fileType == FileType.ARCHIVE;
    }

    /**
     * 미리보기 가능한 파일인지 확인
     */
    public boolean isPreviewable() {
        return isImage() || isVideo() || isAudio() || isPdf();
    }

    /**
     * PDF 파일인지 확인
     */
    public boolean isPdf() {
        return "pdf".equalsIgnoreCase(fileExtension) ||
                "application/pdf".equals(mimeType);
    }

    /**
     * 웹에서 직접 보기 가능한 이미지인지 확인
     */
    public boolean isWebImage() {
        if (!isImage()) {
            return false;
        }

        String ext = fileExtension != null ? fileExtension.toLowerCase() : "";
        return "jpg".equals(ext) || "jpeg".equals(ext) ||
                "png".equals(ext) || "gif".equals(ext) ||
                "webp".equals(ext) || "svg".equals(ext);
    }

    /**
     * 썸네일 존재 여부 확인
     */
    public boolean hasThumbnail() {
        return thumbnailUrl != null && !thumbnailUrl.trim().isEmpty();
    }

    /**
     * 이미지 크기 정보 존재 여부 확인
     */
    public boolean hasImageDimensions() {
        return imageWidth != null && imageHeight != null &&
                imageWidth > 0 && imageHeight > 0;
    }

    /**
     * 파일 크기를 사람이 읽기 쉬운 형태로 변환
     */
    public String formatFileSize() {
        if (fileSize == null || fileSize <= 0) {
            return "0 B";
        }

        long size = fileSize;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return size + " " + units[unitIndex];
        } else {
            return String.format("%.1f %s", (double) fileSize / Math.pow(1024, unitIndex), units[unitIndex]);
        }
    }

    /**
     * 이미지 비율 계산 (가로:세로)
     */
    public String getAspectRatio() {
        if (!hasImageDimensions()) {
            return null;
        }

        int gcd = gcd(imageWidth, imageHeight);
        int ratioWidth = imageWidth / gcd;
        int ratioHeight = imageHeight / gcd;

        return ratioWidth + ":" + ratioHeight;
    }

    /**
     * 최대공약수 계산 (유클리드 호제법)
     */
    private int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * 이미지 방향 판단
     */
    public String getImageOrientation() {
        if (!hasImageDimensions()) {
            return "unknown";
        }

        if (imageWidth > imageHeight) {
            return "landscape"; // 가로형
        } else if (imageHeight > imageWidth) {
            return "portrait";  // 세로형
        } else {
            return "square";    // 정사각형
        }
    }

    /**
     * 다운로드 횟수를 사람이 읽기 쉬운 형태로 변환
     */
    public String getFormattedDownloadCount() {
        if (downloadCount == null || downloadCount <= 0) {
            return "0";
        }

        if (downloadCount >= 1000000) {
            return String.format("%.1fM", downloadCount / 1000000.0);
        } else if (downloadCount >= 1000) {
            return String.format("%.1fK", downloadCount / 1000.0);
        } else {
            return downloadCount.toString();
        }
    }

    /**
     * 파일 업로드 경과 시간 (상대적 시간)
     */
    public String getUploadTimeAgo() {
        if (createdAt == null) {
            return "";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createdAt, now).toMinutes();

        if (minutes < 1) {
            return "방금 전";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (minutes < 1440) { // 24시간
            return (minutes / 60) + "시간 전";
        } else if (minutes < 10080) { // 7일
            return (minutes / 1440) + "일 전";
        } else {
            return createdAt.toLocalDate().toString();
        }
    }

    /**
     * 파일 정보 요약 문자열
     */
    public String getFileSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append(originalFileName);

        if (formattedFileSize != null) {
            summary.append(" (").append(formattedFileSize).append(")");
        }

        if (downloadCount != null && downloadCount > 0) {
            summary.append(" - ").append(getFormattedDownloadCount()).append(" 다운로드");
        }

        return summary.toString();
    }

    /**
     * 파일 아이콘 URL 반환 (파일 타입별)
     */
    public String getFileIconUrl() {
        String baseUrl = "/static/icons/files/";

        return switch (fileType) {
            case IMAGE -> baseUrl + "image.png";
            case DOCUMENT -> baseUrl + "document.png";
            case VIDEO -> baseUrl + "video.png";
            case AUDIO -> baseUrl + "audio.png";
            case ARCHIVE -> baseUrl + "archive.png";
            default -> baseUrl + "file.png";
        };
    }

    /**
     * 파일 색상 코드 반환 (UI 구분용)
     */
    public String getFileColor() {
        return switch (fileType) {
            case IMAGE -> "#4CAF50";      // 녹색
            case DOCUMENT -> "#2196F3";   // 파랑
            case VIDEO -> "#FF5722";      // 주황
            case AUDIO -> "#9C27B0";      // 보라
            case ARCHIVE -> "#607D8B";    // 회색
            default -> "#757575";         // 진한 회색
        };
    }
}