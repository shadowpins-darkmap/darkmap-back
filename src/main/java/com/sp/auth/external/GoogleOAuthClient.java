package com.sp.auth.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Slf4j
@Component
public class GoogleOAuthClient {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * 만료된 액세스 토큰을 리프레시 토큰으로 갱신
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            URL url = new URL("https://oauth2.googleapis.com/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = "grant_type=refresh_token"
                    + "&client_id=" + URLEncoder.encode(clientId, "UTF-8")
                    + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
                    + "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = params.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(response.toString());
                    return rootNode.get("access_token").asText();
                }
            } else {
                log.error("액세스 토큰 갱신 실패. 응답 코드: {}", responseCode);
                return null;
            }

        } catch (IOException e) {
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
            URL url = new URL("https://oauth2.googleapis.com/revoke");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = "token=" + URLEncoder.encode(token, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = params.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                log.info("구글 토큰 revoke 성공");
                return true;
            } else {
                log.warn("구글 토큰 revoke 실패. 응답 코드: {}", responseCode);

                // 에러 응답 읽기
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    log.warn("구글 토큰 revoke 에러 응답: {}", response.toString());
                }
                return false;
            }

        } catch (IOException e) {
            log.error("구글 토큰 revoke 실패", e);
            return false;
        }
    }

    /**
     * 구글 리프레시 토큰 revoke (연동 해제)
     */
    public boolean revokeRefreshToken(String refreshToken) {
        return revokeToken(refreshToken); // 같은 API 사용
    }
}