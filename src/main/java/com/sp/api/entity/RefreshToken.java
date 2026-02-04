package com.sp.api.entity;

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
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at"),
        @Index(name = "idx_refresh_token_member_id", columnList = "member_id")
})
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    /**
     * JWT Refresh Token
     * 암호화된 토큰 저장 권장 (AES-256 등)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String token;

    /**
     * 토큰 만료 시간
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 토큰 생성 시간
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 토큰 갱신 시간
     */
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * 마지막 사용 시간 (선택사항)
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * 토큰 갱신 횟수 추적 (선택사항)
     */
    @Column(name = "refresh_count", columnDefinition = "INT default 0")
    private Integer refreshCount;

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * 토큰 만료 임박 확인 (1일 이내)
     */
    public boolean isExpiringSoon() {
        return Instant.now().plusSeconds(24 * 60 * 60).isAfter(this.expiresAt);
    }

    /**
     * 토큰 만료 임박 확인 (사용자 정의 시간)
     * @param secondsBeforeExpiry 만료 전 확인할 초 단위 시간
     */
    public boolean isExpiringSoon(long secondsBeforeExpiry) {
        return Instant.now().plusSeconds(secondsBeforeExpiry).isAfter(this.expiresAt);
    }

    /**
     * 토큰 사용 기록 업데이트
     */
    public void recordUsage() {
        this.lastUsedAt = Instant.now();
        if (this.refreshCount == null) {
            this.refreshCount = 1;
        } else {
            this.refreshCount++;
        }
    }

    /**
     * 토큰 갱신
     * @param newToken 새로운 토큰 값
     * @param expiresAt 새로운 만료 시간
     */
    public void renewToken(String newToken, Instant expiresAt) {
        this.token = newToken;
        this.expiresAt = expiresAt;
        recordUsage();
    }

    /**
     * 토큰 무효화
     */
    public void invalidate() {
        this.token = null;
        this.expiresAt = Instant.now();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isValid() {
        return this.token != null
                && !this.token.isBlank()
                && !isExpired();
    }
}