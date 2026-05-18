package com.sp.cyberflashing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Schema(description = "사이버 플래싱 사례 목록 조회 응답")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CyberFlashingCaseListVO {

    @Schema(description = "조회된 사례 목록")
    private List<CyberFlashingCaseVO> items;

    @Schema(description = "페이징 정보")
    private PageInfoVO pageInfo;

    @Schema(description = "적용된 필터 (정규화 후 값)")
    private FilterInfoVO filterInfo;

    @Schema(description = "페이징 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class PageInfoVO {

        @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
        private Integer currentPage;

        @Schema(description = "페이지 크기", example = "20")
        private Integer pageSize;

        @Schema(description = "필터 적용된 전체 건수", example = "717")
        private Long totalElements;

        @Schema(description = "전체 페이지 수", example = "36")
        private Integer totalPages;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private Boolean hasPrevious;

        @Schema(description = "첫 페이지 여부", example = "true")
        private Boolean isFirst;

        @Schema(description = "마지막 페이지 여부", example = "false")
        private Boolean isLast;
    }

    @Schema(description = "적용된 필터 (정규화 후 값, 미적용 시 null)")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class FilterInfoVO {

        @Schema(description = "국가 코드 (대문자 정규화 후)", example = "GB", nullable = true)
        private String countryCode;

        @Schema(description = "중복 여부", example = "", nullable = true)
        private String duplicateFlag;

        @Schema(description = "포함 여부", example = "포함", nullable = true)
        private String includeFlag;
    }
}
