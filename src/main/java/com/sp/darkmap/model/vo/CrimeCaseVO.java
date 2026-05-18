package com.sp.darkmap.model.vo;

import com.sp.darkmap.code.InfoType;
import com.sp.darkmap.code.MapCrimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "범죄 사례 단건")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CrimeCaseVO {

    @Schema(description = "사례 ID (PK)", example = "1")
    private Long id;

    @Schema(description = "정보유형", example = "회원의 경험담")
    private InfoType infoType;

    @Schema(description = "범죄유형", example = "바바리맨")
    private MapCrimeType crimeType;

    @Schema(description = "시도", example = "서울특별시")
    private String sido;

    @Schema(description = "시군구", example = "강남구")
    private String sigungu;

    @Schema(description = "뉴스기사 URL", example = "https://news.example.com/article/123", nullable = true)
    private String newsUrl;

    @Schema(description = "위도", example = "37.4979")
    private Double latitude;

    @Schema(description = "경도", example = "127.0276")
    private Double longitude;

    @Schema(description = "작성 회원 ID", example = "42", nullable = true)
    private Long reporterId;

    @Schema(description = "생성 시각", example = "2026-05-18T00:12:24")
    private LocalDateTime createdAt;
}
