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
 * 새 댓글 알림 DTO
 */
@Schema(description = "새 댓글 알림 정보")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCommentNotificationDTO {

    @Schema(description = "댓글 ID", example = "123")
    private Long commentId;

    @Schema(description = "댓글 내용", example = "좋은 게시글이네요!")
    private String content;

    @Schema(description = "댓글 작성자 ID", example = "user456")
    private Long commenterUserId;

    @Schema(description = "댓글 작성자 닉네임", example = "댓글러456")
    private String commenterNickname;

    @Schema(description = "게시글 ID", example = "1")
    private Long boardId;

    @Schema(description = "게시글 제목", example = "내가 작성한 게시글")
    private String boardTitle;

    @Schema(description = "댓글 작성 시간", example = "2024-01-15 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "댓글 내용 미리보기 (50자)", example = "좋은 게시글이네요! 많은 도움이 되었습니다...")
    public String getContentPreview() {
        if (content == null) return "";
        return content.length() <= 50 ? content : content.substring(0, 50) + "...";
    }
}