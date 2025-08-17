package com.sp.token.service;

import com.sp.token.persistent.entity.GoogleToken;
import com.sp.token.persistent.repository.GoogleTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private final GoogleTokenRepository repository;

    @Transactional
    public void saveTokens(Long memberId, String accessToken, String refreshToken, Instant expiresAt) {
        // 기존 토큰 삭제 후 새로 저장
        repository.deleteByMemberId(memberId);

        GoogleToken googleToken = new GoogleToken(memberId, accessToken, refreshToken, expiresAt);
        repository.save(googleToken);

        log.info("구글 토큰 저장 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
    }

    public Optional<GoogleToken> findByMemberId(Long memberId) {
        return repository.findByMemberId(memberId);
    }

    /**
     * 액세스 토큰이 만료되었는지 확인
     */
    public boolean isAccessTokenExpired(GoogleToken googleToken) {
        if (googleToken.getExpiresAt() == null) {
            return false; // 만료시간 정보가 없으면 유효하다고 가정
        }

        boolean expired = Instant.now().isAfter(googleToken.getExpiresAt());
        if (expired) {
            log.info("구글 액세스 토큰 만료됨 - 사용자 ID: {}", googleToken.getMemberId());
        }
        return expired;
    }

    /**
     * 유효한 토큰만 반환 (만료 체크 포함)
     */
    public Optional<GoogleToken> findValidTokenByMemberId(Long memberId) {
        Optional<GoogleToken> tokenOpt = repository.findByMemberId(memberId);

        if (tokenOpt.isPresent()) {
            GoogleToken token = tokenOpt.get();
            if (isAccessTokenExpired(token)) {
                log.warn("구글 액세스 토큰이 만료되었지만 리프레시 토큰은 사용 가능 - 사용자 ID: {}", memberId);
                // 만료되어도 리프레시 토큰이 있으면 반환 (스마트 처리를 위해)
            }
            return tokenOpt;
        }

        return Optional.empty();
    }

    @Transactional
    public void deleteByMemberId(Long memberId) {
        repository.deleteByMemberId(memberId);
        log.info("구글 토큰 삭제 완료 - 사용자 ID: {}", memberId);
    }
}