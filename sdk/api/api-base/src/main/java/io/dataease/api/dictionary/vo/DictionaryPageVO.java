package io.dataease.api.dictionary.vo;

import java.util.List;

public record DictionaryPageVO(List<DictionaryTableVO> items, long total, int page, int size) {
}
