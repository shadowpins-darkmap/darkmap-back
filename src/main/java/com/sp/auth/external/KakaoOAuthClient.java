package com.sp.auth.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.auth.model.vo.KakaoUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Component
public class KakaoOAuthClient {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public String getAccessToken(String code) {
        try {
            URL url = new URL(tokenUri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String params = "grant_type=authorization_code"
                    + "&client_id=" + clientId
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
                    + "&code=" + code;

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                bw.write(params);
                bw.flush();
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(result.toString());
            return rootNode.get("access_token").asText();

        } catch (IOException e) {
            throw new RuntimeException("카카오 토큰 요청 실패", e);
        }
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            URL url = new URL("https://kapi.kakao.com/v2/user/me");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.toString());

            String email = json.get("kakao_account").get("email").asText();
            String userId = json.get("id").asText();

            return new KakaoUserInfo(email, userId);
        } catch (IOException e) {
            throw new RuntimeException("사용자 정보 조회 실패", e);
        }
    }

    public void unlink(String accessToken) {
        try {
            URL url = new URL("https://kapi.kakao.com/v1/user/unlink");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.getInputStream().close();
        } catch (IOException e) {
            throw new RuntimeException("카카오 연동 해제 실패", e);
        }
    }

}

