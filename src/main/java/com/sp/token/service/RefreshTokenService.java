package com.sp.token.service;

import com.sp.token.persistent.entity.RefreshToken;
import com.sp.token.persistent.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    public void save(Long memberId, String token, LocalDateTime expiry) {
        RefreshToken refreshToken = new RefreshToken(memberId, token, expiry);
        repository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    public void deleteByMemberId(Long memberId) {
        repository.deleteById(memberId);
    }
}