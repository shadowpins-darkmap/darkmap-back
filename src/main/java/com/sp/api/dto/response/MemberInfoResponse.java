package com.sp.api.dto.response;

import com.sp.api.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberInfoResponse {

    private String email;
    private String nickname;
    private String memberId;
    private String level;
    private String type;
    private int loginCount;
    private int visitCount;
    private LocalDateTime joinedAt;
    private Boolean marketingAgreed;

    public static MemberInfoResponse from(Member member) {
        return MemberInfoResponse.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .memberId(member.getId().toString()) // seq id
                .type(member.getType().toString())
                .level(member.getLevel())
                .loginCount(member.getLoginCount())
                .visitCount(member.getVisitCount())
                .joinedAt(member.getJoinedAt())
                .marketingAgreed(member.getMarketingAgreed())
                .build();
    }
}
