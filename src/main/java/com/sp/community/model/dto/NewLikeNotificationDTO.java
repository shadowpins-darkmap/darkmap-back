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
 * 새 좋아요 알림 DTO
 */
@Schema(description = "새 좋아요 알림 정보")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewLikeNotificationDTO {

    @Schema(description = "좋아요 ID", example = "789")
    private Long likeId;

    @Schema(description = "좋아요를 누른 사용자 ID", example = "user789")
    private Long likerUserId;

    @Schema(description = "좋아요를 누른 사용자 닉네임", example = "좋아요러789")
    private String likerNickname;

    @Schema(description = "게시글 ID", example = "1")
    private Long boardId;

    @Schema(description = "게시글 제목", example = "내가 작성한 게시글")
    private String boardTitle;

    @Schema(description = "게시글 내용 미리보기", example = "게시글 내용입니다...")
    private String boardContentPreview;

    @Schema(description = "좋아요 생성 시간", example = "2024-01-15 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}