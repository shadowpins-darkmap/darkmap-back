package com.sp.auth.oauth.handler;

import com.sp.auth.controller.AuthBridgeResponder;
import com.sp.auth.dto.response.AuthResponse;
import com.sp.auth.security.jwt.JwtTokenProvider;
import com.sp.config.EnvironmentConfig;
import com.sp.config.EnvironmentResolver;
import com.sp.exception.WithdrawnMemberException;
import com.sp.member.entity.Member;
import com.sp.auth.enums.AuthType;
import com.sp.member.service.MemberService;
import com.sp.auth.service.RefreshTokenService;
import com.sp.auth.service.GoogleTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

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
    private final AuthBridgeResponder authBridgeResponder;
    @Value("${auth.rejoin-hold-days:7}")
    private int rejoinHoldDays;

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
            Duration hold = Duration.ofDays(rejoinHoldDays);
//            if (member.isRejoinBlocked(hold)) {
//                log.warn("🚫 탈퇴한 회원의 구글 로그인 시도 차단 - ID: {}, Email: {}",
//                        member.getId(), member.getEmail());
//
//                redirectWithError(response, envConfig, "WITHDRAWN_MEMBER");
//                return;
//            }

            // 3. JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
            String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .tokenType("Bearer")
                    .email(member.getEmail())
                    .userId(member.getId())
                    .nickname(member.getNickname())
                    .loginCount(member.getLoginCount())
                    .build();

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

            // 6. postMessage bridge로 토큰 전달
            log.info("✅ Google OAuth2 로그인 완료 - ID: {}, 소요시간: {}ms",
                    member.getId(), System.currentTimeMillis() - startTime);
            authBridgeResponder.writeSuccess(response, envConfig, authResponse);

        } catch (WithdrawnMemberException e) {
            redirectWithError(response, envConfig, "WITHDRAWN_MEMBER");
        } catch (Exception e) {
            log.error("❌ Google OAuth2 로그인 실패", e);
            redirectWithError(response, envConfig, "SERVER_ERROR");
        }
    }

    private void redirectWithError(HttpServletResponse response,
                                   EnvironmentConfig envConfig,
                                   String errorCode) throws IOException {
        log.warn("Google OAuth redirect with error {}", errorCode);
        authBridgeResponder.writeError(response, envConfig, errorCode);
    }
}
