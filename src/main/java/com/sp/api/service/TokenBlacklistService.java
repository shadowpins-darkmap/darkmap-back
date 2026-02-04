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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository repository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void blacklistToken(String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                Claims claims = jwtTokenProvider.getClaims(token);
                Date expiration = claims.getExpiration();

                LocalDateTime expiresAt = expiration.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                TokenBlacklist blacklist = new TokenBlacklist(token, expiresAt);
                repository.save(blacklist);

                log.info("토큰이 블랙리스트에 추가됨: {}", token.substring(0, 20) + "...");
            }
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 실패", e);
        }
    }

    public boolean isBlacklisted(String token) {
        return repository.existsByToken(token);
    }

    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            repository.deleteExpiredTokens(LocalDateTime.now());
            log.info("만료된 블랙리스트 토큰 정리 완료");
        } catch (Exception e) {
            log.error("블랙리스트 토큰 정리 실패", e);
        }
    }
}