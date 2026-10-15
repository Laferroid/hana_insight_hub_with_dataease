package io.dataease.api.dictionary.vo;

import java.util.List;

public record DictionaryTableVO(String id, String datasourceId, String datasourceName,
                                String schemaName, String tableName, String displayName,
                                String purpose, String sourceName, String subjectName,
                                String remark, int fieldCount, long updatedAt,
                                List<String> matchedFields) {
}
