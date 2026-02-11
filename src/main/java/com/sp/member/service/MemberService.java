package com.sp.member.service;

import com.sp.exception.NicknameChangeException;
import com.sp.member.dto.response.MemberInfoResponse;
import com.sp.member.dto.response.NicknameChangeInfo;
import com.sp.member.entity.Member;
import com.sp.auth.enums.AuthType;
import com.sp.member.repository.MemberRepository;
import com.sp.member.util.NicknameGenerator;
import com.sp.exception.WithdrawnMemberException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final BadWordFilter badWordFilter;

    @Value("${auth.rejoin-hold-days:7}")
    private int rejoinHoldDays;

    /**
     * 회원 저장 (없으면 생성, 있으면 로그인 카운트 증가)
     */
    @Transactional
    public Member saveIfNotExists(String email, String userId, AuthType type) {
        long startTime = System.currentTimeMillis();
        log.info("💾 회원 저장 시작 - email: {}, type: {}", email, type);

        // 1. 기존 회원 조회
        Optional<Member> emailExists = memberRepository.findByEmail(email);

        if (emailExists.isPresent()) {
            Member existing = emailExists.get();

            // 탈퇴 회원 재가입 처리
            if (existing.getIsDeleted()) {
                Duration hold = Duration.ofDays(rejoinHoldDays);
                if (existing.isRejoinBlocked(hold)) {
                    Instant availableAt = existing.getRejoinAvailableAt(hold);
                    String message = availableAt != null
                            ? String.format("탈퇴한 회원은 %s 까지 재가입이 불가능합니다.", availableAt)
                            : "탈퇴한 회원은 재가입이 불가능합니다.";
                    throw new WithdrawnMemberException(message);
                }

                // 소셜 유형 또는 provider ID가 다르면 중복 이메일 정책 유지
                if (existing.getType() != type || !existing.getMemberId().equals(userId)) {
                    log.error("❌ 탈퇴 회원 이메일 중복 - 기존: {}, 시도: {}", existing.getType(), type);
                    throw new IllegalStateException("이미 다른 소셜 로그인으로 가입된 이메일입니다.");
                }

                // 유보기간 경과: 계정 복구 후 로그인 처리
                existing.restore();
                existing.increaseLoginVisitCount();
                Member restored = memberRepository.save(existing);
                log.info("✅ 탈퇴 회원 복구 및 로그인 - ID: {}, 소요시간: {}ms",
                        restored.getId(), System.currentTimeMillis() - startTime);
                return restored;
            }

            // 동일한 소셜 로그인 계정인 경우
            if (existing.getType() == type && existing.getMemberId().equals(userId)) {
                existing.increaseLoginVisitCount();
                Member saved = memberRepository.save(existing);

                log.info("✅ 기존 회원 로그인 완료 - ID: {}, 소요시간: {}ms",
                        saved.getId(), System.currentTimeMillis() - startTime);
                return saved;
            }

            // 이메일은 같은데 다른 소셜 로그인인 경우
            log.error("❌ 중복 이메일 - 기존: {}, 시도: {}", existing.getType(), type);
            throw new IllegalStateException("이미 다른 소셜 로그인으로 가입된 이메일입니다.");
        }

        // 2. 신규 회원 가입 - userNumber 조회
        Integer lastUserNumber = memberRepository.findMaxUserNumber();
        int userNumber = (lastUserNumber != null ? lastUserNumber : 0) + 1;
        String nickname = NicknameGenerator.generateNickname(userNumber);

        // 3. 회원 저장
        Member newMember = memberRepository.save(Member.builder()
                .email(email)
                .memberId(userId)
                .userNumber(userNumber)
                .nickname(nickname)
                .type(type)
                .level("BASIC")
                .loginCount(1)
                .visitCount(1)
                .isDeleted(false)
                .build());

        log.info("✅ 신규 회원 가입 완료 - ID: {}, userNumber: {}, 소요시간: {}ms",
                newMember.getId(), userNumber, System.currentTimeMillis() - startTime);

        return newMember;
    }

    /**
     * ID로 회원 조회
     */
    public Member findById(Long id) {
        return memberRepository.findById(id).orElse(null);
    }

    /**
     * ID로 회원 조회 (예외 발생)
     */
    public Member findByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + id));
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     */
    @Transactional
    public void withdraw(Long id) {
        log.info("🗑️ 회원 탈퇴 처리 시작 - 사용자 ID: {}", id);

        Member member = findByIdOrThrow(id);

        if (member.getIsDeleted()) {
            log.warn("⚠️ 이미 탈퇴한 회원 - 사용자 ID: {}", id);
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        // 탈퇴 처리 (소프트 삭제)
        member.softDelete();
        memberRepository.save(member);

        log.info("✅ 회원 탈퇴 완료 - 사용자 ID: {}, 이메일: {}", id, member.getEmail());
    }

    /**
     * 회원 복구
     */
    @Transactional
    public void restore(Long id) {
        log.info("🔄 회원 복구 시작 - 사용자 ID: {}", id);

        Member member = findByIdOrThrow(id);

        if (!member.getIsDeleted()) {
            log.warn("⚠️ 이미 활성 상태인 회원 - 사용자 ID: {}", id);
            throw new IllegalStateException("이미 활성 상태인 회원입니다.");
        }

        member.restore();
        memberRepository.save(member);

        log.info("✅ 회원 복구 완료 - 사용자 ID: {}", id);
    }

    /**
     * 마케팅 동의 상태 설정
     */
    @Transactional
    public MemberInfoResponse updateMarketingAgreementById(Long id, boolean agreed) {
        Member member = findByIdOrThrow(id);

        boolean oldStatus = member.getMarketingAgreed();
        if (oldStatus != agreed) {
            member.updateMarketingAgreement(agreed);
            log.info("🔧 마케팅 동의 상태 변경 - ID: {}, {} -> {}", id, oldStatus, agreed);
        } else {
            log.info("ℹ️ 마케팅 동의 상태 변경 없음 - ID: {}, 현재 상태 유지 ({})", id, agreed);
        }

        return MemberInfoResponse.from(member);
    }

    /**
     * 닉네임 변경
     */
    @Transactional
    public Member updateNickname(Long memberId, String newNickname) {
        Member member = findByIdOrThrow(memberId);

        // 닉네임 변경 가능 여부 확인
        if (!member.canChangeNickname()) {
            if (member.getNicknameChangeCount() >= 3) {
                throw NicknameChangeException.reachedMaxCount();
            } else {
                throw NicknameChangeException.tooSoon(member.getNextChangeAvailableAt());
            }
        }

        // 닉네임 유효성 검증
        validateNickname(newNickname, memberId);

        // 닉네임 업데이트
        member.updateNickname(newNickname);
        return memberRepository.save(member);
    }

    /**
     * 닉네임 유효성 검증
     */
    private void validateNickname(String nickname, Long memberId) {
        // 길이 체크
        if (nickname.length() < 2 || nickname.length() > 25) {
            throw new IllegalArgumentException("닉네임은 2자 이상 25자 이하로 입력해주세요.");
        }

        // 욕설 필터링
        if (badWordFilter.containsBadWord(nickname)) {
            throw new IllegalArgumentException("부적절한 단어가 포함된 닉네임입니다.");
        }

        // 중복 체크
        if (memberRepository.existsByNicknameAndIdNot(nickname, memberId)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }
    }

    /**
     * 닉네임 변경 정보 조회
     */
    @Transactional(readOnly = true)
    public NicknameChangeInfo getNicknameChangeInfo(Long memberId) {
        Member member = findByIdOrThrow(memberId);

        return NicknameChangeInfo.builder()
                .canChange(member.canChangeNickname())
                .changeCount(member.getNicknameChangeCount())
                .maxChangeCount(3)
                .lastChangeAt(member.getLastNicknameChangeAt())
                .nextAvailableAt(member.getNextChangeAvailableAt())
                .build();
    }

    /**
     * 전체 회원 수 조회
     */
    public long getTotalMemberCount() {
        return memberRepository.count();
    }

    /**
     * 활성 회원 수 조회
     */
    public long getActiveMemberCount() {
        return memberRepository.countByIsDeleted(false);
    }

    /**
     * 탈퇴 회원 수 조회
     */
    public long getWithdrawnMemberCount() {
        return memberRepository.countByIsDeleted(true);
    }
}
