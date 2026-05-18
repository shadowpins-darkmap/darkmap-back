package com.sp.darkmap.controller;

import com.sp.community.model.response.CommonApiResponse;
import com.sp.darkmap.model.dto.CrimeCaseSaveRequest;
import com.sp.darkmap.model.vo.CrimeCaseVO;
import com.sp.darkmap.service.CrimeCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Crime Case", description = "범죄 사례(뉴스기사/회원 경험담) API")
@Slf4j
@RestController
@RequestMapping("/api/v1/crime-cases")
@RequiredArgsConstructor
public class CrimeCaseController {

    private final CrimeCaseService crimeCaseService;

    @Operation(
            summary = "범죄 사례 등록",
            description = "정보유형/범죄유형/시도/시군구/뉴스기사 URL/위경도로 범죄 사례를 등록합니다. 인증 필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(검증 실패/유효하지 않은 유형)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<CommonApiResponse<CrimeCaseVO>> create(
            @Valid @RequestBody CrimeCaseSaveRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CrimeCaseVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다.")
                            .build());
        }

        log.info("범죄 사례 등록 요청: memberId={}, request={}", memberId, request);
        CrimeCaseVO data = crimeCaseService.create(request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonApiResponse.success("범죄 사례 등록 성공", data));
    }
}
