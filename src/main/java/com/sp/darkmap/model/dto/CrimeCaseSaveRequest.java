package com.sp.darkmap.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sp.darkmap.code.InfoType;
import com.sp.darkmap.code.MapCrimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "범죄 사례 등록 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CrimeCaseSaveRequest {

    @Schema(description = "정보유형", example = "회원의 경험담",
            allowableValues = {"인터넷 뉴스기사", "회원의 경험담"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "정보유형은 필수입니다.")
    private InfoType infoType;

    @Schema(description = "범죄유형", example = "바바리맨",
            allowableValues = {"바바리맨", "헌팅", "미행"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "범죄유형은 필수입니다.")
    private MapCrimeType crimeType;

    @Schema(description = "시도", example = "서울특별시", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "시도는 필수입니다.")
    private String sido;

    @Schema(description = "시군구", example = "강남구", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "시군구는 필수입니다.")
    private String sigungu;

    @Schema(description = "뉴스기사 URL (정보유형이 '인터넷 뉴스기사'면 필수)",
            example = "https://news.example.com/article/123", nullable = true)
    private String newsUrl;

    @Schema(description = "위도 (한국 범위 약 33.0~39.0)", example = "37.4979", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "33.0", message = "위도가 한국 범위를 벗어났습니다.")
    @DecimalMax(value = "39.0", message = "위도가 한국 범위를 벗어났습니다.")
    private Double latitude;

    @Schema(description = "경도 (한국 범위 약 124.0~132.0)", example = "127.0276", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "124.0", message = "경도가 한국 범위를 벗어났습니다.")
    @DecimalMax(value = "132.0", message = "경도가 한국 범위를 벗어났습니다.")
    private Double longitude;

    /**
     * 조건부 검증: 정보유형이 '인터넷 뉴스기사'면 newsUrl 필수.
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "인터넷 뉴스기사는 뉴스기사 URL이 필수입니다.")
    public boolean isNewsUrlValid() {
        if (infoType == InfoType.NEWS_ARTICLE) {
            return newsUrl != null && !newsUrl.isBlank();
        }
        return true;
    }
}
