package io.dataease.listener.sql;

import io.dataease.initSql.Version;
import io.dataease.menu.dao.auto.entity.CoreMenu;
import io.dataease.menu.dao.auto.mapper.CoreMenuRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DictionaryInit implements CoreSqlBlock {
    private final JdbcTemplate jdbcTemplate;
    private final CoreMenuRepository menuRepository;

    public DictionaryInit(JdbcTemplate jdbcTemplate, CoreMenuRepository menuRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.menuRepository = menuRepository;
    }

    @Override
    public Version getVersion() {
        return new Version("2.41");
    }

    @Override
    public void execute() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS core_dictionary_import (" +
                "id BIGINT PRIMARY KEY, datasource_id BIGINT NOT NULL, file_name VARCHAR(512) NOT NULL, " +
                "table_count INTEGER NOT NULL, field_count INTEGER NOT NULL, imported_by BIGINT NOT NULL, " +
                "imported_at BIGINT NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS core_dictionary_table (" +
                "id BIGINT PRIMARY KEY, datasource_id BIGINT NOT NULL, schema_name VARCHAR(255) NOT NULL, " +
                "table_name VARCHAR(255) NOT NULL, display_name VARCHAR(1024), purpose VARCHAR(4096), " +
                "source_name VARCHAR(255), subject_name VARCHAR(255), remark VARCHAR(4096), " +
                "field_count INTEGER NOT NULL, updated_at BIGINT NOT NULL, import_id BIGINT NOT NULL, " +
                "CONSTRAINT uk_dictionary_table UNIQUE (datasource_id, schema_name, table_name))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS core_dictionary_field (" +
                "id BIGINT PRIMARY KEY, table_id BIGINT NOT NULL, field_name VARCHAR(255) NOT NULL, " +
                "data_type VARCHAR(255), description VARCHAR(4096), primary_key BOOLEAN, required_field BOOLEAN, " +
                "remark VARCHAR(4096), ordinal INTEGER NOT NULL, " +
                "CONSTRAINT uk_dictionary_field UNIQUE (table_id, field_name))");
        createMenu(900001L, "dictionary", "visualized/data/dictionary", "/dictionary", false);
        createMenu(900002L, "dictionary-results", "visualized/data/dictionary/results", "/dictionary-results", true);
        createMenu(900003L, "dictionary-detail", "visualized/data/dictionary/detail", "/dictionary-detail", true);
    }

    private void createMenu(long id, String name, String component, String path, boolean hidden) {
        if (menuRepository.findById(id).isPresent()) {
            return;
        }
        CoreMenu menu = new CoreMenu();
        menu.setId(id);
        menu.setPid(4L);
        menu.setType(2);
        menu.setName(name);
        menu.setComponent(component);
        menu.setMenuSort(hidden ? 99 : 3);
        menu.setPath(path);
        menu.setHidden(hidden);
        menu.setInLayout(true);
        menu.setAuth(false);
        menuRepository.saveAndFlush(menu);
    }
}
