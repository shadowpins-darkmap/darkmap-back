package com.sp.common.search.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 검색 응답 DTO")
public class UnifiedSearchResponseDTO {

    @Schema(description = "검색 결과 목록")
    private List<UnifiedSearchResultDTO> results;

    @Schema(description = "전체 검색 결과 개수", example = "25")
    private Long totalElements;

    @Schema(description = "뉴스(Article) 결과 개수", example = "10")
    private Long newsTotalElements;

    @Schema(description = "커뮤니티(Board) 결과 개수", example = "15")
    private Long communityTotalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private Integer totalPages;

    @Schema(description = "현재 페이지 (1부터 시작)", example = "1")
    private Integer page;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;
}
