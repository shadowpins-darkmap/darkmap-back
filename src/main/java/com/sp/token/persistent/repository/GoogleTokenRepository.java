package com.sp.token.persistent.repository;

import com.sp.token.persistent.entity.GoogleToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleTokenRepository extends JpaRepository<GoogleToken, Long> {
    Optional<GoogleToken> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}