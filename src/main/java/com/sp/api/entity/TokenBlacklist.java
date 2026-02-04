package com.sp.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "token_blacklist", indexes = {
        @Index(name = "idx_token_blacklist_expires_at", columnList = "expires_at"),
        @Index(name = "idx_token_blacklist_blacklisted_at", columnList = "blacklisted_at")
})
@EntityListeners(AuditingEntityListener.class)
public class TokenBlacklist {

    /**
     * 블랙리스트에 등록된 토큰 (JWT Access Token)
     * 해시값 저장 권장 (SHA-256)
     */
    @Id
    @Column(name = "token", columnDefinition = "VARCHAR(512)")
    private String token;

    /**
     * 블랙리스트 등록 시간
     */
    @CreatedDate
    @Column(name = "blacklisted_at", nullable = false, updatable = false)
    private Instant blacklistedAt;

    /**
     * 원본 토큰 만료 시간
     * 이 시간 이후 블랙리스트에서 제거 가능
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 블랙리스트 등록 사유 (선택사항)
     */
    @Column(name = "reason", length = 100)
    private String reason;

    /**
     * 연관된 회원 ID (선택사항, 추적용)
     */
    @Column(name = "member_id")
    private Long memberId;

    /**
     * 생성자 (간편 생성)
     */
    public TokenBlacklist(String token, Instant expiresAt) {
        this.token = token;
        this.blacklistedAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    /**
     * 생성자 (사유 포함)
     */
    public TokenBlacklist(String token, Instant expiresAt, String reason) {
        this.token = token;
        this.blacklistedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    /**
     * 생성자 (사유 및 회원 ID 포함)
     */
    public TokenBlacklist(String token, Instant expiresAt, String reason, Long memberId) {
        this.token = token;
        this.blacklistedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.memberId = memberId;
    }

    /**
     * 만료 여부 확인
     * 원본 토큰이 만료되었으면 블랙리스트에서 제거 가능
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * 블랙리스트 정리 가능 여부
     * 토큰이 만료되었고, 블랙리스트 등록 후 충분한 시간이 지났는지 확인
     */
    public boolean canBeDeleted() {
        // 토큰 만료 + 블랙리스트 등록 후 7일 경과
        Instant sevenDaysAfterBlacklist = this.blacklistedAt.plusSeconds(7L * 24 * 60 * 60);
        return isExpired() && Instant.now().isAfter(sevenDaysAfterBlacklist);
    }

    /**
     * 블랙리스트 등록 후 경과 시간 (초 단위)
     */
    public long getSecondsSinceBlacklisted() {
        return Instant.now().getEpochSecond() - this.blacklistedAt.getEpochSecond();
    }
}