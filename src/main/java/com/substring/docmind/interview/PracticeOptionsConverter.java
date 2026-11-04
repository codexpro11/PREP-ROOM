package com.substring.docmind.interview;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.ObjectMapper;

@Converter
public class PracticeOptionsConverter implements AttributeConverter<PracticeOptions, String> {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Override public String convertToDatabaseColumn(PracticeOptions value) {
        return value == null ? null : JSON.writeValueAsString(value);
    }
    @Override public PracticeOptions convertToEntityAttribute(String value) {
        return value == null ? null : JSON.readValue(value, PracticeOptions.class);
    }
}
