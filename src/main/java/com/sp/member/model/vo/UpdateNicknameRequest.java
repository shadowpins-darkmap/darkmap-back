package com.sp.member.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@Schema(description = "닉네임 변경 요청")
public class UpdateNicknameRequest {

    @Schema(description = "새로운 닉네임", example = "새로운닉네임", required = true)
    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 1, max = 25, message = "닉네임은 1자 이상 25자 이하로 입력해주세요.")
    private String nickname;
}