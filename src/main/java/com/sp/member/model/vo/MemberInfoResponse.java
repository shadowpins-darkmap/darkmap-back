package com.sp.member.model.vo;

import com.sp.member.persistent.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberInfoResponse {

    private String email;
    private String nickname;
    private String memberId; // KAKAO_12345, GOOGLE_12345
    private String level;
    private String type;
    private int loginCount;
    private int visitCount;
    private LocalDateTime joinedAt;

    public static MemberInfoResponse from(Member member) {
        return MemberInfoResponse.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .memberId(member.getMemberId())
                .type(member.getType().toString())
                .level(member.getLevel())
                .loginCount(member.getLoginCount())
                .visitCount(member.getVisitCount())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
