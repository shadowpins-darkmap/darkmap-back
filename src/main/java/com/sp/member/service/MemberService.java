package com.sp.member.service;

import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.member.persistent.repository.MemberRepository;
import com.sp.member.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 이메일 필수값으로 들어온다는 전제하에 가입여부 판단
    public Member saveIfNotExists(String email, String userId, AuthType type) {
        Optional<Member> emailExists = memberRepository.findByEmail(email);
        if (emailExists.isPresent()) {
            Member existing = emailExists.get();
            // 로그인 횟수 카운트
            if (existing.getType() == type && existing.getMemberId().equals(userId)) {
                existing.increaseLoginVisitCount();
                return memberRepository.save(existing);
            }
            // 이메일은 같은데 다른 소셜 로그인일 경우
            throw new IllegalStateException("이미 다른 소셜 로그인으로 가입된 이메일입니다.");
        }

        // 가입 처리
        long memberCount = memberRepository.count();
        int userNumber = (int) (memberCount + 1);
        String nickname = NicknameGenerator.generateNickname(userNumber);

        return memberRepository.save(Member.builder()
                .email(email)
                .memberId(userId)
                .nickname(nickname)
                .type(type)
                .level("BASIC")
                .loginCount(1)
                .visitCount(1)
                .isDeleted(Boolean.FALSE)
                .build());
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElse(null);
    }

    public void withdraw(Long id) {
        memberRepository.findById(id).ifPresent(member -> {
            member.setIsDeleted(true);
            memberRepository.save(member);
        });
    }
}