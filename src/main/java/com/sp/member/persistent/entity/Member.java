package com.sp.member.persistent.entity;

import com.sp.member.model.type.AuthType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberId;

    @Enumerated(EnumType.STRING)
    private AuthType type;

    private String email;

    private String nickname;

    private String level;

    @Column(nullable = false)
    private int loginCount;

    @Column(nullable = false)
    private int visitCount;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isDeleted;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int nicknameChangeCount = 0;

    private LocalDateTime lastNicknameChangeAt;

    public static MemberBuilder builder() {
        return new MemberBuilder()
                .isDeleted(false)
                .nicknameChangeCount(0);
    }

    public void increaseLoginVisitCount() {
        this.loginCount++;
        this.visitCount++;
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
        this.nicknameChangeCount++;
        this.lastNicknameChangeAt = LocalDateTime.now();
    }

    public boolean canChangeNickname() {
        if (this.nicknameChangeCount >= 3) {
            return false;
        }

        if (this.lastNicknameChangeAt != null) {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            return this.lastNicknameChangeAt.isBefore(thirtyDaysAgo);
        }

        return true; // 처음 변경하는 경우
    }

    public LocalDateTime getNextChangeAvailableAt() {
        if (this.lastNicknameChangeAt == null) {
            return null; // 처음 변경하는 경우
        }
        return this.lastNicknameChangeAt.plusDays(30);
    }
}