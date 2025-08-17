package com.sp.auth.oauth;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.member.service.MemberService;
import com.sp.token.service.RefreshTokenService;
import com.sp.token.service.GoogleTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GoogleTokenService googleTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");

        Member member = memberService.saveIfNotExists(email, providerId, AuthType.GOOGLE);

        // 구글 액세스 토큰 추출 및 저장
        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient("google", authentication.getName());

        if (authorizedClient != null) {
            String googleAccessToken = authorizedClient.getAccessToken().getTokenValue();
            String googleRefreshToken = null;

            if (authorizedClient.getRefreshToken() != null) {
                googleRefreshToken = authorizedClient.getRefreshToken().getTokenValue();
            }

            // 구글 토큰들을 별도 테이블에 저장
            googleTokenService.saveTokens(
                    member.getId(),
                    googleAccessToken,
                    googleRefreshToken,
                    authorizedClient.getAccessToken().getExpiresAt()
            );
        }

        // 우리 시스템의 JWT 토큰 생성 (기존과 동일)
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // Refresh Token을 DB에 저장
        refreshTokenService.save(
                member.getId(),
                refreshToken,
                LocalDateTime.now().plusDays(7)
        );

        // Refresh Token을 쿠키에 저장 (기존과 동일)
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .domain("api.kdark.weareshadowpins.com")
                .sameSite("None")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 프론트엔드로 리다이렉트 (우리 JWT 토큰 전달)
        //String redirectUrl = "https://kdark.weareshadowpins.co.kr/social-redirect-google?success=true&token=" + accessToken;
        String redirectUrl = "https://darkmap-pi.vercel.app/social-redirect-google?success=true&token=" + accessToken;
        response.sendRedirect(redirectUrl);
    }
}