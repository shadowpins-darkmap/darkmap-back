package com.sp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
   //     registry.addMapping("/**")
   //             .allowedOrigins(
   //                     "https://kdark.weareshadowpins.com",
   //                     "https://darkmap-pi.vercel.app",
    //                    "http://localhost:3000",
    //                    "http://localhost:3001",
    //                    "http://localhost:8080",
    //                    "http://localhost:8081",
     //                   "http://127.0.0.1:3000",
     //                   "http://127.0.0.1:8080"
    //            )
    //            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    //            .allowedHeaders("*")
    //            .allowCredentials(true)
    //            .maxAge(3600);
   // }

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 핸들러 추가
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
