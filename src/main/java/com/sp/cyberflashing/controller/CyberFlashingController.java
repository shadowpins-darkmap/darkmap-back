package com.sp.cyberflashing.controller;

import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.cyberflashing.model.dto.CyberFlashingSearchDTO;
import com.sp.cyberflashing.model.vo.CyberFlashingCaseListVO;
import com.sp.cyberflashing.service.CyberFlashingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cyber Flashing", description = "사이버 플래싱 사례 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/cyber-flashing/cases")
@RequiredArgsConstructor
public class CyberFlashingController {

    private final CyberFlashingService cyberFlashingService;

    @Operation(
            summary = "사이버 플래싱 사례 목록 조회",
            description = "국가코드, 중복 여부, 포함 여부 조건으로 사이버 플래싱 사례를 페이징 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<CommonApiResponse<CyberFlashingCaseListVO>> getCases(
            @Parameter(description = "조회 필터") @ModelAttribute CyberFlashingSearchDTO searchDTO,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO) {
        log.info("사이버 플래싱 사례 목록 조회 요청: search={}, page={}", searchDTO, pageRequestDTO);

        CyberFlashingCaseListVO data = cyberFlashingService.getCases(searchDTO, pageRequestDTO);

        return ResponseEntity.ok(CommonApiResponse.success("사이버 플래싱 사례 목록 조회 성공", data));
    }

    @Operation(
            summary = "사이버 플래싱 사례 총 개수 조회",
            description = "국가코드, 중복 여부, 포함 여부 조건으로 사이버 플래싱 사례 총 개수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "개수 조회 성공")
    })
    @GetMapping("/count")
    public ResponseEntity<CommonApiResponse<Long>> getCaseCount(
            @Parameter(description = "조회 필터") @ModelAttribute CyberFlashingSearchDTO searchDTO) {
        log.info("사이버 플래싱 사례 개수 조회 요청: search={}", searchDTO);

        long totalCount = cyberFlashingService.getCaseCount(searchDTO);

        return ResponseEntity.ok(CommonApiResponse.success("사이버 플래싱 사례 총 개수 조회 성공", totalCount));
    }
}
