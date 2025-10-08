package com.sp.member.persistent.repository;

import com.sp.member.persistent.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    long count();
    Optional<Member> findByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    @Query("SELECT m.nickname FROM Member m WHERE m.id = :memberId AND m.isDeleted = false")
    Optional<String> findNicknameByMemberId(@Param("memberId") Long memberId);
}