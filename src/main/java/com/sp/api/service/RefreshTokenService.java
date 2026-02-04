package com.sp.api.service;

import com.sp.api.entity.RefreshToken;
import com.sp.api.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repository;

    @Transactional
    public void save(Long memberId, String token, LocalDateTime expiry) {
        // 기존 토큰 삭제 후 새로 저장 (중복 방지)
        repository.deleteByMemberId(memberId);
        RefreshToken refreshToken = new RefreshToken(memberId, token, expiry);
        repository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token)
                .filter(rt -> rt.getExpiryDate().isAfter(LocalDateTime.now())); // 만료 체크
    }

    @Transactional
    public void deleteByMemberId(Long memberId) {
        repository.deleteByMemberId(memberId);
    }

    @Transactional
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }
}