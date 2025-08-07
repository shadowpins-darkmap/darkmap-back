package com.sp.auth.controller;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.WithdrawRequest;
import com.sp.auth.service.AuthService;
import com.sp.member.service.MemberService;
import com.sp.token.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/login/kakao")
    public void redirectToKakao(HttpServletResponse response) throws IOException {
        String redirectUrl = authService.getKakaoAuthorizeUrl();
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/login/kakao/callback")
    public void kakaoCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        AuthResponse authResponse = authService.loginWithKakao(code);
        setTokensAndRedirect(authResponse, response);
    }

    @DeleteMapping("/withdraw/kakao")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal Long id,
                                      @RequestBody WithdrawRequest request,
                                      HttpServletResponse response) {
        memberService.withdraw(id);
        // Refresh Token 삭제
        refreshTokenService.deleteByMemberId(id);
        // 쿠키 삭제
        clearTokenCookies(response);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Long id,
                                    HttpServletResponse response) {
        // Refresh Token 삭제
        refreshTokenService.deleteByMemberId(id);
        // 쿠키 삭제
        clearTokenCookies(response);

        return ResponseEntity.ok().build();
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

    private void setTokensAndRedirect(AuthResponse authResponse, HttpServletResponse response) throws IOException {
        // Refresh Token을 API 서버 도메인의 쿠키에 저장
        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .domain("api.kdark.weareshadowpins.com") // API 서버 도메인 명시
                    .sameSite("None")
                    .maxAge(Duration.ofDays(7))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        // 프론트엔드로 리다이렉트
        String redirectUrl = buildTokenRedirectUrl(authResponse.getJwtToken());
        response.sendRedirect(redirectUrl);
    }

    private String buildTokenRedirectUrl(String accessToken) {
        // 임시
        return "https://darkmap-pi.vercel.app/social-redirect?success=true&token=" + accessToken;
        //return "https://kdark.weareshadowpins.com?success=true&token=" + accessToken;
        //return "https://localhost:8080?success=true&token=" + accessToken;
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie clearRefreshCookie = ResponseCookie.from("refresh_token", "")
                .path("/")
                .domain("api.kdark.weareshadowpins.com")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
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