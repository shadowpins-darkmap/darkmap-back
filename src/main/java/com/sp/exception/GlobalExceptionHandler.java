package com.sp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WithdrawnMemberException.class)
    public ResponseEntity<?> handleWithdrawnMemberException(WithdrawnMemberException e) {
        log.warn("🚫 탈퇴 회원 로그인 시도: {}", e.getMessage());
        return ResponseEntity.status(403).body(Map.of(
                "error", "WITHDRAWN_MEMBER",
                "message", e.getMessage()
        ));
    }
}