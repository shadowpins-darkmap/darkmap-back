package com.sp.auth.model.vo;

import lombok.Getter;

@Getter
public class KakaoUserInfo {
    private final String email;
    private final String userId;

    public KakaoUserInfo(String email, String userId) {
        this.email = email;
        this.userId = userId;
    }
}