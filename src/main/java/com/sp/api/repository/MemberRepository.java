package com.sp.api.repository;

import com.sp.api.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 조회
     */
    Optional<Member> findByEmail(String email);

    /**
     * 최대 userNumber 조회
     */
    @Query("SELECT MAX(m.userNumber) FROM Member m")
    Integer findMaxUserNumber();

    /**
     * 닉네임 중복 체크 (자신 제외)
     */
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    /**
     * 탈퇴 여부로 카운트
     */
    long countByIsDeleted(Boolean isDeleted);

    /**
     * 회원 ID로 닉네임 조회 (커뮤니티용)
     */
    @Query("SELECT m.nickname FROM Member m WHERE m.id = :memberId")
    Optional<String> findNicknameByMemberId(Long memberId);

    /**
     * 회원 ID로 탈퇴 여부 조회 (커뮤니티용)
     */
    @Query("SELECT m.isDeleted FROM Member m WHERE m.id = :memberId")
    Optional<Boolean> findIsDeletedByMemberId(Long memberId);
}