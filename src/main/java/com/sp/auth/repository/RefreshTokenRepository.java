package com.sp.auth.repository;

import com.sp.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰으로 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 만료된 Refresh Token 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);

    /**
     * 만료 임박 Refresh Token 조회
     */
    List<RefreshToken> findByExpiresAtBetween(Instant start, Instant end);

    /**
     * 토큰으로 삭제
     */
    void deleteByToken(String token);
}