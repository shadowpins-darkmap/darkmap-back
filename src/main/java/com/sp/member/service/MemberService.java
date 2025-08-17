package com.sp.member.service;

import com.sp.member.exception.NicknameChangeException;
import com.sp.member.model.vo.MemberInfoResponse;
import com.sp.member.model.vo.NicknameChangeInfo;
import com.sp.member.persistent.entity.Member;
import com.sp.member.model.type.AuthType;
import com.sp.member.persistent.repository.MemberRepository;
import com.sp.member.util.BadWordFilter;
import com.sp.member.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final BadWordFilter badWordFilter;

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

    @Transactional
    public void withdraw(Long id) {
        log.info("회원 탈퇴 처리 시작 - 사용자 ID: {}", id);

        Optional<Member> memberOpt = memberRepository.findById(id);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();

            if (member.getIsDeleted()) {
                log.warn("이미 탈퇴한 회원 - 사용자 ID: {}", id);
                throw new IllegalStateException("이미 탈퇴한 회원입니다.");
            }

            // 탈퇴 처리
            member.setIsDeleted(true);
            memberRepository.save(member);

            log.info("회원 탈퇴 처리 완료 - 사용자 ID: {}, 이메일: {}", id, member.getEmail());
        } else {
            log.error("탈퇴 처리 실패: 존재하지 않는 회원 - 사용자 ID: {}", id);
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
    }

    /**
     * 마케팅 동의 상태 토글
     */
    public MemberInfoResponse toggleMarketingAgreementById(Long id) {
        Member member = findMemberByPrimaryKey(id);

        boolean oldStatus = member.getMarketingAgreed();
        boolean newStatus = !oldStatus;
        member.updateMarketingAgreement(newStatus);

        log.info("Marketing agreement toggled - id: {}, memberId: {}, {} -> {}",
                id, member.getMemberId(), oldStatus, newStatus);

        return MemberInfoResponse.from(member);
    }
    private Member findMemberByPrimaryKey(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + id));
    }

    public Member updateNickname(Long memberId, String newNickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!member.canChangeNickname()) {
            if (member.getNicknameChangeCount() >= 3) {
                throw NicknameChangeException.reachedMaxCount();
            } else {
                throw NicknameChangeException.tooSoon(member.getNextChangeAvailableAt());
            }
        }

        if (newNickname.length() < 2 || newNickname.length() > 25) {
            throw new IllegalArgumentException("닉네임은 2자 이상 25자 이하로 입력해주세요.");
        }

        if (badWordFilter.containsBadWord(newNickname)) {
            throw new IllegalArgumentException("부적절한 단어가 포함된 닉네임입니다.");
        }

        if (memberRepository.existsByNicknameAndIdNot(newNickname, memberId)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        member.updateNickname(newNickname);
        return memberRepository.save(member);
    }


    public long getTotalMemberCount() {
        return memberRepository.count(); // 탈퇴한 회원도 카운트 수에 포함
    }

    @Transactional(readOnly = true)
    public NicknameChangeInfo getNicknameChangeInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return NicknameChangeInfo.builder()
                .canChange(member.canChangeNickname())
                .changeCount(member.getNicknameChangeCount())
                .maxChangeCount(3)
                .lastChangeAt(member.getLastNicknameChangeAt())
                .nextAvailableAt(member.getNextChangeAvailableAt())
                .build();
    }
}
