package com.sp.auth.controller;

import com.sp.auth.dto.response.AuthResponse;
import com.sp.auth.dto.request.RefreshRequest;
import com.sp.auth.security.jwt.JwtTokenProvider;
import com.sp.auth.service.AuthService;
import com.sp.config.EnvironmentConfig;
import com.sp.config.EnvironmentResolver;
import com.sp.exception.WithdrawnMemberException;
import com.sp.member.service.MemberService;
import com.sp.member.entity.Member;
import com.sp.auth.enums.AuthType;
import com.sp.auth.service.KakaoTokenService;
import com.sp.auth.service.RefreshTokenService;
import com.sp.auth.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Tag(
        name = "Authentication",
        description = """
        ## 인증 관리 API
     
        ### 인증 방식
        - **Access Token**: Authorization 헤더를 통해 Bearer 토큰으로 전달 (30분 유효)
        - **Refresh Token**: 클라이언트 저장소에서 관리하며 `/refresh` API를 통해 재발급 (7일 유효)
        - OAuth 콜백은 postMessage 기반 브리지 페이지로 토큰 payload를 프론트엔드에 전달합니다.
        
        ### 지원 기능
        - 카카오/구글 소셜 로그인
        - 토큰 재발급, 로그아웃, 회원 탈퇴
        - OAuth state 검증 및 에러 전달
        
        ## 🔍 Swagger UI 테스트 방법
        1. 브라우저 새 탭 또는 팝업에서 `/api/v1/auth/login/kakao`와 같은 로그인 엔드포인트 접근
        2. 로그인 완료 후 프론트엔드가 수신한 Access Token을 복사
        3. Swagger UI "Authorize 🔓" 버튼 클릭
        4. `Bearer <token>` 형태로 입력하여 인증 후 API 호출
        """
)
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final KakaoTokenService kakaoTokenService;
    private final EnvironmentResolver environmentResolver;
    private final AuthBridgeResponder authBridgeResponder;

    private static final String OAUTH_STATE_SESSION = "oauth_state";
    private static final String OAUTH_REDIRECT_SESSION = "oauth_redirect";

    /**
     * 카카오 로그인 - 카카오 인증 페이지로 리다이렉트
     */
    @Operation(
            summary = "카카오 로그인 시작",
            description = """
            ## 브라우저에서 직접 접근하세요
```
            https://api.kdark.weareshadowpins.com/api/v1/auth/login/kakao
```
            ### 로그인 플로우
            1. 이 URL로 리다이렉트
            2. 카카오 로그인 페이지 표시
            3. 사용자 인증 완료
            4. 콜백으로 자동 리다이렉트
            5. 콜백 페이지가 postMessage로 토큰 payload를 프론트엔드에 전달
            """
    )
    @GetMapping("/login/kakao")
    public void redirectToKakao(
            @RequestParam(value = "redirectUri", required = false) String redirectOverride,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        EnvironmentConfig envConfig = environmentResolver.resolve(request, redirectOverride);
        String state = UUID.randomUUID().toString();

        persistEphemeralState(request, state, envConfig.getFrontendUrl());

        String redirectUrl = authService.getKakaoAuthorizeUrl(state);
        response.sendRedirect(redirectUrl);
    }

    /**
     * 카카오 로그인 콜백
     */
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "로그인 성공 - 프론트엔드로 리다이렉트 (JWT 토큰 포함)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 인증 코드"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "탈퇴한 회원 - 재가입/재로그인 유보기간(기본 7일) 미경과",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                    "error": "WITHDRAWN_MEMBER",
                                    "message": "탈퇴한 회원은 2026-02-18T12:34:00Z 까지 재로그인이 불가능합니다."
                                }
                                """
                            )
                    )
            )
    })
    @GetMapping("/login/kakao/callback")
    public void kakaoCallback(
            @Parameter(description = "카카오 인증 코드", required = true) @RequestParam String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String redirectPreference = getSessionValue(request, OAUTH_REDIRECT_SESSION).orElse(null);
        EnvironmentConfig envConfig = environmentResolver.resolve(request, redirectPreference);

        if (!validateState(request, state)) {
            redirectWithError(request, response, envConfig, "INVALID_STATE");
            return;
        }

        try {
            AuthResponse authResponse = authService.loginWithKakao(code);
            clearEphemeralState(request);
            authBridgeResponder.writeSuccess(response, envConfig, authResponse);
            return;
        } catch (WithdrawnMemberException e) {
            redirectWithError(request, response, envConfig, "WITHDRAWN_MEMBER");
        } catch (IllegalStateException e) {
            log.error("카카오 사용자 정보가 누락되었습니다: {}", e.getMessage());
            redirectWithError(request, response, envConfig, "KAKAO_ACCOUNT_INCOMPLETE");
        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 알 수 없는 오류", e);
            redirectWithError(request, response, envConfig, "SERVER_ERROR");
        }
    }

    /**
     * 카카오 회원 탈퇴
     */
    @Operation(
            summary = "카카오 회원 탈퇴",
            description = "카카오 계정 연동을 해제하고 회원 탈퇴를 진행합니다. 저장된 카카오 토큰으로 자동 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "message": "카카오 탈퇴가 완료되었습니다."
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "카카오 사용자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "error": "카카오 사용자가 아닙니다."
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - JWT 토큰 없음 또는 만료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "error": "인증이 필요합니다."
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "error": "사용자를 찾을 수 없습니다."
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "error": "탈퇴 처리 중 오류가 발생했습니다."
                                    }
                                    """
                            )
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw/kakao")
    public ResponseEntity<?> withdrawKakao(
            @Parameter(hidden = true) @AuthenticationPrincipal Long id,
            HttpServletRequest httpRequest) {
        return processWithdrawal(id, httpRequest, AuthType.KAKAO);
    }

    /**
     * 구글 회원 탈퇴
     */
    @Operation(
            summary = "구글 회원 탈퇴",
            description = "구글 계정 연동을 해제하고 회원 탈퇴를 진행합니다. 저장된 구글 토큰으로 자동 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "message": "구글 탈퇴가 완료되었습니다."
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "구글 사용자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "error": "구글 사용자가 아닙니다."
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - JWT 토큰 없음 또는 만료"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw/google")
    public ResponseEntity<?> withdrawGoogle(
            @Parameter(hidden = true) @AuthenticationPrincipal Long id,
            HttpServletRequest httpRequest) {
        return processWithdrawal(id, httpRequest, AuthType.GOOGLE);
    }

    /**
     * 소셜 유형 없이 회원 탈퇴
     */
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 연동을 해제하고 탈퇴"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "message": "카카오 탈퇴가 완료되었습니다.",
                                        "provider": "KAKAO"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdrawAuto(
            @Parameter(hidden = true) @AuthenticationPrincipal Long id,
            HttpServletRequest httpRequest) {
        return processWithdrawal(id, httpRequest, null);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private ResponseEntity<?> processWithdrawal(
            Long memberId,
            HttpServletRequest request,
            AuthType requiredType) {

        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            Member member = memberService.findById(memberId);

            if (member == null || member.getIsDeleted()) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            AuthType memberType = member.getType();

            if (memberType == null) {
                return ResponseEntity.status(400).body(Map.of("error", "회원 유형이 설정되어 있지 않습니다."));
            }

            if (requiredType != null && memberType != requiredType) {
                String typeError = requiredType == AuthType.KAKAO
                        ? "카카오 사용자가 아닙니다."
                        : "구글 사용자가 아닙니다.";
                return ResponseEntity.status(400).body(Map.of("error", typeError));
            }

            switch (memberType) {
                case KAKAO -> {
                    authService.disconnectKakao(memberId);
                    // 카카오 토큰 수동 삭제 (연동 해제 실패 대비)
                    kakaoTokenService.deleteByMemberId(memberId);
                }
                case GOOGLE -> authService.disconnectGoogle(memberId);
                default -> {
                    return ResponseEntity.status(400).body(Map.of("error", "지원하지 않는 회원 유형입니다."));
                }
            }

            String token = getTokenFromRequest(request);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            memberService.withdraw(memberId);
            refreshTokenService.deleteByMemberId(memberId);

            String successMessage = memberType == AuthType.KAKAO
                    ? "카카오 탈퇴가 완료되었습니다."
                    : "구글 탈퇴가 완료되었습니다.";

            log.info("✅ {} 탈퇴 완료 - 사용자 ID: {}", memberType, memberId);
            return ResponseEntity.ok().body(Map.of(
                    "message", successMessage,
                    "provider", memberType.name()
            ));

        } catch (Exception e) {
            log.error("❌ 회원 탈퇴 처리 실패 - 사용자 ID: {}", memberId, e);
            return ResponseEntity.status(500).body(Map.of("error", "탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그아웃
     */
    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃합니다. Refresh Token을 삭제하고 JWT를 블랙리스트에 추가합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "message": "로그아웃이 완료되었습니다."
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "error": "로그아웃 처리 중 오류가 발생했습니다."
                                    }
                                    """
                            )
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long id,
            HttpServletRequest request) {
        if (id == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }
        try {
            // JWT 토큰 블랙리스트 처리
            String token = getTokenFromRequest(request);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            refreshTokenService.deleteByMemberId(id);

            log.info("✅ 로그아웃 완료 - 사용자 ID: {}", id);
            return ResponseEntity.ok().body(Map.of("message", "로그아웃이 완료되었습니다."));

        } catch (Exception e) {
            log.error("❌ 로그아웃 처리 실패 - 사용자 ID: {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "로그아웃 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * Access Token 갱신
     */
    @Operation(
            summary = "Access Token 갱신",
            description = "Refresh Token을 request body로 전송하여 새로운 Access Token을 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5Iiwicm9sZSI6IkJBU0lDIiwidHlwZSI6ImFjY2VzcyIsImlhdCI6MTczMDUyOTYwMCwiZXhwIjoxNzMwNTMwNTAwfQ.abc123",
                                        "expiresIn": 900000
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - Refresh Token 없음, 만료, 또는 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "토큰 없음",
                                            value = """
                                            {
                                                "error": "Refresh token이 없습니다."
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "토큰 만료",
                                            value = """
                                            {
                                                "error": "만료된 refresh token입니다."
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "유효하지 않은 토큰",
                                            value = """
                                            {
                                                "error": "유효하지 않은 refresh token입니다."
                                            }
                                            """
                                    )
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh Token (JSON body)",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RefreshRequest.class),
                    examples = @ExampleObject(
                            value = """
                            {
                                "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5IiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE3MzA1Mjk2MDAsImV4cCI6MTczMTEzNDQwMH0.xyz789"
                            }
                            """
                    )
            )
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();

        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token이 없습니다."));
        }

        try {
            if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "유효하지 않은 refresh token입니다."));
            }

            var storedToken = refreshTokenService.findByToken(refreshToken);
            if (storedToken.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "만료된 refresh token입니다."));
            }

            Long memberId = Long.parseLong(jwtTokenProvider.getClaims(refreshToken).getSubject());
            var member = memberService.findById(memberId);

            if (member == null) {
                return ResponseEntity.status(401).body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            String newAccessToken = jwtTokenProvider.createAccessToken(memberId, member.getLevel());

            log.info("✅ 토큰 갱신 완료 - 사용자 ID: {}", memberId);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "tokenType", "Bearer",
                    "expiresIn", jwtTokenProvider.getExpirationTime()
            ));

        } catch (Exception e) {
            log.error("❌ 토큰 갱신 실패", e);
            return ResponseEntity.status(401).body(Map.of("error", "토큰 갱신 실패"));
        }
    }

    private boolean validateState(HttpServletRequest request, String incomingState) {
        if (!StringUtils.hasText(incomingState)) {
            return false;
        }
        return getSessionValue(request, OAUTH_STATE_SESSION)
                .map(stored -> stored.equals(incomingState))
                .orElse(false);
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, EnvironmentConfig envConfig, String errorCode) throws IOException {
        log.warn("OAuth redirect with error {}", errorCode);
        clearEphemeralState(request);
        authBridgeResponder.writeError(response, envConfig, errorCode);
    }

    private void persistEphemeralState(HttpServletRequest request, String state, String redirectUrl) {
        HttpSession session = request.getSession(true);
        session.setAttribute(OAUTH_STATE_SESSION, state);
        session.setAttribute(OAUTH_REDIRECT_SESSION, redirectUrl);
        session.setMaxInactiveInterval((int) Duration.ofMinutes(10).getSeconds());
    }

    private Optional<String> getSessionValue(HttpServletRequest request, String name) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(name);
        return value == null ? Optional.empty() : Optional.of(value.toString()).filter(StringUtils::hasText);
    }

    private void clearEphemeralState(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(OAUTH_STATE_SESSION);
        session.removeAttribute(OAUTH_REDIRECT_SESSION);
    }
}
