package com.sp.api.service;

import com.sp.api.entity.KakaoToken;
import com.sp.api.entity.Member;
import com.sp.api.repository.KakaoTokenRepository;
import com.sp.api.repository.MemberRepository;
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
public class KakaoTokenService {

    private final KakaoTokenRepository kakaoTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * 토큰 저장/업데이트 (Upsert)
     */
    @Transactional
    public void saveTokens(Long memberId, String accessToken, String refreshToken, Instant expiresAt) {
        Optional<KakaoToken> existingToken = kakaoTokenRepository.findById(memberId);

        if (existingToken.isPresent()) {
            KakaoToken token = existingToken.get();
            token.updateTokens(accessToken, refreshToken, expiresAt);
            kakaoTokenRepository.save(token);
            log.info("카카오 토큰 업데이트 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        } else {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + memberId));

            KakaoToken kakaoToken = KakaoToken.builder()
                    //.memberId(memberId)
                    .member(member)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .build();

            kakaoTokenRepository.save(kakaoToken);
            log.info("카카오 토큰 저장 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        }
    }

    /**
     * Access Token만 갱신
     */
    @Transactional
    public void updateAccessToken(Long memberId, String accessToken, Instant expiresAt) {
        KakaoToken token = kakaoTokenRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 카카오 토큰이 없습니다: " + memberId));

        token.updateAccessToken(accessToken, expiresAt);
        kakaoTokenRepository.save(token);
        log.info("카카오 액세스 토큰 갱신 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
    }

    /**
     * memberId로 토큰 조회
     */
    public Optional<KakaoToken> findByMemberId(Long memberId) {
        return kakaoTokenRepository.findById(memberId);
    }

    /**
     * 유효한 토큰만 반환 (만료되지 않은 토큰)
     */
    public Optional<KakaoToken> findValidTokenByMemberId(Long memberId) {
        Optional<KakaoToken> tokenOpt = kakaoTokenRepository.findById(memberId);

        if (tokenOpt.isPresent()) {
            KakaoToken token = tokenOpt.get();

            // Access Token이 만료되었지만 Refresh Token이 있으면 반환
            if (token.isExpired()) {
                if (token.hasValidRefreshToken()) {
                    log.info("카카오 액세스 토큰 만료됨, 리프레시 토큰 사용 가능 - 사용자 ID: {}", memberId);
                    return tokenOpt;
                } else {
                    log.warn("카카오 토큰 완전 만료 - 사용자 ID: {}", memberId);
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
    public List<KakaoToken> findExpiringSoonTokens() {
        Instant threshold = Instant.now().plusSeconds(600); // 10분 후
        return kakaoTokenRepository.findExpiringSoon(threshold);
    }

    /**
     * 만료된 토큰 조회
     */
    public List<KakaoToken> findExpiredTokens() {
        return kakaoTokenRepository.findByExpiresAtBefore(Instant.now());
    }

    /**
     * 토큰 무효화
     */
    @Transactional
    public void invalidateToken(Long memberId) {
        Optional<KakaoToken> tokenOpt = kakaoTokenRepository.findById(memberId);

        if (tokenOpt.isPresent()) {
            KakaoToken token = tokenOpt.get();
            token.invalidate();
            kakaoTokenRepository.save(token);
            log.info("카카오 토큰 무효화 완료 - 사용자 ID: {}", memberId);
        }
    }

    /**
     * 토큰 삭제
     */
    @Transactional
    public void deleteByMemberId(Long memberId) {
        kakaoTokenRepository.deleteById(memberId);
        log.info("카카오 토큰 삭제 완료 - 사용자 ID: {}", memberId);
    }

    /**
     * 만료된 토큰 일괄 삭제
     */
    @Transactional
    public int deleteExpiredTokens() {
        List<KakaoToken> expiredTokens = findExpiredTokens();

        int count = 0;
        for (KakaoToken token : expiredTokens) {
            if (!token.hasValidRefreshToken()) {
                kakaoTokenRepository.delete(token);
                count++;
            }
        }

        if (count > 0) {
            log.info("만료된 카카오 토큰 {}개 삭제 완료", count);
        }

        return count;
    }
}