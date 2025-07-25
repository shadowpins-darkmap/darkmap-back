package com.sp.community.model.response;

import lombok.*;

/**
 * API 응답 래퍼 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CommonApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private long timestamp;

    @Builder
    public CommonApiResponse(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 성공 응답 생성
     */
    public static <T> CommonApiResponse<T> success(T data) {
        return CommonApiResponse.<T>builder()
                .success(true)
                .message("성공")
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (메시지 포함)
     */
    public static <T> CommonApiResponse<T> success(String message, T data) {
        return CommonApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static <T> CommonApiResponse<T> error(String message) {
        return CommonApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * 실패 응답 생성 (에러 코드 포함)
     */
    public static <T> CommonApiResponse<T> error(String message, String errorCode) {
        return CommonApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}