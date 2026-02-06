package com.sp.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {
    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}
