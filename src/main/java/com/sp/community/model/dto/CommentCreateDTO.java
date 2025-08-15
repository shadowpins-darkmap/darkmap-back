package com.sp.community.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 댓글 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CommentCreateDTO {

    /**
     * 게시글 ID
     */
    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long boardId;

    /**
     * 댓글 내용
     */
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(min = 1, max = 490, message = "댓글은 1자 이상 490자 이하로 입력해주세요.")
    private String content;

    /**
     * 작성자 ID
     */
    @Size(max = 50, message = "")
    private String authorId;

    /**
     * 댓글 내용 정리 (앞뒤 공백 제거)
     */
    public String getTrimmedContent() {
        return content != null ? content.trim() : "";
    }

    /**
     * DTO 검증
     */
    public void validate() {
        if (boardId == null || boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글 ID입니다.");
        }

        if (getTrimmedContent().length() > 490) {
            throw new IllegalArgumentException("댓글은 490자 이하로 입력해주세요.");
        }
    }

    /**
     * 댓글 내용 미리보기 (100자)
     */
    public String getContentPreview() {
        String trimmed = getTrimmedContent();
        if (trimmed.length() <= 100) {
            return trimmed;
        }
        return trimmed.substring(0, 100) + "...";
    }
}
