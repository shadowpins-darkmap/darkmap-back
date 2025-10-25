package com.sp.auth.oauth;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.config.EnvironmentConfig;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.member.service.MemberService;
import com.sp.token.service.RefreshTokenService;
import com.sp.token.service.GoogleTokenService;
import com.sp.util.EnvironmentUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

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

        // 1. Member 저장 (동기 - 필수)
        long memberStart = System.currentTimeMillis();
        Member member = memberService.saveIfNotExists(email, providerId, AuthType.GOOGLE);
        log.info("✅ Member 저장 완료: {}ms", System.currentTimeMillis() - memberStart);

        // 2. JWT 토큰 생성 (동기 - 필수)
        long jwtStart = System.currentTimeMillis();
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        log.info("✅ JWT 토큰 생성 완료: {}ms", System.currentTimeMillis() - jwtStart);

        // 3. Google Token 저장 (비동기)
        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient("google", authentication.getName());

        if (authorizedClient != null) {
            String googleAccessToken = authorizedClient.getAccessToken().getTokenValue();
            String googleRefreshToken = null;

            if (authorizedClient.getRefreshToken() != null) {
                googleRefreshToken = authorizedClient.getRefreshToken().getTokenValue();
            }

            // 비동기로 Google Token 저장
            final String finalGoogleRefreshToken = googleRefreshToken;
            CompletableFuture.runAsync(() -> {
                try {
                    googleTokenService.saveTokens(
                            member.getId(),
                            googleAccessToken,
                            finalGoogleRefreshToken,
                            authorizedClient.getAccessToken().getExpiresAt()
                    );
                    log.info("✅ Google Token 비동기 저장 완료");
                } catch (Exception e) {
                    log.error("❌ Google Token 저장 실패", e);
                }
            });
        }

        // 4. RefreshToken 저장 (비동기)
        CompletableFuture.runAsync(() -> {
            try {
                refreshTokenService.save(
                        member.getId(),
                        refreshToken,
                        LocalDateTime.now().plusDays(7)
                );
                log.info("✅ RefreshToken 비동기 저장 완료");
            } catch (Exception e) {
                log.error("❌ RefreshToken 저장 실패", e);
            }
        });

        // 5. 환경 설정 및 쿠키 설정
        EnvironmentConfig envConfig = EnvironmentUtil.determineEnvironment(request);
        log.info("OAuth2 Success - Frontend: {}, Cookie Domain: {}, Is Local: {}",
                envConfig.getFrontendUrl(), envConfig.getCookieDomain(), envConfig.isLocal());

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
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 6. 즉시 리다이렉트 (DB 저장 완료 대기 안 함)
        String redirectUrl = envConfig.getFrontendUrl() + "/social-redirect-google?success=true&token=" + accessToken;

        log.info("🔵 OAuth2 Success Handler 총 소요 시간: {}ms (비동기 작업 제외)", System.currentTimeMillis() - startTime);
        log.info("Redirecting to: {}", redirectUrl);

        response.sendRedirect(redirectUrl);
    }
}