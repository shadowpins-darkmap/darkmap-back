package com.sp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "file")
@Getter
@Setter
public class FileProperties {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${file.max.size}")
    private long maxFileSize;

    @Value("${file.allowed.extensions}")
    private String[] allowedExtensions;

    @Value("${file.base.url}")
    private String baseUrl;
}
