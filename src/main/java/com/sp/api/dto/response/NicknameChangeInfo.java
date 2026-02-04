package com.sp.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "닉네임 변경 정보")
public class NicknameChangeInfo {

    @Schema(description = "변경 가능 여부", example = "true")
    private boolean canChange;

    @Schema(description = "현재 변경 횟수", example = "1")
    private int changeCount;

    @Schema(description = "최대 변경 횟수", example = "3")
    private int maxChangeCount;

    @Schema(description = "마지막 변경 일시", example = "2024-01-15T10:30:00")
    private LocalDateTime lastChangeAt;

    @Schema(description = "다음 변경 가능 일시", example = "2024-02-14T10:30:00")
    private LocalDateTime nextAvailableAt;
}