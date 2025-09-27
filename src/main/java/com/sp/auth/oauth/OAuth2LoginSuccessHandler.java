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
import org.springframework.beans.factory.annotation.Value;
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

    // application.properties에서 프론트엔드 URL 설정
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    // application.properties에서 쿠키 도메인 설정
    @Value("${cookie.domain}")
    private String cookieDomain;

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

        // 우리 시스템의 JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // Refresh Token을 DB에 저장
        refreshTokenService.save(
                member.getId(),
                refreshToken,
                LocalDateTime.now().plusDays(7)
        );

        // 환경에 따른 쿠키 설정
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(!cookieDomain.equals("localhost")) // localhost일 때는 secure false
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(7));

        // localhost가 아닌 경우에만 domain 설정
        if (!cookieDomain.equals("localhost")) {
            cookieBuilder.domain(cookieDomain);
        }

        ResponseCookie refreshCookie = cookieBuilder.build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 환경별 동적 리다이렉트 URL
        String redirectUrl = frontendBaseUrl + "/social-redirect-google?success=true&token=" + accessToken;
        response.sendRedirect(redirectUrl);
    }
}