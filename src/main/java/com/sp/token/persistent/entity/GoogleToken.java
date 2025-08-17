package com.sp.token.persistent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
public class GoogleToken {

    @Id
    private Long memberId;

    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;

    public GoogleToken(Long memberId, String accessToken, String refreshToken, Instant expiresAt) {
        this.memberId = memberId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}