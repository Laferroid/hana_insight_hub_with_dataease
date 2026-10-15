package io.dataease.api.dictionary.vo;

public record DictionaryFieldVO(String fieldName, String dataType, String description,
                                boolean primaryKey, boolean requiredField, String remark,
                                int ordinal) {
}
