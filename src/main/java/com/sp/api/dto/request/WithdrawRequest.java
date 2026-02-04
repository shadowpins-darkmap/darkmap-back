package com.sp.api.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawRequest {
    private String kakaoAccessToken;
    private String googleAccessToken;
    private String googleRefreshToken;
}
