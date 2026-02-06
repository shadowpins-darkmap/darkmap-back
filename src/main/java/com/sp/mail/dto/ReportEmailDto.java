package com.sp.mail.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 제보 이메일 전송용 DTO
 */
@Getter
@Builder
public class ReportEmailDto {

    private String reporterName;        // 제보자 이름
    private String reporterEmail;       // 제보자 이메일
    private String reportTitle;         // 제보 제목
    private String reportContent;       // 제보 내용
    private String reportCategory;      // 제보 카테고리
    private String reportLocation;      // 제보 위치/장소
    private MultipartFile attachmentFile; // 첨부 이미지 파일

    // 추가 정보
    private String reportDate;          // 제보 접수 시간
    private String reportId;            // 제보 ID (추적용)
}
