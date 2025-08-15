package com.sp.exception;

/**
 * 댓글을 찾을 수 없을 때 발생하는 예외
 */
public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(String message) {
        super(message);
    }

    public CommentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}