package com.sp.auth.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String jwtToken;
    private String refreshToken;
    private Long expiresIn;
    private String email;
    private Long userId;
    private String nickname;
    private String accessToken;
    private int loginCount;
}