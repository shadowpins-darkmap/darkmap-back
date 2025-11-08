package com.sp.auth.controller;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.service.AuthService;
import com.sp.config.EnvironmentConfig;
import com.sp.config.EnvironmentResolver;
import com.sp.exception.WithdrawnMemberException;
import com.sp.member.service.MemberService;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.token.service.KakaoTokenService;
import com.sp.token.service.RefreshTokenService;
import com.sp.token.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Tag(
        name = "Authentication",
        description = """
        ## 인증 관리 API
     
        ### 인증 방식
        - **Access Token**: 쿠키로 자동 관리 (30분 유효)
        - **Refresh Token**: 쿠키로 자동 관리 (7일 유효)
        - 프론트엔드에서 토큰을 직접 다룰 필요 없음
        
        ### 지원 기능
        - 카카오/구글 소셜 로그인
        - 자동 토큰 갱신
        - 로그아웃
        - 회원 탈퇴
        
        ## 🔍 Swagger UI 테스트 방법
        
        Swagger UI에서는 쿠키를 직접 테스트할 수 없습니다.
       
        ### 방법 1: 브라우저에서 로그인 후 테스트
        1. 브라우저 새 탭에서 `/api/v1/auth/login/kakao` 접근
        2. 로그인 완료 (쿠키 자동 설정됨)
        3. Swagger UI로 돌아와서 API 테스트
        4. 쿠키가 자동으로 전송되어 인증됨
       
        ### 방법 2: Bearer Token 직접 입력
        1. 개발자 도구 → Application → Cookies
        2. `access_token` 값 복사
        3. Swagger "Authorize 🔓" 버튼 클릭
        4. 복사한 토큰 입력 (Bearer 접두사 제외)
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

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String ACCESS_COOKIE = "access_token";
    private static final String OAUTH_STATE_COOKIE = "oauth_state";
    private static final String OAUTH_REDIRECT_COOKIE = "oauth_redirect";

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
            5. 최종적으로 프론트엔드로 리다이렉트 (토큰 포함)
```
            https://yourfrontend.com/social-redirect-kakao?success=true&token={ACCESS_TOKEN}
```
            """
    )
    @GetMapping("/login/kakao")
    public void redirectToKakao(
            @RequestParam(value = "redirectUri", required = false) String redirectOverride,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        EnvironmentConfig envConfig = environmentResolver.resolve(request, redirectOverride);
        String state = UUID.randomUUID().toString();

        persistEphemeralCookie(response, envConfig, OAUTH_STATE_COOKIE, state, Duration.ofMinutes(10));
        persistEphemeralCookie(response, envConfig, OAUTH_REDIRECT_COOKIE, envConfig.getFrontendUrl(), Duration.ofMinutes(10));

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
                    description = "탈퇴한 회원 - 재가입 불가",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                    "error": "WITHDRAWN_MEMBER"
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

        String redirectPreference = getCookieValue(request, OAUTH_REDIRECT_COOKIE).orElse(null);
        EnvironmentConfig envConfig = environmentResolver.resolve(request, redirectPreference);

        if (!validateState(request, state)) {
            redirectWithError(response, envConfig, "INVALID_STATE");
            return;
        }

        try {
            AuthResponse authResponse = authService.loginWithKakao(code);
            setTokensAndRedirect(authResponse, response, envConfig);
        } catch (WithdrawnMemberException e) {
            // 탈퇴 회원
            redirectWithError(response, envConfig, "WITHDRAWN_MEMBER");
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
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        return processWithdrawal(id, httpRequest, response, AuthType.KAKAO);
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
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        return processWithdrawal(id, httpRequest, response, AuthType.GOOGLE);
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
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        return processWithdrawal(id, httpRequest, response, null);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "access_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private ResponseEntity<?> processWithdrawal(
            Long memberId,
            HttpServletRequest request,
            HttpServletResponse response,
            AuthType requiredType) {

        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            EnvironmentConfig envConfig = environmentResolver.resolve(request);
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

            clearTokenCookies(response, envConfig);

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
            HttpServletRequest request,
            HttpServletResponse response) {
        if (id == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }
        try {
            EnvironmentConfig envConfig = environmentResolver.resolve(request);
            // JWT 토큰 블랙리스트 처리
            String token = getTokenFromRequest(request);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            refreshTokenService.deleteByMemberId(id);
            clearTokenCookies(response, envConfig);

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
            description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. " +
                    "Refresh Token은 쿠키 또는 request body로 전송할 수 있습니다."
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
            description = "Refresh Token (선택사항 - 쿠키로도 전송 가능)",
            required = false,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.sp.auth.model.vo.RefreshRequest.class),
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
    public ResponseEntity<?> refreshToken(
            @RequestBody(required = false) com.sp.auth.model.vo.RefreshRequest refreshRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = null;

        // 1. body에서 먼저 확인 (프론트엔드가 localStorage에서 보낸 경우)
        if (refreshRequest != null && refreshRequest.getRefreshToken() != null) {
            refreshToken = refreshRequest.getRefreshToken();
            log.info("📱 Refresh token from request body");
        }

        // 2. 없으면 쿠키에서 확인 (기존 방식 호환)
        if (refreshToken == null) {
            refreshToken = getRefreshTokenFromCookie(request);
            log.info("🍪 Refresh token from cookie");
        }

        if (refreshToken == null) {
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
            EnvironmentConfig envConfig = environmentResolver.resolve(request);
            addTokenCookie(response, ACCESS_COOKIE, newAccessToken,
                    Duration.ofMillis(jwtTokenProvider.getExpirationTime()), envConfig);

            log.info("✅ 토큰 갱신 완료 - 사용자 ID: {}", memberId);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "expiresIn", jwtTokenProvider.getExpirationTime()
            ));

        } catch (Exception e) {
            log.error("❌ 토큰 갱신 실패", e);
            return ResponseEntity.status(401).body(Map.of("error", "토큰 갱신 실패"));
        }
    }

    private void setTokensAndRedirect(AuthResponse authResponse, HttpServletResponse response, EnvironmentConfig envConfig) throws IOException {
        log.info("Environment detected - Frontend: {}, Cookie Domain: {}, Is Local: {}",
                envConfig.getFrontendUrl(), envConfig.getCookieDomain(), envConfig.isLocal());

        addTokenCookie(response, REFRESH_COOKIE, authResponse.getRefreshToken(), Duration.ofDays(7), envConfig);
        addTokenCookie(response, ACCESS_COOKIE, authResponse.getJwtToken(),
                Duration.ofMillis(jwtTokenProvider.getExpirationTime()), envConfig);
        clearEphemeralCookies(response, envConfig);

        String redirectUrl = envConfig.getFrontendUrl() + "/social-redirect-kakao?success=true";
        log.info("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void clearTokenCookies(HttpServletResponse response, EnvironmentConfig envConfig) {
        clearCookie(response, REFRESH_COOKIE, envConfig);
        clearCookie(response, ACCESS_COOKIE, envConfig);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .filter(StringUtils::hasText);
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, Duration maxAge, EnvironmentConfig envConfig) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        addCookie(response, name, value, maxAge, true, envConfig);
    }

    private void persistEphemeralCookie(HttpServletResponse response, EnvironmentConfig envConfig, String name, String value, Duration maxAge) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        addCookie(response, name, value, maxAge, true, envConfig);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge, boolean httpOnly, EnvironmentConfig envConfig) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(httpOnly)
                .secure(!envConfig.isLocal())
                .maxAge(maxAge);

        if (!envConfig.isLocal() && StringUtils.hasText(envConfig.getCookieDomain())) {
            builder.domain(envConfig.getCookieDomain());
            builder.sameSite("None");
        } else {
            builder.sameSite("Lax");
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearCookie(HttpServletResponse response, String name, EnvironmentConfig envConfig) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(!envConfig.isLocal());

        if (!envConfig.isLocal() && StringUtils.hasText(envConfig.getCookieDomain())) {
            builder.domain(envConfig.getCookieDomain());
            builder.sameSite("None");
        } else {
            builder.sameSite("Lax");
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearEphemeralCookies(HttpServletResponse response, EnvironmentConfig envConfig) {
        clearCookie(response, OAUTH_STATE_COOKIE, envConfig);
        clearCookie(response, OAUTH_REDIRECT_COOKIE, envConfig);
    }

    private boolean validateState(HttpServletRequest request, String incomingState) {
        if (!StringUtils.hasText(incomingState)) {
            return false;
        }
        return getCookieValue(request, OAUTH_STATE_COOKIE)
                .map(stored -> stored.equals(incomingState))
                .orElse(false);
    }

    private void redirectWithError(HttpServletResponse response, EnvironmentConfig envConfig, String errorCode) throws IOException {
        String redirectUrl = envConfig.getFrontendUrl() + "/social-redirect-kakao?success=false&error=" + errorCode;
        log.warn("OAuth redirect with error {} -> {}", errorCode, redirectUrl);
        clearEphemeralCookies(response, envConfig);
        response.sendRedirect(redirectUrl);
    }
}
