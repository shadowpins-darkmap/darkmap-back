package com.sp.member.persistent.repository;

import com.sp.member.persistent.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    long count();
    Optional<Member> findByEmail(String email);
}