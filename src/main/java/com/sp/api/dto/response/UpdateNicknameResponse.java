package com.sp.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "닉네임 변경 응답")
public class UpdateNicknameResponse {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "변경된 사용자 정보")
    private MemberData data;

    @Schema(description = "응답 메시지", example = "닉네임이 성공적으로 변경되었습니다.")
    private String message;

    @Getter
    @Builder
    @Schema(description = "변경된 사용자 정보")
    public static class MemberData {
        @Schema(description = "사용자 ID", example = "1")
        private Long id;

        @Schema(description = "변경된 닉네임", example = "새로운닉네임")
        private String nickname;

        @Schema(description = "닉네임 변경 횟수", example = "2")
        private int changeCount;

        @Schema(description = "마지막 변경 일시", example = "2024-01-15T10:30:00")
        private LocalDateTime lastChangeAt;
    }
}