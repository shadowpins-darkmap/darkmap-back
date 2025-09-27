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

            googleTokenService.saveTokens(
                    member.getId(),
                    googleAccessToken,
                    googleRefreshToken,
                    authorizedClient.getAccessToken().getExpiresAt()
            );
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        refreshTokenService.save(
                member.getId(),
                refreshToken,
                LocalDateTime.now().plusDays(7)
        );

        // 동적으로 환경 판단
        String frontendUrl = determineFrontendUrl(request);
        String cookieDomain = determineCookieDomain(request);

        // 쿠키 설정
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(!cookieDomain.equals("localhost"))
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(7));

        if (!cookieDomain.equals("localhost")) {
            cookieBuilder.domain(cookieDomain);
        }

        ResponseCookie refreshCookie = cookieBuilder.build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 동적 리다이렉트
        String redirectUrl = frontendUrl + "/social-redirect-google?success=true&token=" + accessToken;
        response.sendRedirect(redirectUrl);
    }

    private String determineFrontendUrl(HttpServletRequest request) {
        String host = request.getHeader("Host");
        String referer = request.getHeader("Referer");
        String origin = request.getHeader("Origin");

        // 1. Referer 헤더에서 판단 (OAuth 시작점)
        if (referer != null) {
            if (referer.contains("localhost")) {
                return "http://localhost:3000";
            } else if (referer.contains("darkmap-pi.vercel.app")) {
                return "https://darkmap-pi.vercel.app";
            } else if (referer.contains("kdark.weareshadowpins.co.kr")) {
                return "https://kdark.weareshadowpins.co.kr";
            }
        }

        // 2. Origin 헤더에서 판단
        if (origin != null) {
            if (origin.contains("localhost")) {
                return "http://localhost:3000";
            } else if (origin.contains("darkmap-pi.vercel.app")) {
                return "https://darkmap-pi.vercel.app";
            } else if (origin.contains("kdark.weareshadowpins.co.kr")) {
                return "https://kdark.weareshadowpins.co.kr";
            }
        }

        // 3. Host 헤더에서 판단 (API 서버 기준)
        if (host != null) {
            if (host.contains("localhost")) {
                return "http://localhost:3000";
            }
        }

        // 4. 기본값 (운영환경)
        return "https://kdark.weareshadowpins.co.kr";
    }

    private String determineCookieDomain(HttpServletRequest request) {
        String host = request.getHeader("Host");

        if (host != null && host.contains("localhost")) {
            return "localhost";
        }

        return "api.kdark.weareshadowpins.com";
    }
}