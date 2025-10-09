package com.sp.common.search.controller;

import com.sp.common.search.model.dto.UnifiedSearchResultDTO;
import com.sp.common.search.service.UnifiedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final UnifiedSearchService unifiedSearchService;

    @Operation(
            summary = "통합 검색",
            description = "Article의 제목과 Board의 제목/내용을 한번에 검색 | 최신순 정렬 및 10개 단위 페이징"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어가 비어있거나 유효하지 않음",
                    content = @Content
            )
    })
    @GetMapping("/all")
    public ResponseEntity<Page<UnifiedSearchResultDTO>> search(
            @Parameter(
                    description = "검색 키워드 (Article 제목, Board 제목/내용에서 검색)",
                    required = true,
                    example = "강도"
            )
            @RequestParam("keyword") String keyword,

            @Parameter(
                    description = "페이지 번호 (0부터 시작, 기본값: 0)",
                    example = "0"
            )
            @RequestParam(value = "page", defaultValue = "0") int page) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(page, 10);

        Page<UnifiedSearchResultDTO> results = unifiedSearchService.unifiedSearch(keyword, pageable);
        return ResponseEntity.ok(results);
    }
}
