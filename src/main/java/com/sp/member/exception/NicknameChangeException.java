package com.sp.member.exception;

public class NicknameChangeException extends RuntimeException {
    private final String errorCode;

    public NicknameChangeException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // 정적 팩토리 메서드들
    public static NicknameChangeException reachedMaxCount() {
        return new NicknameChangeException(
                "닉네임 변경 횟수를 모두 사용했습니다. (최대 3회)",
                "NICKNAME_MAX_COUNT_REACHED"
        );
    }

    public static NicknameChangeException tooSoon(java.time.LocalDateTime nextAvailableAt) {
        return new NicknameChangeException(
                String.format("닉네임은 30일마다 변경 가능합니다. 다음 변경 가능일: %s",
                        nextAvailableAt.toString()),
                "NICKNAME_CHANGE_TOO_SOON"
        );
    }
}