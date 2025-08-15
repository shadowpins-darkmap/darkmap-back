package com.sp.community.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 활동 요약 DTO
 */
@Schema(description = "사용자 활동 요약 정보")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivitySummaryDTO {

    @Schema(description = "새 댓글 수", example = "5")
    private Long newCommentsCount;

    @Schema(description = "새 좋아요 수", example = "12")
    private Long newLikesCount;

    @Schema(description = "조회 시작 시간", example = "2024-01-13 15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime since;

    @Schema(description = "조회 종료 시간", example = "2024-01-15 15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime until;

    @Schema(description = "조회 기간 (시간)", example = "48")
    private Integer periodHours;

    @Schema(description = "총 활동 수", example = "17")
    public Long getTotalActivityCount() {
        return (newCommentsCount != null ? newCommentsCount : 0L) +
                (newLikesCount != null ? newLikesCount : 0L);
    }

    @Schema(description = "활동이 있는지 여부", example = "true")
    public Boolean hasActivity() {
        return getTotalActivityCount() > 0;
    }
}