package com.sp.auth.controller;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.WithdrawRequest;
import com.sp.auth.service.AuthService;
import com.sp.member.service.MemberService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;

    @GetMapping("/login/kakao")
    public void redirectToKakao(HttpServletResponse response) throws IOException {
        String redirectUrl = authService.getKakaoAuthorizeUrl();
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/login/kakao/callback")
    public void kakaoCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
    //public ResponseEntity<AuthResponse> kakaoCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        AuthResponse authResponse = authService.loginWithKakao(code);

        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", authResponse.getJwtToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofMinutes(15))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.sendRedirect("https://kdark.weareshadowpins.com/");

        // 리다이렉트 없이 사용자 정보 JSON 반환 ResponseEntity<AuthResponse>
        //return ResponseEntity.ok(authResponse);
    }

    @DeleteMapping("/withdraw/kakao")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal Long id,
                                      @RequestBody WithdrawRequest request) {
        memberService.withdraw(id);
        //authService.disconnectKakao(request.getKakaoAccessToken());
        //refreshTokenService.deleteByMemberId(memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Long id,
                                    HttpServletResponse response) {
        //refreshTokenService.deleteByMemberId(id);
        ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok().build();
    }
}
