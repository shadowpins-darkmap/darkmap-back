package com.sp.auth.oauth;

import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.config.EnvironmentConfig;
import com.sp.exception.WithdrawnMemberException;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.member.service.MemberService;
import com.sp.token.service.RefreshTokenService;
import com.sp.token.service.GoogleTokenService;
import com.sp.util.AsyncRetryUtil;
import com.sp.util.EnvironmentUtil;
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
import java.net.URLEncoder;
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
        log.info("🔐 Google OAuth2 로그인 시작");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");

        try {
            // 1. Member 저장/조회 (동기 - 필수)
            Member member = memberService.saveIfNotExists(email, providerId, AuthType.GOOGLE);

            // ✅ 2. 탈퇴 여부 검증 추가
            if (member.getIsDeleted()) {
                log.warn("🚫 탈퇴한 회원의 구글 로그인 시도 차단 - ID: {}, Email: {}",
                        member.getId(), member.getEmail());

                // 탈퇴 회원은 에러 페이지로 리다이렉트
                EnvironmentConfig envConfig = EnvironmentUtil.determineEnvironment(request);
                String redirectUrl = envConfig.getFrontendUrl() +
                        "/social-redirect-google?success=false&error=WITHDRAWN_MEMBER";
                log.warn("🚫 탈퇴 회원 로그인 차단 - 리다이렉트: {}", redirectUrl);
                response.sendRedirect(redirectUrl);
                return;
            }

            // 3. JWT 토큰 생성 (동기 - 필수)
            String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
            String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

            // 4. Google Token 저장 (비동기 + 재시도)
            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientService.loadAuthorizedClient("google", authentication.getName());

            if (authorizedClient != null) {
                String googleAccessToken = authorizedClient.getAccessToken().getTokenValue();
                String googleRefreshToken = null;

                if (authorizedClient.getRefreshToken() != null) {
                    googleRefreshToken = authorizedClient.getRefreshToken().getTokenValue();
                }

                final String finalGoogleRefreshToken = googleRefreshToken;
                AsyncRetryUtil.executeWithRetry(
                        "Google Token 저장",
                        () -> googleTokenService.saveTokens(
                                member.getId(),
                                googleAccessToken,
                                finalGoogleRefreshToken,
                                authorizedClient.getAccessToken().getExpiresAt()
                        ),
                        3 // 최대 3회 재시도
                );
            }

            // 5. RefreshToken 저장 (비동기 + 재시도)
            AsyncRetryUtil.executeWithRetry(
                    "RefreshToken 저장",
                    () -> refreshTokenService.save(
                            member.getId(),
                            refreshToken,
                            LocalDateTime.now().plusDays(7)
                    ),
                    3
            );

            // 6. 환경 설정 및 쿠키 설정
            EnvironmentConfig envConfig = EnvironmentUtil.determineEnvironment(request);

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

            // 7. 즉시 리다이렉트 (DB 저장 완료 대기 안 함)
            String redirectUrl = envConfig.getFrontendUrl() + "/social-redirect-google?success=true&token=" + accessToken;

            log.info("✅ Google OAuth2 로그인 완료 - ID: {}, 소요시간: {}ms (비동기 작업 제외)",
                    member.getId(), System.currentTimeMillis() - startTime);
            log.info("Redirecting to: {}", redirectUrl);

            response.sendRedirect(redirectUrl);

        } catch (WithdrawnMemberException e) {
            // WithdrawnMemberException이 발생한 경우 (만약 saveIfNotExists에서 던진다면)
            EnvironmentConfig envConfig = EnvironmentUtil.determineEnvironment(request);
            String redirectUrl = envConfig.getFrontendUrl() +
                    "/social-redirect-google?success=false&error=WITHDRAWN_MEMBER";
            log.warn("🚫 탈퇴 회원 로그인 차단 - 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("❌ Google OAuth2 로그인 실패", e);
            EnvironmentConfig envConfig = EnvironmentUtil.determineEnvironment(request);
            String redirectUrl = envConfig.getFrontendUrl() +
                    "/social-redirect-google?success=false&error=SERVER_ERROR";
            response.sendRedirect(redirectUrl);
        }
    }
}