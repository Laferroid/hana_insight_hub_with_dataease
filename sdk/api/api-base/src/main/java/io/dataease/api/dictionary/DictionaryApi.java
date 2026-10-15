package io.dataease.api.dictionary;

import io.dataease.api.dictionary.vo.DictionaryDetailVO;
import io.dataease.api.dictionary.vo.DictionaryImportVO;
import io.dataease.api.dictionary.vo.DictionaryPageVO;
import io.dataease.api.dictionary.vo.DictionarySourceVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DictionaryApi {
    @GetMapping("/search")
    DictionaryPageVO search(@RequestParam(defaultValue = "") String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size);

    @GetMapping("/suggest")
    DictionaryPageVO suggest(@RequestParam String keyword);

    @GetMapping("/tables/{id}")
    DictionaryDetailVO detail(@PathVariable Long id);

    @GetMapping("/tables/{id}/ddl")
    String ddl(@PathVariable Long id);

    @GetMapping("/sources")
    List<DictionarySourceVO> sources();

    @PostMapping("/import")
    DictionaryImportVO importExcel(@RequestParam Long datasourceId, @RequestParam MultipartFile file);

    @GetMapping("/admin")
    boolean isAdmin();
}
