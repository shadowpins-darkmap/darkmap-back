package com.sp.auth.repository;

import com.sp.auth.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {

    /**
     * 만료된 블랙리스트 조회
     */
    List<TokenBlacklist> findByExpiresAtBefore(Instant now);

    /**
     * 만료되지 않은 블랙리스트 조회
     */
    List<TokenBlacklist> findByExpiresAtAfter(Instant now);

    /**
     * 특정 회원의 블랙리스트 조회
     */
    List<TokenBlacklist> findByMemberId(Long memberId);
}