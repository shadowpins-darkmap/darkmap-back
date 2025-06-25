package com.sp.auth.service;

import com.sp.auth.external.KakaoOAuthClient;
import com.sp.auth.jwt.JwtTokenProvider;
import com.sp.auth.model.vo.AuthResponse;
import com.sp.auth.model.vo.KakaoUserInfo;
import com.sp.member.model.type.AuthType;
import com.sp.member.persistent.entity.Member;
import com.sp.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    /**
     * 카카오 로그인 URL redirect
     * @return 카카오 로그인 URL
     */
    public String getKakaoAuthorizeUrl() {
        return "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";
    }

    public AuthResponse loginWithKakao(String code) {
        String accessToken = kakaoOAuthClient.getAccessToken(code);
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(accessToken);

        Member member = memberService.saveIfNotExists(
                userInfo.getEmail(),
                userInfo.getUserId(),
                AuthType.KAKAO
        );

        String jwt = jwtTokenProvider.createToken(member.getId(), member.getLevel()); // 백오피스 개발 전까지 임시 level을 role로 사용

        return AuthResponse.builder()
                .jwtToken(jwt)
                .build();
    }

    public void disconnectKakao(String accessToken) {
        kakaoOAuthClient.unlink(accessToken);
    }
}
