package com.sp.auth.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.auth.model.vo.KakaoTokenResponse;
import com.sp.auth.model.vo.KakaoUserInfo;
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
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    /**
     * 카카오 액세스 토큰 및 만료 정보 조회
     */
    public KakaoTokenResponse getTokenResponse(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("redirect_uri", redirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                String accessToken = (String) body.get("access_token");
                String refreshToken = (String) body.get("refresh_token");
                Integer expiresIn = (Integer) body.get("expires_in"); // 초 단위

                return KakaoTokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresIn(expiresIn)
                        .build();
            }

            throw new RuntimeException("카카오 토큰 요청 실패");

        } catch (Exception e) {
            log.error("카카오 토큰 요청 실패", e);
            throw new RuntimeException("카카오 토큰 요청 실패", e);
        }
    }

    /**
     * 기존 호환성을 위한 메서드 (Deprecated)
     */
    @Deprecated
    public String getAccessToken(String code) {
        return getTokenResponse(code).getAccessToken();
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JsonNode json = objectMapper.readTree(response.getBody());
            String email = json.get("kakao_account").get("email").asText();
            String userId = json.get("id").asText();

            return new KakaoUserInfo(email, userId);

        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            throw new RuntimeException("사용자 정보 조회 실패", e);
        }
    }

    public void unlink(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    "https://kapi.kakao.com/v1/user/unlink",
                    HttpMethod.POST,
                    request,
                    Void.class
            );

            log.info("✅ 카카오 연동 해제 성공");

        } catch (Exception e) {
            log.error("❌ 카카오 연동 해제 실패", e);
            throw new RuntimeException("카카오 연동 해제 실패", e);
        }
    }
}