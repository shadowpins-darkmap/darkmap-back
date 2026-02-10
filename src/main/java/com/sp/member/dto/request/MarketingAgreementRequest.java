package com.sp.member.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketingAgreementRequest {

    @NotNull(message = "동의 여부는 필수입니다.")
    private Boolean agreed;
}
