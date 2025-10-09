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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
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

        long startTime = System.currentTimeMillis();
        log.info("🔵 OAuth2 Success Handler 시작");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");

        // Member 저장 시간 측정
        long memberStart = System.currentTimeMillis();
        Member member = memberService.saveIfNotExists(email, providerId, AuthType.GOOGLE);
        log.info("✅ Member 저장 완료: {}ms", System.currentTimeMillis() - memberStart);

        // Google Token 저장 시간 측정
        long tokenStart = System.currentTimeMillis();
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
        log.info("✅ Google Token 저장 완료: {}ms", System.currentTimeMillis() - tokenStart);

        // JWT 토큰 생성 시간 측정
        long jwtStart = System.currentTimeMillis();
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        log.info("✅ JWT 토큰 생성 완료: {}ms", System.currentTimeMillis() - jwtStart);

        // RefreshToken 저장 시간 측정
        long refreshStart = System.currentTimeMillis();
        refreshTokenService.save(
                member.getId(),
                refreshToken,
                LocalDateTime.now().plusDays(7)
        );
        log.info("✅ RefreshToken 저장 완료: {}ms", System.currentTimeMillis() - refreshStart);

        // 환경 설정
        EnvironmentConfig envConfig = determineEnvironment(request);
        log.info("OAuth2 Success - Frontend: {}, Cookie Domain: {}, Is Local: {}",
                envConfig.frontendUrl, envConfig.cookieDomain, envConfig.isLocal);

        // 쿠키 설정 및 리다이렉트
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(!envConfig.isLocal)
                .path("/")
                .maxAge(Duration.ofDays(7));

        if (!envConfig.isLocal) {
            cookieBuilder.domain(envConfig.cookieDomain);
            cookieBuilder.sameSite("None");
        } else {
            cookieBuilder.sameSite("Lax");
        }

        ResponseCookie refreshCookie = cookieBuilder.build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        String redirectUrl = envConfig.frontendUrl + "/social-redirect-google?success=true&token=" + accessToken;

        log.info("🔵 OAuth2 Success Handler 총 소요 시간: {}ms", System.currentTimeMillis() - startTime);
        log.info("Redirecting to: {}", redirectUrl);

        response.sendRedirect(redirectUrl);
    }

    // 요청출처 사용
    private EnvironmentConfig determineEnvironment(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        log.debug("OAuth2 Request headers - Origin: {}, Referer: {}", origin, referer);

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