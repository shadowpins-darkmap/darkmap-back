package com.sp.cyberflashing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "사이버 플래싱 사례 단건")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CyberFlashingCaseVO {

    @Schema(description = "사례 ID (PK)", example = "717")
    private Long id;

    @Schema(description = "국가 코드 (ISO alpha-2)", example = "GB")
    private String countryCode;

    @Schema(description = "중복 여부", example = "", nullable = true)
    private String duplicateFlag;

    @Schema(description = "포함 여부", example = "포함")
    private String includeFlag;

    @Schema(description = "비고 (CSV 4번째 컬럼 — 데이터 큐레이션/수정 메모)", example = "국가 기입: 캐나다", nullable = true)
    private String editNote;

    @Schema(description = "기사 제목", example = "Tech firms face cyberflashing crackdown - MSN")
    private String articleTitle;

    @Schema(description = "본문", example = "Tech firms face cyberflashing crackdown MSN", nullable = true)
    private String content;

    @Schema(description = "기사 원문 URL", example = "https://www.msn.com/en-gb/news/uknews/tech-firms-face-cyberflashing-crackdown")
    private String url;

    @Schema(description = "발생 일자 (yyyyMMdd 형식 문자열)", example = "20260427")
    private String occurredDate;

    @Schema(description = "RSS URL (Google News RSS 링크)", example = "https://news.google.com/rss/articles/CBMiqgJBVV95cUxP...?oc=5", nullable = true)
    private String rssUrl;

    @Schema(description = "언론사", example = "BBC", nullable = true)
    private String press;

    @Schema(description = "기고자", example = "John Doe", nullable = true)
    private String reporter;

    @Schema(description = "비고 (CSV 12번째 컬럼)", example = "", nullable = true)
    private String note;
}
