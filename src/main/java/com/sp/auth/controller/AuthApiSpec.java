package com.sp.auth.controller;

import com.sp.auth.model.vo.KakaoLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth API", description = "인증/로그인 관련 API")
@RequestMapping("/api/v1/auth")
public interface AuthApiSpec {

    @Operation(
            summary = "카카오 소셜 로그인",
            description = "카카오 소셜 로그인, 성공 시 JWT 토큰을 반환",
            requestBody = @RequestBody(
                    description = "소셜 로그인 요청 본문",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = KakaoLoginRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공 (JWT 토큰 반환)"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    @PostMapping("/kakao")
    ResponseEntity<?> kakaoLogin(@org.springframework.web.bind.annotation.RequestBody KakaoLoginRequest request);


}
