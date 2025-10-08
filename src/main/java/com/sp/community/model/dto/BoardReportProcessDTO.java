package com.sp.community.model.dto;

import com.sp.community.persistent.entity.BoardReportEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 게시글 신고 처리 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoardReportProcessDTO {

    /**
     * 신고 ID
     */
    @NotNull(message = "신고 ID는 필수입니다.")
    private Long reportId;

    /**
     * 처리 액션 (승인/거부)
     */
    @NotNull(message = "처리 액션은 필수입니다.")
    private ProcessAction action;

    /**
     * 처리자 ID
     */
    @Size(max = 50, message = "처리자 ID는 50자 이하로 입력해주세요.")
    private Long processorId;

    /**
     * 처리 결과/사유
     */
    @NotBlank(message = "처리 결과는 필수입니다.")
    @Size(min = 10, max = 500, message = "처리 결과는 10자 이상 500자 이하로 입력해주세요.")
    private String result;

    /**
     * 게시글 조치 사항 (선택사항)
     */
    private BoardAction boardAction;

    /**
     * 처리 액션 Enum
     */
    public enum ProcessAction {
        APPROVE("승인"),
        REJECT("거부");

        private final String description;

        ProcessAction(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 게시글 조치 Enum
     */
    public enum BoardAction {
        NONE("조치 없음"),
        HIDE("게시글 숨김"),
        DELETE("게시글 삭제"),
        WARNING("작성자 경고");

        private final String description;

        BoardAction(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * ReportStatus로 변환
     */
    public BoardReportEntity.ReportStatus toReportStatus() {
        return switch (action) {
            case APPROVE -> BoardReportEntity.ReportStatus.APPROVED;
            case REJECT -> BoardReportEntity.ReportStatus.REJECTED;
        };
    }

    /**
     * 정리된 처리 결과 반환
     */
    public String getTrimmedResult() {
        return result != null ? result.trim() : "";
    }

    /**
     * DTO 검증
     */
    public void validate() {
        if (result != null) {
            String trimmedResult = result.trim();
            if (trimmedResult.length() < 10) {
                throw new IllegalArgumentException("처리 결과는 최소 10자 이상 입력해주세요.");
            }
        }
    }
}