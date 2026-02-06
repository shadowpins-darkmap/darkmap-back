package com.sp.member.dto.response;

import com.sp.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MemberInfoResponse {

    private String email;
    private String nickname;
    private String memberId;
    private String level;
    private String type;
    private Integer loginCount;
    private Integer visitCount;
    private Instant joinedAt;
    private Instant updatedAt;
    private Boolean marketingAgreed;

    public static MemberInfoResponse from(Member member) {
        return MemberInfoResponse.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .memberId(member.getId().toString())
                .type(member.getType().toString())
                .level(member.getLevel())
                .loginCount(member.getLoginCount())
                .visitCount(member.getVisitCount())
                .joinedAt(member.getJoinedAt())
                .updatedAt(member.getUpdatedAt())
                .marketingAgreed(member.getMarketingAgreed())
                .build();
    }
}