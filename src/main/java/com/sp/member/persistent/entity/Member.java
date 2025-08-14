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

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isDeleted = false;

    public void increaseLoginVisitCount() {
        this.loginCount++;
        this.visitCount++;
    }
}
