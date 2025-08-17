package com.sp.auth.service;

import com.sp.auth.external.KakaoOAuthClient;
import com.sp.auth.external.GoogleOAuthClient;
import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.KakaoUserInfo;
import com.sp.member.model.type.AuthType;
import com.sp.member.persistent.entity.Member;
import com.sp.member.service.MemberService;
import com.sp.token.service.GoogleTokenService;
import com.sp.token.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    /**
     * 카카오 로그인 URL redirect
     * @return 카카오 로그인 URL
     */
    public String getKakaoAuthorizeUrl() {
        String url = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";
        return url;
    }

    /**
     * 카카오 로그인 처리
     */
    public AuthResponse loginWithKakao(String code) {
        String accessToken = kakaoOAuthClient.getAccessToken(code);
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(accessToken);

        Member member = memberService.saveIfNotExists(
                userInfo.getEmail(),
                userInfo.getUserId(),
                AuthType.KAKAO
        );

        // Access Token과 Refresh Token 생성
        String jwt = jwtTokenProvider.createAccessToken(member.getId(), member.getLevel());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // Refresh Token을 DB에 저장
        refreshTokenService.save(
                member.getId(),
                refreshToken,
                LocalDateTime.now().plusDays(7)
        );

        return AuthResponse.builder()
                .jwtToken(jwt)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 카카오 연동 해제
     */
    public void disconnectKakao(String accessToken) {
        try {
            kakaoOAuthClient.unlink(accessToken);
            log.info("카카오 연동 해제 완료");
        } catch (Exception e) {
            log.error("카카오 연동 해제 실패: {}", e.getMessage());
            // 카카오 API 실패해도 우리 시스템에서는 탈퇴 처리 진행
        }
    }

    /**
     * 구글 연동 해제 (저장된 토큰 사용)
     */
    public void disconnectGoogle(Long memberId) {
        try {
            // 저장된 구글 토큰 조회 (만료 체크 포함)
            var googleToken = googleTokenService.findValidTokenByMemberId(memberId);

            if (googleToken.isPresent()) {
                String accessToken = googleToken.get().getAccessToken();
                String refreshToken = googleToken.get().getRefreshToken();

                // 스마트 토큰 revoke (만료 처리 포함)
                boolean success = googleOAuthClient.smartRevokeToken(accessToken, refreshToken);

                if (success) {
                    log.info("구글 연동 해제 성공 - 사용자 ID: {}", memberId);
                } else {
                    log.warn("구글 연동 해제 실패했지만 계속 진행 - 사용자 ID: {}", memberId);
                }

                // 저장된 구글 토큰 삭제
                googleTokenService.deleteByMemberId(memberId);

            } else {
                log.warn("저장된 구글 토큰이 없습니다 - 사용자 ID: {}", memberId);
                // 토큰이 없어도 우리 시스템에서는 탈퇴 처리 진행
            }

        } catch (Exception e) {
            log.error("구글 연동 해제 실패 - 사용자 ID: {}", memberId, e);
            // 구글 API 실패해도 우리 시스템에서는 탈퇴 처리 진행
        }
    }

    /**
     * 구글 액세스 토큰으로 연동 해제
     */
    public void disconnectGoogleWithToken(String accessToken) {
        try {
            boolean success = googleOAuthClient.revokeToken(accessToken);
            if (success) {
                log.info("구글 액세스 토큰으로 연동 해제 완료");
            } else {
                log.warn("구글 액세스 토큰 연동 해제 실패 (토큰 만료 가능성)");
            }
        } catch (Exception e) {
            log.error("구글 액세스 토큰 연동 해제 실패: {}", e.getMessage());
            // 구글 API 실패해도 우리 시스템에서는 탈퇴 처리 진행
        }
    }

    /**
     * 구글 리프레시 토큰으로 연동 해제
     */
    public void disconnectGoogleWithRefreshToken(String refreshToken) {
        try {
            boolean success = googleOAuthClient.revokeRefreshToken(refreshToken);
            if (success) {
                log.info("구글 리프레시 토큰으로 연동 해제 완료");
            } else {
                log.warn("구글 리프레시 토큰 연동 해제 실패");
            }
        } catch (Exception e) {
            log.error("구글 리프레시 토큰 연동 해제 실패: {}", e.getMessage());
            // 구글 API 실패해도 우리 시스템에서는 탈퇴 처리 진행
        }
    }
}