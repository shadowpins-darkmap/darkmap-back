package com.sp.token.persistent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "google_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleToken {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    /**
     * Google Access Token
     * columnDefinition = "TEXT"로 충분한 길이 확보 (최대 65,535자)
     */
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    /**
     * Google Refresh Token
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /**
     * Access Token 만료 시간
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 토큰 업데이트 (갱신)
     */
    public void updateTokens(String accessToken, String refreshToken, Instant expiresAt) {
        this.accessToken = accessToken;
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
        }
        this.expiresAt = expiresAt;
    }

    /**
     * Access Token만 갱신
     */
    public void updateAccessToken(String accessToken, Instant expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * 토큰 만료 임박 확인 (5분 이내)
     */
    public boolean isExpiringSoon() {
        return Instant.now().plusSeconds(300).isAfter(this.expiresAt);
    }
}