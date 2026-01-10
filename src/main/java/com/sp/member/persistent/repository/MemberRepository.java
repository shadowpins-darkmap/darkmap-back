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
    @Query("SELECT m.isDeleted FROM Member m WHERE m.id = :memberId")
    Optional<Boolean> findIsDeletedByMemberId(@Param("memberId") Long memberId);
    @Query("SELECT MAX(m.userNumber) FROM Member m")
    Integer findMaxUserNumber();
}
