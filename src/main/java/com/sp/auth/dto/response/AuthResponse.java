package com.sp.auth.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private String email;
    private Long userId;
    private String nickname;
    private int loginCount;
}
