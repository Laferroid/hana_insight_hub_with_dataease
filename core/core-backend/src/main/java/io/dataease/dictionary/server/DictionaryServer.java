package io.dataease.dictionary.server;

import io.dataease.api.dictionary.DictionaryApi;
import io.dataease.api.dictionary.vo.DictionaryDetailVO;
import io.dataease.api.dictionary.vo.DictionaryImportVO;
import io.dataease.api.dictionary.vo.DictionaryPageVO;
import io.dataease.api.dictionary.vo.DictionarySourceVO;
import io.dataease.dictionary.manage.DictionaryManage;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/dictionary")
public class DictionaryServer implements DictionaryApi {
    private final DictionaryManage dictionaryManage;

    public DictionaryServer(DictionaryManage dictionaryManage) {
        this.dictionaryManage = dictionaryManage;
    }

    @Override
    public DictionaryPageVO search(String keyword, int page, int size) {
        return dictionaryManage.search(keyword, page, size);
    }

    @Override
    public DictionaryPageVO suggest(String keyword) {
        return dictionaryManage.search(keyword, 0, 8);
    }

    @Override
    public DictionaryDetailVO detail(Long id) {
        return dictionaryManage.detail(id);
    }

    @Override
    public String ddl(Long id) {
        return dictionaryManage.ddl(id);
    }

    @Override
    public List<DictionarySourceVO> sources() {
        return dictionaryManage.sources();
    }

    @Override
    public DictionaryImportVO importExcel(Long datasourceId, MultipartFile file) {
        return dictionaryManage.importExcel(datasourceId, file);
    }

    @Override
    public boolean isAdmin() {
        return dictionaryManage.isAdmin();
    }
}
