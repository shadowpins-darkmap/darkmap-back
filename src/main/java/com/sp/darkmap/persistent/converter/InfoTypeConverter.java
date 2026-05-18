package com.sp.darkmap.persistent.converter;

import com.sp.darkmap.code.InfoType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * InfoType ↔ DB 한글 라벨 변환 (DB에는 "인터넷 뉴스기사" 등 한글 그대로 저장)
 */
@Converter(autoApply = false)
public class InfoTypeConverter implements AttributeConverter<InfoType, String> {

    @Override
    public String convertToDatabaseColumn(InfoType attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public InfoType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InfoType.from(dbData);
    }
}
