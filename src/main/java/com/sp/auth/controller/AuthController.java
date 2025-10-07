package com.sp.auth.controller;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.WithdrawRequest;
import com.sp.auth.service.AuthService;
import com.sp.member.service.MemberService;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.token.service.RefreshTokenService;
import com.sp.token.service.TokenBlacklistService;
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
import java.net.URI;
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
            // 사용자 정보 조회 및 검증
            Member member = memberService.findById(id);
            if (member == null) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            // 카카오 사용자인지 확인
            if (member.getType() != AuthType.KAKAO) {
                return ResponseEntity.status(400).body(Map.of("error", "카카오 사용자가 아닙니다."));
            }

            // 카카오 연동 해제 (액세스 토큰이 있는 경우)
            if (request.getKakaoAccessToken() != null && !request.getKakaoAccessToken().isEmpty()) {
                authService.disconnectKakao(request.getKakaoAccessToken());
            }

            // 회원 탈퇴 처리
            memberService.withdraw(id);

            // Refresh Token 삭제
            refreshTokenService.deleteByMemberId(id);

            // 쿠키 삭제
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
            // 사용자 정보 조회 및 검증
            Member member = memberService.findById(id);
            if (member == null || member.getIsDeleted()) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            // 구글 사용자인지 확인
            if (member.getType() != AuthType.GOOGLE) {
                return ResponseEntity.status(400).body(Map.of("error", "구글 사용자가 아닙니다."));
            }

            // 🔗 구글 연동 해제 (DB에서 토큰 자동 조회)
            authService.disconnectGoogle(id);

            // 현재 사용 중인 JWT 토큰을 블랙리스트에 추가
            String token = getTokenFromRequest(httpRequest);
            if (token != null) {
                tokenBlacklistService.blacklistToken(token);
            }

            // 회원 탈퇴 처리
            memberService.withdraw(id);

            // Refresh Token 삭제
            refreshTokenService.deleteByMemberId(id);

            // 쿠키 삭제
            clearTokenCookies(response, httpRequest);

            return ResponseEntity.ok().body(Map.of("message", "구글 탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            log.error("구글 탈퇴 처리 실패 - 사용자 ID: {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 확인
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. 쿠키에서 토큰 확인
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
            // Refresh Token 삭제
            refreshTokenService.deleteByMemberId(id);

            // 쿠키 삭제
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
            // Refresh Token 검증
            if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "유효하지 않은 refresh token입니다."));
            }

            // DB에서 Refresh Token 확인
            var storedToken = refreshTokenService.findByToken(refreshToken);
            if (storedToken.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "만료된 refresh token입니다."));
            }

            // 새로운 Access Token 발행
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
        // 요청 출처 기반으로 환경 판단
        EnvironmentConfig envConfig = determineEnvironment(request);

        log.info("Environment detected - Frontend: {}, Cookie Domain: {}, Is Local: {}",
                envConfig.frontendUrl, envConfig.cookieDomain, envConfig.isLocal);

        // Refresh Token을 환경에 맞는 쿠키에 저장
        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken != null) {
            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(!envConfig.isLocal)
                    .path("/")
                    .maxAge(Duration.ofDays(7));

            // 로컬 환경이 아닌 경우에만 domain과 sameSite 설정
            if (!envConfig.isLocal) {
                cookieBuilder.domain(envConfig.cookieDomain);
                cookieBuilder.sameSite("None");
            } else {
                cookieBuilder.sameSite("Lax");
            }

            ResponseCookie refreshCookie = cookieBuilder.build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        // 동적으로 프론트엔드 URL 결정하여 리다이렉트
        String redirectUrl = envConfig.frontendUrl + "/social-redirect-kakao?success=true&token=" + authResponse.getJwtToken();
        log.info("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void clearTokenCookies(HttpServletResponse response, HttpServletRequest request) {
        // request가 null인 경우 운영 환경 기본값 사용
        EnvironmentConfig envConfig = (request != null)
                ? determineEnvironment(request)
                : new EnvironmentConfig("https://kdark.weareshadowpins.co.kr", "api.kdark.weareshadowpins.com", false);

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(!envConfig.isLocal);

        // 로컬 환경이 아닌 경우에만 domain과 sameSite 설정
        if (!envConfig.isLocal) {
            cookieBuilder.domain(envConfig.cookieDomain);
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

    // 요청 출처 사용
    private EnvironmentConfig determineEnvironment(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        log.debug("Request headers - Origin: {}, Referer: {}", origin, referer);

        // 1. Origin 헤더가 있으면 그대로 사용
        if (origin != null && !origin.isEmpty()) {
            boolean isLocal = origin.contains("localhost") || origin.contains("127.0.0.1");
            String cookieDomain = isLocal ? "localhost" : extractDomain(origin);
            return new EnvironmentConfig(origin, cookieDomain, isLocal);
        }

        // 2. Referer에서 origin 추출
        if (referer != null && !referer.isEmpty()) {
            String extractedOrigin = extractOriginFromReferer(referer);
            boolean isLocal = extractedOrigin.contains("localhost") || extractedOrigin.contains("127.0.0.1");
            String cookieDomain = isLocal ? "localhost" : extractDomain(extractedOrigin);
            return new EnvironmentConfig(extractedOrigin, cookieDomain, isLocal);
        }

        // 3. 기본값 (운영환경)
        return new EnvironmentConfig(
                "https://kdark.weareshadowpins.co.kr",
                "api.kdark.weareshadowpins.com",
                false
        );
    }

    private String extractOriginFromReferer(String referer) {
        try {
            URI uri = new URI(referer);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            log.warn("Failed to parse referer: {}", referer, e);
            // fallback: 간단한 문자열 파싱
            try {
                String[] parts = referer.split("/");
                if (parts.length >= 3) {
                    return parts[0] + "//" + parts[2];
                }
            } catch (Exception ex) {
                log.error("Failed to extract origin from referer: {}", referer, ex);
            }
            return referer;
        }
    }

    private String extractDomain(String origin) {
        return origin.replaceAll("^https?://", "");
    }

    private static class EnvironmentConfig {
        String frontendUrl;
        String cookieDomain;
        boolean isLocal;

        EnvironmentConfig(String frontendUrl, String cookieDomain, boolean isLocal) {
            this.frontendUrl = frontendUrl;
            this.cookieDomain = cookieDomain;
            this.isLocal = isLocal;
        }
    }
}