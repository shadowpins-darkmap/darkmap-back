package com.sp.auth.model.vo;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String jwtToken;
    private String email;
    private String userId;
    private String nickname;
    private String accessToken;
    private int loginCount;
}