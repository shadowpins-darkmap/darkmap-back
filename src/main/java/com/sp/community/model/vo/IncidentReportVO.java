package com.sp.community.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 제보 응답 VO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class IncidentReportVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 제보 유형
     */
    private String reportType;

    /**
     * 제보 위치
     */
    private String reportLocation;

    /**
     * 내용
     */
    private String content;

    /**
     * 뉴스기사 url
     */
    private String url;

    /**
     * 신고자 ID
     */
    private Long reporterId;

    /**
     * 신고 생성 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 첨부파일 원본 이름
     */
    private String attachmentOriginalName;

    /**
     * 첨부파일 저장된 이름
     */
    private String attachmentStoredName;

    /**
     * 첨부파일 크기
     */
    private Long attachmentSize;

    /**
     * 첨부파일 MIME 타입
     */
    private String attachmentContentType;

    /**
     * 첨부파일 다운로드 URL
     */
    private String attachmentUrl;

    /**
     * 첨부파일 존재 여부
     */
    public boolean hasAttachment() {
        return attachmentStoredName != null && !attachmentStoredName.trim().isEmpty();
    }
}