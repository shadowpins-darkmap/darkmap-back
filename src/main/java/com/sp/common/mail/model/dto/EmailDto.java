package com.sp.common.mail.model.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 일반 이메일 전송용 DTO
 */
@Getter
@Builder
public class EmailDto {

    private String to;              // 받는 사람
    private String from;            // 보내는 사람
    private String subject;         // 제목
    private String content;         // 내용 (HTML 또는 텍스트)
    private boolean isHtml;         // HTML 여부
    private MultipartFile attachment; // 첨부파일
    private String attachmentName;  // 첨부파일명
}