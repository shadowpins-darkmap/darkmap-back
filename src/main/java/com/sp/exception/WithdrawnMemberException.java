package com.sp.exception;

public class WithdrawnMemberException extends RuntimeException {
    public WithdrawnMemberException(String message) {
        super(message);
    }
}