package com.sp.exception;


/**
 * 게시글을 찾을 수 없을 때 발생하는 예외
 */
public class BoardNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 기본 생성자
     */
    public BoardNotFoundException() {
        super("게시글을 찾을 수 없습니다.");
    }

    /**
     * 메시지가 포함된 생성자
     *
     * @param message 예외 메시지
     */
    public BoardNotFoundException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인이 포함된 생성자
     *
     * @param message 예외 메시지
     * @param cause 원인
     */
    public BoardNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 원인이 포함된 생성자
     *
     * @param cause 원인
     */
    public BoardNotFoundException(Throwable cause) {
        super("게시글을 찾을 수 없습니다.", cause);
    }

    /**
     * 게시글 ID를 포함한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException withId(Long boardId) {
        return new BoardNotFoundException("게시글을 찾을 수 없습니다. ID: " + boardId);
    }

    /**
     * 게시글 ID와 원인을 포함한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @param cause 원인
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException withId(Long boardId, Throwable cause) {
        return new BoardNotFoundException("게시글을 찾을 수 없습니다. ID: " + boardId, cause);
    }

    /**
     * 삭제된 게시글에 대한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException deletedBoard(Long boardId) {
        return new BoardNotFoundException("삭제된 게시글입니다. ID: " + boardId);
    }

    /**
     * 비공개 게시글에 대한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException privateBoard(Long boardId) {
        return new BoardNotFoundException("비공개 게시글입니다. ID: " + boardId);
    }

    /**
     * 접근 권한이 없는 게시글에 대한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException accessDenied(Long boardId) {
        return new BoardNotFoundException("접근할 수 없는 게시글입니다. ID: " + boardId);
    }

    /**
     * 임시저장된 게시글에 대한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException draftBoard(Long boardId) {
        return new BoardNotFoundException("임시저장된 게시글입니다. ID: " + boardId);
    }

    /**
     * 승인 대기 중인 게시글에 대한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException pendingApproval(Long boardId) {
        return new BoardNotFoundException("승인 대기 중인 게시글입니다. ID: " + boardId);
    }

    /**
     * 블라인드 처리된 게시글에 대한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException blindedBoard(Long boardId) {
        return new BoardNotFoundException("블라인드 처리된 게시글입니다. ID: " + boardId);
    }

    /**
     * 만료된 게시글에 대한 예외 생성 정적 메서드
     *
     * @param boardId 게시글 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException expiredBoard(Long boardId) {
        return new BoardNotFoundException("만료된 게시글입니다. ID: " + boardId);
    }

    /**
     * 검색 조건에 맞는 게시글이 없을 때 예외 생성 정적 메서드
     *
     * @param searchCondition 검색 조건
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException noSearchResults(String searchCondition) {
        return new BoardNotFoundException("검색 조건에 맞는 게시글이 없습니다. 조건: " + searchCondition);
    }

    /**
     * 특정 카테고리에서 게시글을 찾을 수 없을 때 예외 생성 정적 메서드
     *
     * @param categoryId 카테고리 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException noBoardsInCategory(Long categoryId) {
        return new BoardNotFoundException("해당 카테고리에 게시글이 없습니다. 카테고리 ID: " + categoryId);
    }

    /**
     * 특정 사용자의 게시글을 찾을 수 없을 때 예외 생성 정적 메서드
     *
     * @param userId 사용자 ID
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException noBoardsByUser(String userId) {
        return new BoardNotFoundException("해당 사용자의 게시글이 없습니다. 사용자 ID: " + userId);
    }

    /**
     * 페이지 범위를 벗어났을 때 예외 생성 정적 메서드
     *
     * @param page 요청한 페이지
     * @param totalPages 전체 페이지 수
     * @return BoardNotFoundException 인스턴스
     */
    public static BoardNotFoundException pageOutOfRange(int page, int totalPages) {
        return new BoardNotFoundException(
                String.format("페이지 범위를 벗어났습니다. 요청 페이지: %d, 전체 페이지: %d", page, totalPages));
    }
}