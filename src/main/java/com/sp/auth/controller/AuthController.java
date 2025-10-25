package com.sp.auth.controller;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.WithdrawRequest;
import com.sp.auth.service.AuthService;
import com.sp.config.EnvironmentConfig;
import com.sp.member.service.MemberService;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.token.service.RefreshTokenService;
import com.sp.token.service.TokenBlacklistService;
import com.sp.util.EnvironmentUtil;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

    @GetMapping("/login/kakao")
    public void redirectToKakao(HttpServletResponse response) throws IOException {
        String redirectUrl = authService.getKakaoAuthorizeUrl();
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/login/kakao/callback")
    public void kakaoCallback(@RequestParam String code,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        AuthResponse authResponse = authService.loginWithKakao(code);
        setTokensAndRedirect(authResponse, response, request);
    }

    @DeleteMapping("/withdraw/kakao")
    public ResponseEntity<?> withdrawKakao(@AuthenticationPrincipal Long id,
                                           @RequestBody WithdrawRequest request,
                                           HttpServletResponse response) {
        try {
            Member member = memberService.findById(id);
            if (member == null) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            if (member.getType() != AuthType.KAKAO) {
                return ResponseEntity.status(400).body(Map.of("error", "카카오 사용자가 아닙니다."));
            }

            if (request.getKakaoAccessToken() != null && !request.getKakaoAccessToken().isEmpty()) {
                authService.disconnectKakao(request.getKakaoAccessToken());
            }

            memberService.withdraw(id);
            refreshTokenService.deleteByMemberId(id);
            clearTokenCookies(response, null);

            return ResponseEntity.ok().body(Map.of("message", "카카오 탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/withdraw/google")
    public ResponseEntity<?> withdrawGoogle(@AuthenticationPrincipal Long id,
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

            authService.disconnectGoogle(id);

            String token = getTokenFromRequest(httpRequest);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            memberService.withdraw(id);
            refreshTokenService.deleteByMemberId(id);
            clearTokenCookies(response, httpRequest);

            return ResponseEntity.ok().body(Map.of("message", "구글 탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            log.error("구글 탈퇴 처리 실패 - 사용자 ID: {}", id, e);
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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Long id,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        try {
            refreshTokenService.deleteByMemberId(id);
            clearTokenCookies(response, request);

            return ResponseEntity.ok().body(Map.of("message", "로그아웃이 완료되었습니다."));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "로그아웃 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);

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

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "expiresIn", jwtTokenProvider.getExpirationTime()
            ));

        } catch (Exception e) {
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