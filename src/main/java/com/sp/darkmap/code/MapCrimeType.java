package com.sp.darkmap.code;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 범죄 사례 범죄유형 (DB·JSON 모두 한글 라벨로 입출력).
 * 기존 미사용 enum {@link CrimeType} 과 분리한 신규 어휘 체계.
 */
public enum MapCrimeType {

    FLASHER("바바리맨"),
    HUNTING("헌팅"),
    TAILING("미행");

    private final String label;

    MapCrimeType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static MapCrimeType from(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (MapCrimeType type : values()) {
            if (type.label.equals(v) || type.name().equalsIgnoreCase(v)) {
                return type;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 범죄유형: " + value);
    }
}
