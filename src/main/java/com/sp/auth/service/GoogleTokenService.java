package com.sp.auth.service;

import com.sp.auth.entity.GoogleToken;
import com.sp.member.entity.Member;
import com.sp.auth.repository.GoogleTokenRepository;
import com.sp.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private final GoogleTokenRepository googleTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * 토큰 저장/업데이트 (Upsert)
     */
    @Transactional
    public void saveTokens(Long memberId, String accessToken, String refreshToken, Instant expiresAt) {
        Optional<GoogleToken> existingToken = googleTokenRepository.findById(memberId);

        if (existingToken.isPresent()) {
            GoogleToken token = existingToken.get();
            token.updateTokens(accessToken, refreshToken, expiresAt);
            googleTokenRepository.save(token);
            log.info("구글 토큰 업데이트 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        } else {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + memberId));

            GoogleToken googleToken = GoogleToken.builder()
                    //.memberId(memberId)
                    .member(member)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .build();

            googleTokenRepository.save(googleToken);
            log.info("구글 토큰 저장 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        }
    }

    /**
     * Access Token만 갱신
     */
    @Transactional
    public void updateAccessToken(Long memberId, String accessToken, Instant expiresAt) {
        GoogleToken token = googleTokenRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 구글 토큰이 없습니다: " + memberId));

        token.updateAccessToken(accessToken, expiresAt);
        googleTokenRepository.save(token);
        log.info("구글 액세스 토큰 갱신 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
    }

    /**
     * memberId로 토큰 조회
     */
    public Optional<GoogleToken> findByMemberId(Long memberId) {
        return googleTokenRepository.findById(memberId);
    }

    /**
     * 유효한 토큰만 반환 (만료되지 않은 토큰)
     */
    public Optional<GoogleToken> findValidTokenByMemberId(Long memberId) {
        Optional<GoogleToken> tokenOpt = googleTokenRepository.findById(memberId);

        if (tokenOpt.isPresent()) {
            GoogleToken token = tokenOpt.get();

            // Access Token이 만료되었지만 Refresh Token이 있으면 반환
            if (token.isExpired()) {
                if (token.hasValidRefreshToken()) {
                    log.info("구글 액세스 토큰 만료됨, 리프레시 토큰 사용 가능 - 사용자 ID: {}", memberId);
                    return tokenOpt;
                } else {
                    log.warn("구글 토큰 완전 만료 - 사용자 ID: {}", memberId);
                    return Optional.empty();
                }
            }

            return tokenOpt;
        }

        return Optional.empty();
    }

    /**
     * 만료 임박한 토큰 조회 (자동 갱신용)
     */
    public List<GoogleToken> findExpiringSoonTokens() {
        Instant threshold = Instant.now().plusSeconds(600); // 10분 후
        return googleTokenRepository.findExpiringSoon(threshold);
    }

    /**
     * 만료된 토큰 조회
     */
    public List<GoogleToken> findExpiredTokens() {
        return googleTokenRepository.findByExpiresAtBefore(Instant.now());
    }

    /**
     * 토큰 무효화
     */
    @Transactional
    public void invalidateToken(Long memberId) {
        Optional<GoogleToken> tokenOpt = googleTokenRepository.findById(memberId);

        if (tokenOpt.isPresent()) {
            GoogleToken token = tokenOpt.get();
            token.invalidate();
            googleTokenRepository.save(token);
            log.info("구글 토큰 무효화 완료 - 사용자 ID: {}", memberId);
        }
    }

    /**
     * 토큰 삭제
     */
    @Transactional
    public void deleteByMemberId(Long memberId) {
        googleTokenRepository.deleteById(memberId);
        log.info("구글 토큰 삭제 완료 - 사용자 ID: {}", memberId);
    }

    /**
     * 만료된 토큰 일괄 삭제
     */
    @Transactional
    public int deleteExpiredTokens() {
        List<GoogleToken> expiredTokens = findExpiredTokens();

        int count = 0;
        for (GoogleToken token : expiredTokens) {
            if (!token.hasValidRefreshToken()) {
                googleTokenRepository.delete(token);
                count++;
            }
        }

        if (count > 0) {
            log.info("만료된 구글 토큰 {}개 삭제 완료", count);
        }

        return count;
    }
}