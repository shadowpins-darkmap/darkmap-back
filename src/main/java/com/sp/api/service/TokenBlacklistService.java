package com.sp.api.service;

import com.sp.api.security.jwt.JwtTokenProvider;
import com.sp.api.entity.TokenBlacklist;
import com.sp.api.repository.TokenBlacklistRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 토큰 블랙리스트 추가
     */
    @Transactional
    public void blacklistToken(String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                Claims claims = jwtTokenProvider.getClaims(token);
                Date expiration = claims.getExpiration();

                Instant expiresAt = expiration.toInstant();

                TokenBlacklist blacklist = TokenBlacklist.builder()
                        .token(token)
                        .expiresAt(expiresAt)
                        .reason("LOGOUT")
                        .build();

                tokenBlacklistRepository.save(blacklist);

                log.info("토큰이 블랙리스트에 추가됨: {}...", token.substring(0, 20));
            }
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 실패", e);
        }
    }

    /**
     * 토큰 블랙리스트 추가 (사유 포함)
     */
    @Transactional
    public void blacklistToken(String token, String reason) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                Claims claims = jwtTokenProvider.getClaims(token);
                Date expiration = claims.getExpiration();
                Long memberId = claims.get("userId", Long.class);

                Instant expiresAt = expiration.toInstant();

                TokenBlacklist blacklist = TokenBlacklist.builder()
                        .token(token)
                        .expiresAt(expiresAt)
                        .reason(reason)
                        //.memberId(memberId)
                        .build();

                tokenBlacklistRepository.save(blacklist);

                log.info("토큰이 블랙리스트에 추가됨 - 사유: {}, 회원 ID: {}", reason, memberId);
            }
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 실패", e);
        }
    }

    /**
     * 블랙리스트 여부 확인
     */
    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsById(token);
    }

    /**
     * 회원의 모든 블랙리스트 토큰 조회
     */
    public List<TokenBlacklist> findByMemberId(Long memberId) {
        return tokenBlacklistRepository.findByMemberId(memberId);
    }

    /**
     * 만료된 블랙리스트 토큰 정리 (1시간마다)
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            Instant now = Instant.now();
            List<TokenBlacklist> expiredTokens = tokenBlacklistRepository.findByExpiresAtBefore(now);

            int count = 0;
            for (TokenBlacklist token : expiredTokens) {
                if (token.canBeDeleted()) {
                    tokenBlacklistRepository.delete(token);
                    count++;
                }
            }

            if (count > 0) {
                log.info("만료된 블랙리스트 토큰 {}개 정리 완료", count);
            }
        } catch (Exception e) {
            log.error("블랙리스트 토큰 정리 실패", e);
        }
    }

    /**
     * 특정 기간 이상 경과한 블랙리스트 토큰 강제 삭제
     */
    @Transactional
    public int forceCleanup() {
        try {
            Instant now = Instant.now();
            List<TokenBlacklist> allTokens = tokenBlacklistRepository.findByExpiresAtBefore(now);

            int count = 0;
            for (TokenBlacklist token : allTokens) {
                if (token.canBeDeleted()) {
                    tokenBlacklistRepository.delete(token);
                    count++;
                }
            }

            log.info("강제 정리 완료 - 삭제된 토큰: {}개", count);
            return count;
        } catch (Exception e) {
            log.error("블랙리스트 강제 정리 실패", e);
            return 0;
        }
    }

    /**
     * 전체 블랙리스트 토큰 수
     */
    public long getTotalCount() {
        return tokenBlacklistRepository.count();
    }

    /**
     * 만료되지 않은 블랙리스트 토큰 수
     */
    public long getActiveCount() {
        return tokenBlacklistRepository.findByExpiresAtAfter(Instant.now()).size();
    }

    /**
     * 만료된 블랙리스트 토큰 수
     */
    public long getExpiredCount() {
        return tokenBlacklistRepository.findByExpiresAtBefore(Instant.now()).size();
    }
}