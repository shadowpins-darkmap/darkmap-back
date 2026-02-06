package com.sp.auth.entity;

import com.sp.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "kakao_token", indexes = {
        @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at"),
        @Index(name = "idx_refresh_token_member_id", columnList = "member_id")
})
@EntityListeners(AuditingEntityListener.class)
public class KakaoToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    /**
     * Kakao Access Token
     * 암호화된 토큰 저장 권장
     */
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    /**
     * Kakao Refresh Token
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /**
     * Access Token 만료 시간
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Refresh Token 만료 시간 (선택사항)
     */
    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * 토큰 업데이트 (갱신)
     * Refresh Token이 재발급된 경우
     */
    public void updateTokens(String accessToken, String refreshToken, Instant expiresAt) {
        this.accessToken = accessToken;
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
        this.expiresAt = expiresAt;
    }

    /**
     * Access Token만 갱신
     * Refresh Token은 유지
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

    /**
     * 토큰 만료 임박 확인 (사용자 정의 시간)
     * @param secondsBeforeExpiry 만료 전 확인할 초 단위 시간
     */
    public boolean isExpiringSoon(long secondsBeforeExpiry) {
        return Instant.now().plusSeconds(secondsBeforeExpiry).isAfter(this.expiresAt);
    }

    /**
     * Refresh Token 유효성 확인
     */
    public boolean hasValidRefreshToken() {
        return this.refreshToken != null
                && !this.refreshToken.isBlank()
                && (this.refreshTokenExpiresAt == null || Instant.now().isBefore(this.refreshTokenExpiresAt));
    }

    /**
     * 토큰 무효화 (로그아웃 시)
     */
    public void invalidate() {
        this.accessToken = null;
        this.refreshToken = null;
        this.expiresAt = Instant.now();
    }
}