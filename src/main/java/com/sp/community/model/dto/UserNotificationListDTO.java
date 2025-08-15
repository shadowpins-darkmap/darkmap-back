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
 * 사용자 알림 목록 DTO
 */
@Schema(description = "사용자 알림 목록")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationListDTO {

    @Schema(description = "새 댓글 알림 목록")
    private List<NewCommentNotificationDTO> newComments;

    @Schema(description = "새 좋아요 알림 목록")
    private List<NewLikeNotificationDTO> newLikes;

    @Schema(description = "활동 요약")
    private UserActivitySummaryDTO summary;

    @Schema(description = "페이징 정보 - 전체 요소 수", example = "25")
    private Long totalElements;

    @Schema(description = "페이징 정보 - 현재 페이지", example = "1")
    private Integer currentPage;

    @Schema(description = "페이징 정보 - 페이지 크기", example = "10")
    private Integer pageSize;

    @Schema(description = "페이징 정보 - 다음 페이지 존재 여부", example = "true")
    private Boolean hasNext;
}