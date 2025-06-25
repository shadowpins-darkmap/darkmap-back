package com.sp.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member API", description = "회원 정보 관련 API")
@RequestMapping("/api/v1/members")
public interface MemberApiSpec {
   /* @Operation(
            summary = "닉네임 수정",
            description = "회원의 닉네임을 변경합니다",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = NicknameUpdateRequestDto.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "닉네임 수정 성공"),
                    @ApiResponse(responseCode = "400", description = "닉네임 수정 실패")
            }
    )
    @PatchMapping("/nickname")
    ResponseEntity<String> updateNickname(@org.springframework.web.bind.annotation.RequestBody NicknameUpdateRequestDto requestDto);*/
}
