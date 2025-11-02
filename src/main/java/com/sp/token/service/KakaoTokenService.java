package com.sp.token.service;

import com.sp.token.persistent.entity.KakaoToken;
import com.sp.token.persistent.repository.KakaoTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoTokenService {

    private final KakaoTokenRepository repository;

    @Transactional
    public void saveTokens(Long memberId, String accessToken, String refreshToken, Instant expiresAt) {
        // 기존 토큰이 있으면 업데이트, 없으면 새로 생성
        Optional<KakaoToken> existingToken = repository.findByMemberId(memberId);

        if (existingToken.isPresent()) {
            // 기존 토큰 업데이트
            KakaoToken token = existingToken.get();
            token.setAccessToken(accessToken);
            token.setRefreshToken(refreshToken);
            token.setExpiresAt(expiresAt);
            repository.save(token);
            log.info("카카오 토큰 업데이트 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        } else {
            // 새 토큰 생성
            KakaoToken kakaoToken = new KakaoToken(memberId, accessToken, refreshToken, expiresAt);
            repository.save(kakaoToken);
            log.info("카카오 토큰 저장 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        }
    }

    public Optional<KakaoToken> findByMemberId(Long memberId) {
        return repository.findByMemberId(memberId);
    }

    /**
     * 액세스 토큰이 만료되었는지 확인
     */
    public boolean isAccessTokenExpired(KakaoToken kakaoToken) {
        if (kakaoToken.getExpiresAt() == null) {
            return false; // 만료시간 정보가 없으면 유효하다고 가정
        }

        boolean expired = Instant.now().isAfter(kakaoToken.getExpiresAt());
        if (expired) {
            log.info("카카오 액세스 토큰 만료됨 - 사용자 ID: {}", kakaoToken.getMemberId());
        }
        return expired;
    }

    /**
     * 유효한 토큰만 반환 (만료 체크 포함)
     */
    public Optional<KakaoToken> findValidTokenByMemberId(Long memberId) {
        Optional<KakaoToken> tokenOpt = repository.findByMemberId(memberId);

        if (tokenOpt.isPresent()) {
            KakaoToken token = tokenOpt.get();
            if (isAccessTokenExpired(token)) {
                log.warn("카카오 액세스 토큰이 만료되었지만 리프레시 토큰은 사용 가능 - 사용자 ID: {}", memberId);
                // 만료되어도 리프레시 토큰이 있으면 반환 (스마트 처리를 위해)
            }
            return tokenOpt;
        }

        return Optional.empty();
    }

    @Transactional
    public void deleteByMemberId(Long memberId) {
        repository.deleteByMemberId(memberId);
        log.info("카카오 토큰 삭제 완료 - 사용자 ID: {}", memberId);
    }
}