package com.sp.api.oauth.handler;

import com.sp.api.security.jwt.JwtTokenProvider;
import com.sp.config.EnvironmentConfig;
import com.sp.config.EnvironmentResolver;
import com.sp.exception.WithdrawnMemberException;
import com.sp.api.entity.Member;
import com.sp.api.enums.AuthType;
import com.sp.api.service.MemberService;
import com.sp.api.service.RefreshTokenService;
import com.sp.api.service.GoogleTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GoogleTokenService googleTokenService;
    private final EnvironmentResolver environmentResolver;

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String ACCESS_COOKIE = "access_token";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        long startTime = System.currentTimeMillis();
        log.info("🔐 Google OAuth2 로그인 시작");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");

        EnvironmentConfig envConfig = environmentResolver.resolve(request);

        try {
            // 1. Member 저장/조회
            Member member = memberService.saveIfNotExists(email, providerId, AuthType.GOOGLE);

            // 2. 탈퇴 여부 검증
            if (member.getIsDeleted()) {
                log.warn("🚫 탈퇴한 회원의 구글 로그인 시도 차단 - ID: {}, Email: {}",
                        member.getId(), member.getEmail());

                redirectWithError(response, envConfig, "WITHDRAWN_MEMBER");
                return;
            }

            // 3. JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
            String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

            // 4. Google Token 저장
            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientService.loadAuthorizedClient("google", authentication.getName());

            if (authorizedClient != null) {
                String googleAccessToken = authorizedClient.getAccessToken().getTokenValue();
                String googleRefreshToken = null;

                if (authorizedClient.getRefreshToken() != null) {
                    googleRefreshToken = authorizedClient.getRefreshToken().getTokenValue();
                }

                final String finalGoogleRefreshToken = googleRefreshToken;
                try {
                    googleTokenService.saveTokens(
                            member.getId(),
                            googleAccessToken,
                            finalGoogleRefreshToken,
                            authorizedClient.getAccessToken().getExpiresAt()
                    );
                } catch (Exception e) {
                    log.error("❌ Google Token 저장 실패 - 사용자 ID: {}", member.getId(), e);
                }
            }

            // 5. RefreshToken 저장 (Instant 사용)
            try {
                refreshTokenService.save(
                        member.getId(),
                        refreshToken,
                        Instant.now().plusSeconds(7L * 24 * 60 * 60) // 7일
                );
            } catch (Exception e) {
                log.error("❌ RefreshToken 저장 실패 - 사용자 ID: {}", member.getId(), e);
            }

            // 6. 환경 설정 및 쿠키 설정
            addCookie(response, envConfig, REFRESH_COOKIE, refreshToken, Duration.ofDays(7));
            addCookie(response, envConfig, ACCESS_COOKIE, accessToken,
                    Duration.ofMillis(jwtTokenProvider.getExpirationTime()));

            // 7. 즉시 리다이렉트
            String redirectUrl = envConfig.getFrontendUrl() + "/login?success=true";

            log.info("✅ Google OAuth2 로그인 완료 - ID: {}, 소요시간: {}ms",
                    member.getId(), System.currentTimeMillis() - startTime);
            log.info("Redirecting to: {}", redirectUrl);

            response.sendRedirect(redirectUrl);

        } catch (WithdrawnMemberException e) {
            redirectWithError(response, envConfig, "WITHDRAWN_MEMBER");
        } catch (Exception e) {
            log.error("❌ Google OAuth2 로그인 실패", e);
            redirectWithError(response, envConfig, "SERVER_ERROR");
        }
    }

    private void addCookie(HttpServletResponse response,
                           EnvironmentConfig envConfig,
                           String name,
                           String value,
                           Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(!envConfig.isLocal())
                .path("/")
                .maxAge(maxAge);

        if (!envConfig.isLocal() && envConfig.getCookieDomain() != null) {
            builder.domain(envConfig.getCookieDomain());
        }
        builder.sameSite(envConfig.isLocal() ? "Lax" : "None");

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void redirectWithError(HttpServletResponse response,
                                   EnvironmentConfig envConfig,
                                   String errorCode) throws IOException {
        String redirectUrl = envConfig.getFrontendUrl() +
                "/login?success=false&error=" + errorCode;
        log.warn("Google OAuth redirect with error {} -> {}", errorCode, redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}