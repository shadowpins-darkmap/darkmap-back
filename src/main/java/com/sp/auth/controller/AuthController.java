package com.sp.auth.controller;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.WithdrawRequest;
import com.sp.auth.service.AuthService;
import com.sp.config.EnvironmentConfig;
import com.sp.exception.WithdrawnMemberException;
import com.sp.member.service.MemberService;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.token.service.KakaoTokenService;
import com.sp.token.service.RefreshTokenService;
import com.sp.token.service.TokenBlacklistService;
import com.sp.util.EnvironmentUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Tag(name = "Authentication", description = "인증 관리 API - 카카오/구글 소셜 로그인, 로그아웃, 토큰 갱신, 회원 탈퇴 기능을 제공합니다.")
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

    /**
     * 카카오 로그인 - 카카오 인증 페이지로 리다이렉트
     */
    @Operation(
            summary = "카카오 로그인 시작",
            description = "카카오 OAuth 인증 페이지로 리다이렉트합니다. 브라우저에서 직접 접근해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "카카오 인증 페이지로 리다이렉트"
            )
    })
    @GetMapping("/login/kakao")
    public void redirectToKakao(HttpServletResponse response) throws IOException {
        String redirectUrl = authService.getKakaoAuthorizeUrl();
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
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        try {
            AuthResponse authResponse = authService.loginWithKakao(code);
            setTokensAndRedirect(authResponse, response, request);
        } catch (WithdrawnMemberException e) {
            // 탈퇴 회원
            EnvironmentConfig envConfig = EnvironmentUtil.determineEnvironment(request);
            String redirectUrl = envConfig.getFrontendUrl() +
                    "/social-redirect-kakao?success=false&error=WITHDRAWN_MEMBER";
            log.warn("🚫 탈퇴 회원 로그인 차단 - 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
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
    @DeleteMapping("/withdraw/kakao")
    public ResponseEntity<?> withdrawKakao(
            @Parameter(hidden = true) @AuthenticationPrincipal Long id,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        try {
            Member member = memberService.findById(id);
            if (member == null || member.getIsDeleted()) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            if (member.getType() != AuthType.KAKAO) {
                return ResponseEntity.status(400).body(Map.of("error", "카카오 사용자가 아닙니다."));
            }

            // 카카오 연동 해제 (저장된 토큰 자동 사용)
            authService.disconnectKakao(id);

            // JWT 토큰 블랙리스트 처리
            String token = getTokenFromRequest(httpRequest);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            // 회원 탈퇴 및 토큰 삭제
            memberService.withdraw(id);
            refreshTokenService.deleteByMemberId(id);
            kakaoTokenService.deleteByMemberId(id);

            clearTokenCookies(response, httpRequest);

            log.info("✅ 카카오 탈퇴 완료 - 사용자 ID: {}", id);
            return ResponseEntity.ok().body(Map.of("message", "카카오 탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            log.error("❌ 카카오 탈퇴 처리 실패 - 사용자 ID: {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "탈퇴 처리 중 오류가 발생했습니다."));
        }
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
    @DeleteMapping("/withdraw/google")
    public ResponseEntity<?> withdrawGoogle(
            @Parameter(hidden = true) @AuthenticationPrincipal Long id,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        try {
            Member member = memberService.findById(id);
            if (member == null || member.getIsDeleted()) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            if (member.getType() != AuthType.GOOGLE) {
                return ResponseEntity.status(400).body(Map.of("error", "구글 사용자가 아닙니다."));
            }

            // 구글 연동 해제
            authService.disconnectGoogle(id);

            // JWT 토큰 블랙리스트 처리
            String token = getTokenFromRequest(httpRequest);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            // 회원 탈퇴 및 토큰 삭제
            memberService.withdraw(id);
            refreshTokenService.deleteByMemberId(id);

            clearTokenCookies(response, httpRequest);

            log.info("✅ 구글 탈퇴 완료 - 사용자 ID: {}", id);
            return ResponseEntity.ok().body(Map.of("message", "구글 탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            log.error("❌ 구글 탈퇴 처리 실패 - 사용자 ID: {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "탈퇴 처리 중 오류가 발생했습니다."));
        }
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
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long id,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // JWT 토큰 블랙리스트 처리
            String token = getTokenFromRequest(request);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            refreshTokenService.deleteByMemberId(id);
            clearTokenCookies(response, request);

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

    private void setTokensAndRedirect(AuthResponse authResponse, HttpServletResponse response, HttpServletRequest request) throws IOException {
        EnvironmentConfig envConfig = EnvironmentUtil.determineEnvironment(request);

        log.info("Environment detected - Frontend: {}, Cookie Domain: {}, Is Local: {}",
                envConfig.getFrontendUrl(), envConfig.getCookieDomain(), envConfig.isLocal());

        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken != null) {
            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(!envConfig.isLocal())
                    .path("/")
                    .maxAge(Duration.ofDays(7));

            if (!envConfig.isLocal()) {
                cookieBuilder.domain(envConfig.getCookieDomain());
                cookieBuilder.sameSite("None");
            } else {
                cookieBuilder.sameSite("Lax");
            }

            ResponseCookie refreshCookie = cookieBuilder.build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        String redirectUrl = envConfig.getFrontendUrl() + "/social-redirect-kakao?success=true&token=" + authResponse.getJwtToken();
        log.info("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void clearTokenCookies(HttpServletResponse response, HttpServletRequest request) {
        EnvironmentConfig envConfig = (request != null)
                ? EnvironmentUtil.determineEnvironment(request)
                : new EnvironmentConfig("https://kdark.weareshadowpins.co.kr", "api.kdark.weareshadowpins.com", false);

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(!envConfig.isLocal());

        if (!envConfig.isLocal()) {
            cookieBuilder.domain(envConfig.getCookieDomain());
            cookieBuilder.sameSite("None");
        } else {
            cookieBuilder.sameSite("Lax");
        }

        ResponseCookie clearRefreshCookie = cookieBuilder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}