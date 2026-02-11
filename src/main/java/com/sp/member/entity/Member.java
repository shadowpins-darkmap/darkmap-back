package com.sp.member.entity;

import com.sp.auth.enums.AuthType;
import com.sp.auth.entity.GoogleToken;
import com.sp.auth.entity.KakaoToken;
import com.sp.auth.entity.RefreshToken;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "member", indexes = {
        @Index(name = "idx_member_email", columnList = "email"),
        @Index(name = "idx_member_member_id", columnList = "member_id"),
        @Index(name = "idx_member_user_number", columnList = "user_number"),
        @Index(name = "idx_member_auth_type", columnList = "type")
})
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true, length = 100)
    private String memberId;

    @Column(name = "user_number", unique = true)
    private Integer userNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthType type;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 50)
    private String nickname;

    @Column(length = 20)
    private String level;

    @Column(nullable = false)
    private Integer loginCount;

    @Column(nullable = false)
    private Integer visitCount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) default 0")
    private Boolean isDeleted;

    // 탈퇴 시각(재가입 유보기간 계산용)
    private Instant withdrawnAt;

    // 마지막 탈퇴 시각(재가입 후에도 과거 작성물 익명/권한 차단 기준)
    private Instant lastWithdrawnAt;

    @Column(nullable = false, columnDefinition = "INT default 0")
    private Integer nicknameChangeCount;

    private Instant lastNicknameChangeAt;

    // 사이트 이용약관 동의 여부
    @Column(nullable = false, columnDefinition = "TINYINT(1) default 1")
    private Boolean termsOfServiceAgreed;

    // 개인정보 처리방침 동의 여부
    @Column(nullable = false, columnDefinition = "TINYINT(1) default 1")
    private Boolean privacyPolicyAgreed;

    // 마케팅 광고 수신 동의 여부
    @Column(nullable = false, columnDefinition = "TINYINT(1) default 0")
    private Boolean marketingAgreed;

    // 양방향 관계 설정 (선택사항)
    @OneToOne(mappedBy = "member", orphanRemoval = true, fetch = FetchType.LAZY)
    private GoogleToken googleToken;

    @OneToOne(mappedBy = "member", orphanRemoval = true, fetch = FetchType.LAZY)
    private KakaoToken kakaoToken;

    @OneToOne(mappedBy = "member", orphanRemoval = true, fetch = FetchType.LAZY)
    private RefreshToken refreshToken;

    // 낙관적 락을 위한 버전 관리
    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long version = 0L;

    public static MemberBuilder builder() {
        return new MemberBuilder()
                .isDeleted(false)
                .loginCount(0)
                .visitCount(0)
                .nicknameChangeCount(0)
                .termsOfServiceAgreed(true)
                .privacyPolicyAgreed(true)
                .marketingAgreed(false)
                .version(0L);
    }

    /**
     * 로그인 및 방문 카운트 증가 (동시성 고려)
     */
    public void increaseLoginVisitCount() {
        this.loginCount++;
        this.visitCount++;
    }

    /**
     * 닉네임 업데이트
     */
    public void updateNickname(String newNickname) {
        if (!canChangeNickname()) {
            throw new IllegalStateException("닉네임 변경 조건을 만족하지 않습니다.");
        }
        this.nickname = newNickname;
        this.nicknameChangeCount++;
        this.lastNicknameChangeAt = Instant.now();
    }

    /**
     * 닉네임 변경 가능 여부 확인
     * - 최대 3회까지만 변경 가능
     * - 마지막 변경 후 30일이 지나야 다시 변경 가능
     */
    public boolean canChangeNickname() {
        if (this.nicknameChangeCount >= 3) {
            return false;
        }

        if (this.lastNicknameChangeAt != null) {
            Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60);
            return this.lastNicknameChangeAt.isBefore(thirtyDaysAgo);
        }

        return true; // 처음 변경하는 경우
    }

    /**
     * 다음 닉네임 변경 가능 시간
     */
    public Instant getNextChangeAvailableAt() {
        if (this.lastNicknameChangeAt == null) {
            return null; // 처음 변경하는 경우
        }
        return this.lastNicknameChangeAt.plusSeconds(30L * 24 * 60 * 60);
    }

    /**
     * 마케팅 동의 상태 변경
     */
    public void updateMarketingAgreement(boolean agreed) {
        this.marketingAgreed = agreed;
    }

    /**
     * 회원 탈퇴 처리 (소프트 삭제)
     */
    public void softDelete() {
        this.isDeleted = true;
        this.withdrawnAt = Instant.now();
        this.lastWithdrawnAt = this.withdrawnAt;
    }

    /**
     * 회원 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.withdrawnAt = null;
        // lastWithdrawnAt은 유지 (과거 작성물 익명화 기준)
    }

    /**
     * 재가입 유보기간이 남았는지 여부
     */
    public boolean isRejoinBlocked(Duration holdDuration) {
        if (Boolean.FALSE.equals(this.isDeleted)) {
            return false;
        }

        if (this.withdrawnAt == null) {
            return true; // 탈퇴 시간 불명확하면 보수적으로 차단
        }

        return this.withdrawnAt.plus(holdDuration).isAfter(Instant.now());
    }

    /**
     * 재가입 가능 시각 계산
     */
    public Instant getRejoinAvailableAt(Duration holdDuration) {
        if (this.withdrawnAt == null) {
            return null;
        }
        return this.withdrawnAt.plus(holdDuration);
    }
}
