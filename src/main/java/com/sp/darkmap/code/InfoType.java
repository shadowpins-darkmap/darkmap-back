package com.sp.darkmap.code;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 범죄 사례 정보유형 (DB·JSON 모두 한글 라벨로 입출력)
 */
public enum InfoType {

    NEWS_ARTICLE("인터넷 뉴스기사"),
    MEMBER_EXPERIENCE("회원의 경험담");

    private final String label;

    InfoType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static InfoType from(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (InfoType type : values()) {
            if (type.label.equals(v) || type.name().equalsIgnoreCase(v)) {
                return type;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 정보유형: " + value);
    }
}
