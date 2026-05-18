package com.sp.darkmap.persistent.converter;

import com.sp.darkmap.code.MapCrimeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * MapCrimeType ↔ DB 한글 라벨 변환 (DB에는 "바바리맨" 등 한글 그대로 저장)
 */
@Converter(autoApply = false)
public class MapCrimeTypeConverter implements AttributeConverter<MapCrimeType, String> {

    @Override
    public String convertToDatabaseColumn(MapCrimeType attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public MapCrimeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MapCrimeType.from(dbData);
    }
}
