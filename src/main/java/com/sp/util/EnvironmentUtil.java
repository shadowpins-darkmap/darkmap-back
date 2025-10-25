package com.sp.util;


import com.sp.config.EnvironmentConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class EnvironmentUtil {

    public static EnvironmentConfig determineEnvironment(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        log.debug("Request headers - Origin: {}, Referer: {}", origin, referer);

        // 1. Origin 헤더가 있으면 그대로 사용
        if (origin != null && !origin.isEmpty()) {
            boolean isLocal = origin.contains("localhost") || origin.contains("127.0.0.1");
            String cookieDomain = isLocal ? "localhost" : extractDomain(origin);
            return new EnvironmentConfig(origin, cookieDomain, isLocal);
        }

        // 2. Referer에서 origin 추출
        if (referer != null && !referer.isEmpty()) {
            String extractedOrigin = extractOriginFromReferer(referer);
            boolean isLocal = extractedOrigin.contains("localhost") || extractedOrigin.contains("127.0.0.1");
            String cookieDomain = isLocal ? "localhost" : extractDomain(extractedOrigin);
            return new EnvironmentConfig(extractedOrigin, cookieDomain, isLocal);
        }

        // 3. 기본값 (운영환경)
        return new EnvironmentConfig(
                "https://kdark.weareshadowpins.co.kr",
                "api.kdark.weareshadowpins.com",
                false
        );
    }

    private static String extractOriginFromReferer(String referer) {
        try {
            URI uri = new URI(referer);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            log.warn("Failed to parse referer: {}", referer, e);
            try {
                String[] parts = referer.split("/");
                if (parts.length >= 3) {
                    return parts[0] + "//" + parts[2];
                }
            } catch (Exception ex) {
                log.error("Failed to extract origin from referer: {}", referer, ex);
            }
            return referer;
        }
    }

    private static String extractDomain(String origin) {
        return origin.replaceAll("^https?://", "");
    }
}