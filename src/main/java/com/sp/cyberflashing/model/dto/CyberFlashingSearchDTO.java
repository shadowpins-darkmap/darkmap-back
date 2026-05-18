package com.sp.cyberflashing.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "사이버 플래싱 사례 조회 필터 (모든 필드 선택, 빈 값은 미적용)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CyberFlashingSearchDTO {

    @Schema(description = "국가 코드 (ISO alpha-2). 대소문자 무관 — 내부에서 trim 후 대문자로 정규화", example = "GB")
    private String countryCode;

    @Schema(description = "중복 여부 필터 (완전 일치)", example = "")
    private String duplicateFlag;

    @Schema(description = "포함 여부 필터 (완전 일치)", example = "포함")
    private String includeFlag;

    public void normalize() {
        if (countryCode != null) {
            countryCode = countryCode.trim().toUpperCase();
            if (countryCode.isEmpty()) {
                countryCode = null;
            }
        }

        if (duplicateFlag != null) {
            duplicateFlag = duplicateFlag.trim();
            if (duplicateFlag.isEmpty()) {
                duplicateFlag = null;
            }
        }

        if (includeFlag != null) {
            includeFlag = includeFlag.trim();
            if (includeFlag.isEmpty()) {
                includeFlag = null;
            }
        }
    }
}
