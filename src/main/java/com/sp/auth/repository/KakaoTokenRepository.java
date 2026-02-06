package com.sp.auth.repository;

import com.sp.auth.entity.KakaoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface KakaoTokenRepository extends JpaRepository<KakaoToken, Long> {

    /**
     * 만료 임박 토큰 조회
     */
    @Query("SELECT k FROM KakaoToken k WHERE k.expiresAt < :threshold")
    List<KakaoToken> findExpiringSoon(@Param("threshold") Instant threshold);

    /**
     * 만료된 토큰 조회
     */
    List<KakaoToken> findByExpiresAtBefore(Instant now);

    /**
     * Refresh Token이 유효한 토큰 조회
     */
    @Query("SELECT k FROM KakaoToken k WHERE k.refreshToken IS NOT NULL " +
            "AND (k.refreshTokenExpiresAt IS NULL OR k.refreshTokenExpiresAt > :now)")
    List<KakaoToken> findWithValidRefreshToken(@Param("now") Instant now);
}