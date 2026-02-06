package com.sp.auth.repository;

import com.sp.auth.entity.GoogleToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GoogleTokenRepository extends JpaRepository<GoogleToken, Long> {

    /**
     * 만료 임박 토큰 조회
     */
    @Query("SELECT g FROM GoogleToken g WHERE g.expiresAt < :threshold")
    List<GoogleToken> findExpiringSoon(@Param("threshold") Instant threshold);

    /**
     * 만료된 토큰 조회
     */
    List<GoogleToken> findByExpiresAtBefore(Instant now);

    /**
     * Refresh Token이 유효한 토큰 조회
     */
    @Query("SELECT g FROM GoogleToken g WHERE g.refreshToken IS NOT NULL " +
            "AND (g.refreshTokenExpiresAt IS NULL OR g.refreshTokenExpiresAt > :now)")
    List<GoogleToken> findWithValidRefreshToken(@Param("now") Instant now);
}