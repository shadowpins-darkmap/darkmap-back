package com.sp.auth.model.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoTokenResponse {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn; // 초 단위
}