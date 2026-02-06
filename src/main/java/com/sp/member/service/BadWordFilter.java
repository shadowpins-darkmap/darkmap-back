package com.sp.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class BadWordFilter {

    private Set<String> badWords = new HashSet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        loadBadWords();
    }

    private void loadBadWords() {
        try {
            ClassPathResource resource = new ClassPathResource("badwords.json");
            //https://github.com/organization/Gentleman/blob/master/resources/badwords.json
            InputStream inputStream = resource.getInputStream();

            JsonNode rootNode = objectMapper.readTree(inputStream);
            JsonNode badWordsNode = rootNode.get("badwords");

            if (badWordsNode != null && badWordsNode.isArray()) {
                badWordsNode.forEach(node -> badWords.add(node.asText().toLowerCase()));
                log.info("금지어 {} 개를 로드했습니다.", badWords.size());
            }

        } catch (Exception e) {
            log.error("금지어 파일 로드 실패: {}", e.getMessage());
            // 기본 금지어 설정 (최소한의 안전장치)
            badWords.add("test");
        }
    }

    public boolean containsBadWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 텍스트 정규화 (소문자 변환, 공백 제거)
        String normalizedText = text.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^가-힣a-z0-9]", ""); // 특수문자 제거

        // 완전 일치 검사
        if (badWords.contains(normalizedText)) {
            return true;
        }

        // 부분 일치 검사 (금지어가 포함되어 있는지)
        for (String badWord : badWords) {
            if (normalizedText.contains(badWord)) {
                return true;
            }
        }

        return false;
    }

    public boolean isValidNickname(String nickname) {
        return !containsBadWord(nickname);
    }

    // 테스트용 메서드
    public int getBadWordCount() {
        return badWords.size();
    }
}