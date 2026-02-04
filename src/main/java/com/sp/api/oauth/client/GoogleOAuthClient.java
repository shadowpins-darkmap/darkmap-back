package com.sp.api.oauth.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * 만료된 액세스 토큰을 리프레시 토큰으로 갱신
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            } else {
                log.error("액세스 토큰 갱신 실패. 응답 코드: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("액세스 토큰 갱신 중 오류", e);
            return null;
        }
    }

    /**
     * 구글 토큰 revoke (스마트 처리)
     */
    public boolean smartRevokeToken(String accessToken, String refreshToken) {
        // 1. 액세스 토큰으로 먼저 시도
        if (accessToken != null && revokeToken(accessToken)) {
            log.info("액세스 토큰으로 연동 해제 성공");
            return true;
        }

        // 2. 액세스 토큰 실패 시 리프레시 토큰 시도
        if (refreshToken != null && revokeToken(refreshToken)) {
            log.info("리프레시 토큰으로 연동 해제 성공");
            return true;
        }

        // 3. 리프레시 토큰으로 새 액세스 토큰 발급 후 재시도
        if (refreshToken != null) {
            String newAccessToken = refreshAccessToken(refreshToken);
            if (newAccessToken != null && revokeToken(newAccessToken)) {
                log.info("새 액세스 토큰으로 연동 해제 성공");
                return true;
            }
        }

        log.warn("모든 토큰으로 연동 해제 실패");
        return false;
    }

    /**
     * 구글 액세스 토큰 revoke (연동 해제)
     */
    public boolean revokeToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("token", token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/revoke",
                    request,
                    Void.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("구글 토큰 revoke 성공");
                return true;
            } else {
                log.warn("구글 토큰 revoke 실패. 응답 코드: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("구글 토큰 revoke 실패", e);
            return false;
        }
    }

    /**
     * 구글 리프레시 토큰 revoke (연동 해제)
     */
    public boolean revokeRefreshToken(String refreshToken) {
        return revokeToken(refreshToken);
    }
}