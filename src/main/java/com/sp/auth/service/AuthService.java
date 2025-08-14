package com.sp.auth.service;

import com.sp.auth.external.KakaoOAuthClient;
import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.KakaoUserInfo;
import com.sp.member.model.type.AuthType;
import com.sp.member.persistent.entity.Member;
import com.sp.member.service.MemberService;
import com.sp.token.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

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

    public void disconnectKakao(String accessToken) {
        kakaoOAuthClient.unlink(accessToken);
    }
}