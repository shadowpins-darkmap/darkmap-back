package com.sp.member.controller;

import com.sp.member.model.vo.MemberInfoResponse;
import com.sp.member.persistent.entity.Member;
import com.sp.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "회원", description = "회원 정보 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/member")
public class MemberController {

    private final MemberService memberService;

    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰이 없거나 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "인증이 필요합니다.",
                    "code", "UNAUTHORIZED"
            ));
        }

        Member member = memberService.findById(memberId);

        if (member == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "사용자를 찾을 수 없습니다.",
                    "code", "USER_NOT_FOUND"
            ));
        }

        MemberInfoResponse memberResponse = MemberInfoResponse.from(member);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", memberResponse,
                "message", "프로필 조회 성공"
        ));
    }

    @Operation(
            summary = "내 정보 요약",
            description = "현재 로그인한 사용자의 기본 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }

        Member member = memberService.findById(memberId);

        if (member == null) {
            return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(Map.of(
                "id", member.getId(),
                "email", member.getEmail(),
                "nickname", member.getNickname(),
                "level", member.getLevel(),
                "loginCount", member.getLoginCount(),
                "joinedAt", member.getJoinedAt()
        ));
    }

    @Operation(
            summary = "로그인 상태 확인",
            description = "현재 토큰의 유효성을 확인합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        boolean isLoggedIn = memberId != null;

        return ResponseEntity.ok(Map.of(
                "isLoggedIn", isLoggedIn,
                "memberId", memberId,
                "timestamp", System.currentTimeMillis()
        ));
    }
}