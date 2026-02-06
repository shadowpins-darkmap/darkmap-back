package com.sp.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoTokenResponse {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn; // 초 단위
}