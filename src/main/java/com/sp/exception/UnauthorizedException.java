package com.sp.exception;

/**
 * 권한이 없을 때 발생하는 예외
 */
public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 기본 생성자
     */
    public UnauthorizedException() {
        super("권한이 없습니다.");
    }

    /**
     * 메시지가 포함된 생성자
     *
     * @param message 예외 메시지
     */
    public UnauthorizedException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인이 포함된 생성자
     *
     * @param message 예외 메시지
     * @param cause 원인
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 원인이 포함된 생성자
     *
     * @param cause 원인
     */
    public UnauthorizedException(Throwable cause) {
        super("권한이 없습니다.", cause);
    }

    /**
     * 게시글 수정 권한 없음 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @param userId 사용자 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException boardEdit(Long boardId, String userId) {
        return new UnauthorizedException(
                String.format("게시글을 수정할 권한이 없습니다. 게시글 ID: %d, 사용자 ID: %s", boardId, userId));
    }

    /**
     * 게시글 삭제 권한 없음 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @param userId 사용자 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException boardDelete(Long boardId, String userId) {
        return new UnauthorizedException(
                String.format("게시글을 삭제할 권한이 없습니다. 게시글 ID: %d, 사용자 ID: %s", boardId, userId));
    }

    /**
     * 댓글 수정 권한 없음 예외 생성 정적 메서드
     *
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException commentEdit(Long commentId, String userId) {
        return new UnauthorizedException(
                String.format("댓글을 수정할 권한이 없습니다. 댓글 ID: %d, 사용자 ID: %s", commentId, userId));
    }

    /**
     * 댓글 삭제 권한 없음 예외 생성 정적 메서드
     *
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException commentDelete(Long commentId, String userId) {
        return new UnauthorizedException(
                String.format("댓글을 삭제할 권한이 없습니다. 댓글 ID: %d, 사용자 ID: %s", commentId, userId));
    }

    /**
     * 게시글 접근 권한 없음 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException boardAccess(Long boardId) {
        return new UnauthorizedException("게시글에 접근할 권한이 없습니다. ID: " + boardId);
    }

    /**
     * 댓글 접근 권한 없음 예외 생성 정적 메서드
     *
     * @param commentId 댓글 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException commentAccess(Long commentId) {
        return new UnauthorizedException("댓글에 접근할 권한이 없습니다. ID: " + commentId);
    }

    /**
     * 관리자 권한 필요 예외 생성 정적 메서드
     *
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException adminRequired() {
        return new UnauthorizedException("관리자 권한이 필요합니다.");
    }

    /**
     * 본인 확인 실패 예외 생성 정적 메서드
     *
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException ownershipRequired() {
        return new UnauthorizedException("본인만 접근할 수 있습니다.");
    }

    /**
     * 로그인 필요 예외 생성 정적 메서드
     *
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException loginRequired() {
        return new UnauthorizedException("로그인이 필요합니다.");
    }

    /**
     * 계정 비활성화 예외 생성 정적 메서드
     *
     * @param userId 사용자 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException accountDisabled(String userId) {
        return new UnauthorizedException("비활성화된 계정입니다. 사용자 ID: " + userId);
    }

    /**
     * 계정 잠금 예외 생성 정적 메서드
     *
     * @param userId 사용자 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException accountLocked(String userId) {
        return new UnauthorizedException("잠금된 계정입니다. 사용자 ID: " + userId);
    }

    /**
     * 파일 접근 권한 없음 예외 생성 정적 메서드
     *
     * @param fileId 파일 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException fileAccess(Long fileId) {
        return new UnauthorizedException("파일에 접근할 권한이 없습니다. 파일 ID: " + fileId);
    }

    /**
     * 신고 취소 권한 없음 예외 생성 정적 메서드
     *
     * @param reportId 신고 ID
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException reportCancel(Long reportId) {
        return new UnauthorizedException("신고를 취소할 권한이 없습니다. 신고 ID: " + reportId);
    }

    /**
     * 자신의 컨텐츠 신고 시도 예외 생성 정적 메서드
     *
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException cannotReportOwnContent() {
        return new UnauthorizedException("자신의 게시글이나 댓글은 신고할 수 없습니다.");
    }

    /**
     * 세션 만료 예외 생성 정적 메서드
     *
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException sessionExpired() {
        return new UnauthorizedException("세션이 만료되었습니다. 다시 로그인해주세요.");
    }

    /**
     * 토큰 무효 예외 생성 정적 메서드
     *
     * @return UnauthorizedException 인스턴스
     */
    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException("유효하지 않은 토큰입니다.");
    }
}