package com.sp.api.exception;

import java.time.format.DateTimeFormatter;

public class NicknameChangeException extends RuntimeException {

    private final String errorCode;

    private NicknameChangeException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static NicknameChangeException reachedMaxCount() {
        return new NicknameChangeException(
                "닉네임 변경 횟수를 모두 사용했습니다. (최대 3회)",
                "NICKNAME_MAX_COUNT_REACHED"
        );
    }

    public static NicknameChangeException tooSoon(java.time.LocalDateTime nextAvailableAt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        String formattedDate = nextAvailableAt.format(formatter);

        return new NicknameChangeException(
                String.format("닉네임은 30일마다 변경 가능합니다. \n다음 변경 가능일: %s", formattedDate),
                "NICKNAME_CHANGE_TOO_SOON"
        );
    }
}