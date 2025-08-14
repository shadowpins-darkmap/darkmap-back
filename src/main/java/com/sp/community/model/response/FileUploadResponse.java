package com.sp.community.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long fileId;
    private String originalFileName;
    private String storedFileName;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
}