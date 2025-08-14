package com.sp.exception;

/**
 * 파일 업로드 시 발생하는 예외
 */
public class FileUploadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 기본 생성자
     */
    public FileUploadException() {
        super("파일 업로드에 실패했습니다.");
    }

    /**
     * 메시지가 포함된 생성자
     *
     * @param message 예외 메시지
     */
    public FileUploadException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인이 포함된 생성자
     *
     * @param message 예외 메시지
     * @param cause 원인
     */
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 원인이 포함된 생성자
     *
     * @param cause 원인
     */
    public FileUploadException(Throwable cause) {
        super("파일 업로드에 실패했습니다.", cause);
    }

    /**
     * 파일 크기 초과 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @param fileSize 파일 크기
     * @param maxSize 최대 허용 크기
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException fileSizeExceeded(String fileName, long fileSize, long maxSize) {
        return new FileUploadException(
                String.format("파일 크기가 초과되었습니다. 파일명: %s, 크기: %d bytes, 최대 허용: %d bytes",
                        fileName, fileSize, maxSize));
    }

    /**
     * 지원하지 않는 파일 형식 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @param extension 파일 확장자
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException unsupportedFileType(String fileName, String extension) {
        return new FileUploadException(
                String.format("지원하지 않는 파일 형식입니다. 파일명: %s, 확장자: %s", fileName, extension));
    }

    /**
     * 빈 파일 업로드 시도 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException emptyFile(String fileName) {
        return new FileUploadException("빈 파일은 업로드할 수 없습니다. 파일명: " + fileName);
    }

    /**
     * 파일명 없음 예외 생성 정적 메서드
     *
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException noFileName() {
        return new FileUploadException("파일명이 없습니다.");
    }

    /**
     * 파일 저장 실패 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @param cause 원인
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException saveFailed(String fileName, Throwable cause) {
        return new FileUploadException("파일 저장에 실패했습니다. 파일명: " + fileName, cause);
    }

    /**
     * 디렉토리 생성 실패 예외 생성 정적 메서드
     *
     * @param directoryPath 디렉토리 경로
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException directoryCreationFailed(String directoryPath) {
        return new FileUploadException("디렉토리 생성에 실패했습니다. 경로: " + directoryPath);
    }

    /**
     * 파일 읽기 실패 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @param cause 원인
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException readFailed(String fileName, Throwable cause) {
        return new FileUploadException("파일 읽기에 실패했습니다. 파일명: " + fileName, cause);
    }

    /**
     * 파일 삭제 실패 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException deleteFailed(String fileName) {
        return new FileUploadException("파일 삭제에 실패했습니다. 파일명: " + fileName);
    }

    /**
     * 업로드 파일 수 제한 초과 예외 생성 정적 메서드
     *
     * @param fileCount 업로드 시도한 파일 수
     * @param maxCount 최대 허용 파일 수
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException fileCountExceeded(int fileCount, int maxCount) {
        return new FileUploadException(
                String.format("업로드 파일 수가 초과되었습니다. 업로드 시도: %d개, 최대 허용: %d개",
                        fileCount, maxCount));
    }

    /**
     * 중복 파일명 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException duplicateFileName(String fileName) {
        return new FileUploadException("중복된 파일명입니다. 파일명: " + fileName);
    }

    /**
     * 파일 경로가 유효하지 않음 예외 생성 정적 메서드
     *
     * @param filePath 파일 경로
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException invalidFilePath(String filePath) {
        return new FileUploadException("유효하지 않은 파일 경로입니다. 경로: " + filePath);
    }

    /**
     * 바이러스 검사 실패 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException virusScanFailed(String fileName) {
        return new FileUploadException("바이러스 검사에 실패했습니다. 파일명: " + fileName);
    }

    /**
     * 악성 파일 감지 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException maliciousFileDetected(String fileName) {
        return new FileUploadException("악성 파일이 감지되었습니다. 파일명: " + fileName);
    }

    /**
     * 스토리지 용량 부족 예외 생성 정적 메서드
     *
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException storageQuotaExceeded() {
        return new FileUploadException("스토리지 용량이 부족합니다.");
    }

    /**
     * 임시 파일 생성 실패 예외 생성 정적 메서드
     *
     * @param cause 원인
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException tempFileCreationFailed(Throwable cause) {
        return new FileUploadException("임시 파일 생성에 실패했습니다.", cause);
    }

    /**
     * AWS S3 업로드 실패 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @param cause 원인
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException s3UploadFailed(String fileName, Throwable cause) {
        return new FileUploadException("AWS S3 업로드에 실패했습니다. 파일명: " + fileName, cause);
    }

    /**
     * 파일 형식 검증 실패 예외 생성 정적 메서드
     *
     * @param fileName 파일명
     * @param reason 실패 사유
     * @return FileUploadException 인스턴스
     */
    public static FileUploadException fileValidationFailed(String fileName, String reason) {
        return new FileUploadException(
                String.format("파일 형식 검증에 실패했습니다. 파일명: %s, 사유: %s", fileName, reason));
    }
}