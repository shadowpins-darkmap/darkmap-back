package com.sp.auth.service;

import com.sp.member.entity.Member;
import com.sp.auth.entity.RefreshToken;
import com.sp.member.repository.MemberRepository;
import com.sp.auth.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Autowired
    private EntityManager entityManager;

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * Refresh Token 저장 (Upsert)
     */
    @Transactional
    public void save(Long memberId, String token, Instant expiresAt) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByMember_Id(memberId);

        if (existingToken.isPresent()) {
            RefreshToken refreshToken = existingToken.get();
            refreshToken.renewToken(token, expiresAt);
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh Token 갱신 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        } else {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + memberId));
            //Member member = entityManager.getReference(Member.class, memberId);

            RefreshToken refreshToken = RefreshToken.builder()
                    //.memberId(memberId)
                    .member(member)
                    .token(token)
                    .expiresAt(expiresAt)
                    .refreshCount(0)
                    .build();

            refreshTokenRepository.save(refreshToken);
            log.info("Refresh Token 저장 완료 - 사용자 ID: {}, 만료시간: {}", memberId, expiresAt);
        }
    }

    /**
     * 토큰으로 조회 (만료 체크 포함)
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(RefreshToken::isValid);
    }

    /**
     * memberId로 조회
     */
    public Optional<RefreshToken> findByMemberId(Long memberId) {
        return refreshTokenRepository.findByMember_Id(memberId);
    }

    /**
     * 유효한 Refresh Token 조회
     */
    public Optional<RefreshToken> findValidByMemberId(Long memberId) {
        return refreshTokenRepository.findByMember_Id(memberId)
                .filter(RefreshToken::isValid);
    }

    /**
     * 토큰 갱신
     */
    @Transactional
    public void renewToken(Long memberId, String newToken, Instant expiresAt) {
        RefreshToken refreshToken = refreshTokenRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 Refresh Token이 없습니다: " + memberId));

        refreshToken.renewToken(newToken, expiresAt);
        refreshTokenRepository.save(refreshToken);
        log.info("Refresh Token 갱신 완료 - 사용자 ID: {}, 갱신 횟수: {}",
                memberId, refreshToken.getRefreshCount());
    }

    /**
     * 토큰 사용 기록
     */
    @Transactional
    public void recordUsage(Long memberId) {
        RefreshToken refreshToken = refreshTokenRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 Refresh Token이 없습니다: " + memberId));

        refreshToken.recordUsage();
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * memberId로 삭제
     */
    @Transactional
    public void deleteByMemberId(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
        log.info("Refresh Token 삭제 완료 - 사용자 ID: {}", memberId);
    }

    /**
     * 토큰으로 삭제
     */
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
        log.info("Refresh Token 삭제 완료 - 토큰: {}...", token.substring(0, Math.min(20, token.length())));
    }

    /**
     * 토큰 무효화
     */
    @Transactional
    public void invalidateToken(Long memberId) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByMember_Id(memberId);

        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            token.invalidate();
            refreshTokenRepository.save(token);
            log.info("Refresh Token 무효화 완료 - 사용자 ID: {}", memberId);
        }
    }

    /**
     * 만료된 토큰 일괄 삭제
     */
    @Transactional
    public int deleteExpiredTokens() {
        Instant now = Instant.now();
        int count = refreshTokenRepository.deleteByExpiresAtBefore(now);

        if (count > 0) {
            log.info("만료된 Refresh Token {}개 삭제 완료", count);
        }

        return count;
    }

    /**
     * 만료 임박한 토큰 조회 (1일 이내)
     */
    public long countExpiringSoon() {
        Instant oneDayLater = Instant.now().plusSeconds(24 * 60 * 60);
        return refreshTokenRepository.findByExpiresAtBetween(Instant.now(), oneDayLater).size();
    }
}
