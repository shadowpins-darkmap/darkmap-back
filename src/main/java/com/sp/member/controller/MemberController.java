package com.sp.member.controller;

import com.sp.member.exception.NicknameChangeException;
import com.sp.member.model.vo.MemberInfoResponse;
import com.sp.member.model.vo.UpdateNicknameRequest;
import com.sp.member.model.vo.UpdateNicknameResponse;
import com.sp.member.persistent.entity.Member;
import com.sp.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @Operation(
            summary = "닉네임 수정 ",
            description = "현재 로그인한 사용자의 닉네임을 수정합니다. 30일마다 1회, 총 3회까지 변경 가능합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "닉네임 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 입력값 (금지어, 길이 등)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "success": false,
                                  "error": "부적절한 단어가 포함된 닉네임입니다.",
                                  "code": "INVALID_NICKNAME"
                                }
                                """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "success": false,
                                  "error": "인증이 필요합니다.",
                                  "code": "UNAUTHORIZED"
                                }
                                """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "중복된 닉네임",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "success": false,
                                  "error": "이미 사용 중인 닉네임입니다.",
                                  "code": "NICKNAME_DUPLICATE"
                                }
                                """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "변경 제한 (횟수 초과 또는 기간 제한)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "success": false,
                                  "error": "닉네임 변경 횟수를 모두 사용했습니다. (최대 3회)",
                                  "code": "NICKNAME_MAX_COUNT_REACHED"
                                }
                                """
                            )
                    )
            )
    })
    @PutMapping("/nickname")
    public ResponseEntity<UpdateNicknameResponse> updateNickname(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid UpdateNicknameRequest request) {

        if (memberId == null) {
            return ResponseEntity.status(401).body(
                    UpdateNicknameResponse.builder()
                            .success(false)
                            .message("인증이 필요합니다.")
                            .build()
            );
        }

        String newNickname = request.getNickname();
        if (newNickname == null || newNickname.trim().isEmpty()) {
            return ResponseEntity.status(400).body(
                    UpdateNicknameResponse.builder()
                            .success(false)
                            .message("닉네임을 입력해주세요.")
                            .build()
            );
        }

        try {
            Member updatedMember = memberService.updateNickname(memberId, newNickname.trim());

            UpdateNicknameResponse.MemberData memberData = UpdateNicknameResponse.MemberData.builder()
                    .id(updatedMember.getId())
                    .nickname(updatedMember.getNickname())
                    .changeCount(updatedMember.getNicknameChangeCount())
                    .lastChangeAt(updatedMember.getLastNicknameChangeAt())
                    .build();

            return ResponseEntity.ok(
                    UpdateNicknameResponse.builder()
                            .success(true)
                            .data(memberData)
                            .message("닉네임이 성공적으로 변경되었습니다.")
                            .build()
            );

        } catch (NicknameChangeException e) {
            return ResponseEntity.status(429).body(
                    UpdateNicknameResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(
                    UpdateNicknameResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(
                    UpdateNicknameResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }
}