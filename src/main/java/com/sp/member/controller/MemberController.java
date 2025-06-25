package com.sp.member.controller;

import com.sp.member.model.vo.MemberInfoResponse;
import com.sp.member.persistent.entity.Member;
import com.sp.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberInfoResponse> getUserInfo(@AuthenticationPrincipal Long id) {
        Member member = memberService.findById(id);
        return ResponseEntity.ok(MemberInfoResponse.from(member));
    }
}
