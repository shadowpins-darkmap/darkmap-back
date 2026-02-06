package com.sp.auth.service;

import com.sp.member.service.MemberService;
import com.sp.auth.oauth.client.KakaoOAuthClient;
import com.sp.auth.oauth.client.GoogleOAuthClient;
import com.sp.auth.security.jwt.JwtTokenProvider;
import com.sp.auth.dto.response.AuthResponse;
import com.sp.auth.dto.response.KakaoTokenResponse;
import com.sp.auth.dto.response.KakaoUserInfo;
import com.sp.exception.WithdrawnMemberException;
import com.sp.auth.enums.AuthType;
import com.sp.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenService googleTokenService;
    private final KakaoTokenService kakaoTokenService;

    /**
     * 카카오 로그인 URL redirect
     */
    public String getKakaoAuthorizeUrl(String state) {
        return "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + (state != null ? "&state=" + state : "");
    }

    @Transactional
    public AuthResponse loginWithKakao(String code) {
        long startTime = System.currentTimeMillis();
        log.info("🔐 카카오 로그인 시작");

        // 1. 카카오 토큰 및 사용자 정보 조회
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.getTokenResponse(code);
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(tokenResponse.getAccessToken());

        // 2. 회원 저장/조회
        Member member = memberService.saveIfNotExists(
                userInfo.getEmail(),
                userInfo.getUserId(),
                AuthType.KAKAO
        );

        // 3. 탈퇴 여부 검증
        if (member.getIsDeleted()) {
            log.warn("🚫 탈퇴한 회원의 로그인 시도 차단 - ID: {}, Email: {}",
                    member.getId(), member.getEmail());
            throw new WithdrawnMemberException("탈퇴한 회원은 재가입이 불가능합니다.");
        }

        // 4. JWT 토큰 생성
        String jwt = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // 5. 카카오 토큰 저장
        Instant expiresAt = Instant.now().plusSeconds(tokenResponse.getExpiresIn());
        try {
            kakaoTokenService.saveTokens(
                    member.getId(),
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    expiresAt
            );
        } catch (Exception e) {
            log.error("❌ 카카오 토큰 저장 실패 - 사용자 ID: {}", member.getId(), e);
        }

        // 6. Refresh Token 저장
        try {
            refreshTokenService.save(
                    member.getId(),
                    refreshToken,
                    Instant.now().plusSeconds(7L * 24 * 60 * 60) // 7일
            );
        } catch (Exception e) {
            log.error("❌ RefreshToken 저장 실패 - 사용자 ID: {}", member.getId(), e);
        }

        log.info("✅ 카카오 로그인 완료 - ID: {}, 소요시간: {}ms",
                member.getId(), System.currentTimeMillis() - startTime);

        return AuthResponse.builder()
                .jwtToken(jwt)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .email(member.getEmail())
                .userId(member.getId())
                .nickname(member.getNickname())
                .loginCount(member.getLoginCount())
                .build();
    }

    /**
     * 카카오 연동 해제
     */
    @Transactional
    public void disconnectKakao(Long memberId) {
        try {
            var kakaoToken = kakaoTokenService.findValidTokenByMemberId(memberId);

            if (kakaoToken.isPresent()) {
                String accessToken = kakaoToken.get().getAccessToken();

                try {
                    kakaoOAuthClient.unlink(accessToken);
                    log.info("✅ 카카오 연동 해제 성공 - 사용자 ID: {}", memberId);
                } catch (Exception e) {
                    log.warn("⚠️ 카카오 API 연동 해제 실패 (토큰 만료 가능성) - 사용자 ID: {}", memberId);
                }

                kakaoTokenService.deleteByMemberId(memberId);
            } else {
                log.warn("⚠️ 저장된 카카오 토큰이 없습니다 - 사용자 ID: {}", memberId);
            }

        } catch (Exception e) {
            log.error("❌ 카카오 연동 해제 실패 - 사용자 ID: {}", memberId, e);
        }
    }

    /**
     * 카카오 연동 해제 (클라이언트 토큰 사용 - 하위 호환성)
     */
    @Deprecated
    public void disconnectKakao(String accessToken) {
        try {
            kakaoOAuthClient.unlink(accessToken);
            log.info("✅ 카카오 연동 해제 완료");
        } catch (Exception e) {
            log.error("❌ 카카오 연동 해제 실패: {}", e.getMessage());
        }
    }

    /**
     * 구글 연동 해제
     */
    @Transactional
    public void disconnectGoogle(Long memberId) {
        try {
            var googleToken = googleTokenService.findValidTokenByMemberId(memberId);

            if (googleToken.isPresent()) {
                String accessToken = googleToken.get().getAccessToken();
                String refreshToken = googleToken.get().getRefreshToken();

                boolean success = googleOAuthClient.smartRevokeToken(accessToken, refreshToken);

                if (success) {
                    log.info("✅ 구글 연동 해제 성공 - 사용자 ID: {}", memberId);
                } else {
                    log.warn("⚠️ 구글 연동 해제 실패했지만 계속 진행 - 사용자 ID: {}", memberId);
                }

                googleTokenService.deleteByMemberId(memberId);
            } else {
                log.warn("⚠️ 저장된 구글 토큰이 없습니다 - 사용자 ID: {}", memberId);
            }

        } catch (Exception e) {
            log.error("❌ 구글 연동 해제 실패 - 사용자 ID: {}", memberId, e);
        }
    }

    /**
     * 구글 액세스 토큰으로 연동 해제
     */
    @Deprecated
    public void disconnectGoogleWithToken(String accessToken) {
        try {
            boolean success = googleOAuthClient.revokeToken(accessToken);
            if (success) {
                log.info("✅ 구글 액세스 토큰으로 연동 해제 완료");
            } else {
                log.warn("⚠️ 구글 액세스 토큰 연동 해제 실패 (토큰 만료 가능성)");
            }
        } catch (Exception e) {
            log.error("❌ 구글 액세스 토큰 연동 해제 실패: {}", e.getMessage());
        }
    }

    /**
     * 구글 리프레시 토큰으로 연동 해제
     */
    @Deprecated
    public void disconnectGoogleWithRefreshToken(String refreshToken) {
        try {
            boolean success = googleOAuthClient.revokeRefreshToken(refreshToken);
            if (success) {
                log.info("✅ 구글 리프레시 토큰으로 연동 해제 완료");
            } else {
                log.warn("⚠️ 구글 리프레시 토큰 연동 해제 실패");
            }
        } catch (Exception e) {
            log.error("❌ 구글 리프레시 토큰 연동 해제 실패: {}", e.getMessage());
        }
    }
}