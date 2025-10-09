package com.sp.common.search.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 검색 결과 DTO")
public class UnifiedSearchResultDTO {

    @Schema(description = "결과 타입 (ARTICLE 또는 BOARD)", example = "BOARD")
    private String resultType;

    @Schema(description = "ID (Article ID 또는 Board ID)", example = "1")
    private Long id;

    @Schema(description = "제목", example = "강도 사건 발생")
    private String title;

    @Schema(description = "내용 (Board인 경우에만)", example = "어제 저녁 강도 사건이 발생했습니다.")
    private String content;

    private String category; // Board
    private String press; // Article
    private String reporter; // Article
    private String crimeType; // Article
    private String url;
    private LocalDateTime createdAt; // Board
    private String contributionDate; // Article
    private Integer viewCount; // Board
    private Integer likeCount; // Board
}