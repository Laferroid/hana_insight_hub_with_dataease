package io.dataease.api.dictionary.vo;

import java.util.List;

public record DictionaryDetailVO(DictionaryTableVO table, List<DictionaryFieldVO> fields) {
}
