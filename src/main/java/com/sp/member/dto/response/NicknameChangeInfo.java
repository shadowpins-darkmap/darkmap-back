package com.sp.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "닉네임 변경 정보")
public class NicknameChangeInfo {

    @Schema(description = "변경 가능 여부", example = "true")
    private boolean canChange;

    @Schema(description = "현재 변경 횟수", example = "1")
    private Integer changeCount;

    @Schema(description = "최대 변경 횟수", example = "3")
    private Integer maxChangeCount;

    @Schema(description = "마지막 변경 일시 (UTC)", example = "2024-01-15T10:30:00Z")
    private Instant lastChangeAt;

    @Schema(description = "다음 변경 가능 일시 (UTC)", example = "2024-02-14T10:30:00Z")
    private Instant nextAvailableAt;
}