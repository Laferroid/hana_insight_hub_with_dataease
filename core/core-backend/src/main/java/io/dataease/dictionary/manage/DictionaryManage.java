package io.dataease.dictionary.manage;

import io.dataease.api.dictionary.vo.DictionaryDetailVO;
import io.dataease.api.dictionary.vo.DictionaryFieldVO;
import io.dataease.api.dictionary.vo.DictionaryImportVO;
import io.dataease.api.dictionary.vo.DictionaryPageVO;
import io.dataease.api.dictionary.vo.DictionarySourceVO;
import io.dataease.api.dictionary.vo.DictionaryTableVO;
import io.dataease.api.permissions.auth.dto.BusiPerCheckDTO;
import io.dataease.constant.AuthEnum;
import io.dataease.constant.BusiResourceEnum;
import io.dataease.dao.auto.entity.CoreDatasource;
import io.dataease.dao.auto.entity.CoreDatasetTable;
import io.dataease.api.dataset.union.DatasetTableInfoDTO;
import io.dataease.dictionary.dao.entity.CoreDictionaryField;
import io.dataease.dictionary.dao.entity.CoreDictionaryImport;
import io.dataease.dictionary.dao.entity.CoreDictionaryTable;
import io.dataease.dictionary.dao.repository.CoreDictionaryFieldRepository;
import io.dataease.dictionary.dao.repository.CoreDictionaryTableRepository;
import io.dataease.datasource.dao.auto.repository.CoreDatasourceRepository;
import io.dataease.datasource.provider.CalciteProvider;
import io.dataease.exception.DEException;
import io.dataease.extensions.datasource.dto.TableField;
import io.dataease.extensions.datasource.dto.DatasetTableFieldDTO;
import io.dataease.permission.util.V3UserUtil;
import io.dataease.system.manage.CorePermissionManage;
import io.dataease.utils.IDUtils;
import io.dataease.utils.JsonUtil;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class DictionaryManage {
    private static final long MAX_IMPORT_BYTES = 20L * 1024 * 1024;

    private final CoreDatasourceRepository datasourceRepository;
    private final CoreDictionaryTableRepository tableRepository;
    private final CoreDictionaryFieldRepository fieldRepository;
    private final CorePermissionManage permissionManage;
    private final CalciteProvider calciteProvider;
    private final EntityManager entityManager;

    public DictionaryManage(CoreDatasourceRepository datasourceRepository,
                            CoreDictionaryTableRepository tableRepository,
                            CoreDictionaryFieldRepository fieldRepository,
                            CorePermissionManage permissionManage,
                            CalciteProvider calciteProvider,
                            EntityManager entityManager) {
        this.datasourceRepository = datasourceRepository;
        this.tableRepository = tableRepository;
        this.fieldRepository = fieldRepository;
        this.permissionManage = permissionManage;
        this.calciteProvider = calciteProvider;
        this.entityManager = entityManager;
    }

    public boolean isAdmin() {
        return Objects.equals(V3UserUtil.getUid(), 1L);
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            DEException.throwException("仅系统管理员可以导入数据字典");
        }
    }

    public boolean canRead(Long datasourceId) {
        if (isAdmin()) {
            return true;
        }
        try {
            return permissionManage.checkAuth(new BusiPerCheckDTO(datasourceId,
                    BusiResourceEnum.DATASOURCE, AuthEnum.READ));
        } catch (Exception ignored) {
            return false;
        }
    }

    public DictionaryPageVO search(String keyword, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        List<CoreDatasource> sources = datasourceRepository.findAll();
        Map<Long, String> names = new HashMap<>();
        List<Long> allowedIds = new ArrayList<>();
        for (CoreDatasource source : sources) {
            if (!"folder".equalsIgnoreCase(source.getType()) && canRead(source.getId())) {
                allowedIds.add(source.getId());
                names.put(source.getId(), source.getName());
            }
        }
        if (allowedIds.isEmpty()) {
            return new DictionaryPageVO(List.of(), 0, safePage, safeSize);
        }
        String term = StringUtils.left(StringUtils.trimToEmpty(keyword), 100).toLowerCase(Locale.ROOT);
        Page<CoreDictionaryTable> result = tableRepository.search(allowedIds, term,
                PageRequest.of(safePage, safeSize));
        Map<Long, List<String>> matchedFields = matchingFields(result.getContent(), term);
        return new DictionaryPageVO(result.stream().map(table ->
                        toVO(table, names.get(table.getDatasourceId()), matchedFields.getOrDefault(table.getId(), List.of())))
                        .toList(),
                result.getTotalElements(), safePage, safeSize);
    }

    public DictionaryDetailVO detail(Long id) {
        CoreDictionaryTable table = readableTable(id);
        String datasourceName = datasourceRepository.findById(table.getDatasourceId())
                .map(CoreDatasource::getName).orElse("");
        List<DictionaryFieldVO> fields = fieldRepository.findByTableIdOrderByOrdinal(id).stream()
                .map(field -> new DictionaryFieldVO(field.getFieldName(), field.getDataType(),
                        field.getDescription(), Boolean.TRUE.equals(field.getPrimaryKey()),
                        Boolean.TRUE.equals(field.getRequiredField()), field.getRemark(), field.getOrdinal()))
                .toList();
        return new DictionaryDetailVO(toVO(table, datasourceName, List.of()), fields);
    }

    public String ddl(Long id) {
        CoreDictionaryTable table = readableTable(id);
        return calciteProvider.readCreateTableSql(table.getDatasourceId(), table.getSchemaName(), table.getTableName());
    }

    public List<DictionarySourceVO> sources() {
        requireAdmin();
        return datasourceRepository.findAll().stream()
                .filter(source -> !"folder".equalsIgnoreCase(source.getType()))
                .sorted(Comparator.comparing(CoreDatasource::getName))
                .map(source -> new DictionarySourceVO(source.getId().toString(), source.getName(), source.getType()))
                .toList();
    }

    @Transactional
    public DictionaryImportVO importExcel(Long datasourceId, MultipartFile file) {
        requireAdmin();
        CoreDatasource datasource = datasourceRepository.findById(datasourceId)
                .filter(source -> !"folder".equalsIgnoreCase(source.getType()))
                .orElseThrow(() -> DEException.getException("数据源不存在"));
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMPORT_BYTES ||
                !StringUtils.endsWithIgnoreCase(file.getOriginalFilename(), ".xlsx")) {
            DEException.throwException("请上传不超过 20 MB 的 .xlsx 数据字典文件");
        }
        List<ImportedTable> imported;
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            imported = parseWorkbook(workbook);
        } catch (IOException | RuntimeException e) {
            if (e instanceof DEException deException) {
                throw deException;
            }
            throw DEException.getException("数据字典解析失败：" + e.getMessage());
        }

        long now = System.currentTimeMillis();
        CoreDictionaryImport batch = new CoreDictionaryImport();
        batch.setId(IDUtils.snowID());
        batch.setDatasourceId(datasource.getId());
        batch.setFileName(StringUtils.left(file.getOriginalFilename(), 512));
        batch.setTableCount(imported.size());
        batch.setFieldCount(imported.stream().mapToInt(item -> item.fields().size()).sum());
        batch.setImportedBy(V3UserUtil.getUid());
        batch.setImportedAt(now);
        entityManager.persist(batch);

        for (ImportedTable item : imported) {
            Optional<CoreDictionaryTable> existing = tableRepository
                    .findByDatasourceIdAndSchemaNameAndTableName(datasourceId, item.schema(), item.name());
            CoreDictionaryTable table = existing
                    .orElseGet(() -> {
                        CoreDictionaryTable created = new CoreDictionaryTable();
                        created.setId(IDUtils.snowID());
                        created.setDatasourceId(datasourceId);
                        created.setSchemaName(item.schema());
                        created.setTableName(item.name());
                        return created;
                    });
            table.setDisplayName(item.displayName());
            table.setPurpose(item.displayName());
            table.setSourceName(StringUtils.defaultIfBlank(item.sourceName(), datasource.getName()));
            table.setSubjectName(item.subjectName());
            table.setRemark(item.remark());
            table.setFieldCount(item.fields().size());
            table.setUpdatedAt(now);
            table.setImportId(batch.getId());
            if (existing.isEmpty()) {
                entityManager.persist(table);
            } else {
                fieldRepository.deleteByTableId(table.getId());
            }
            for (ImportedField itemField : item.fields()) {
                CoreDictionaryField field = new CoreDictionaryField();
                field.setId(IDUtils.snowID());
                field.setTableId(table.getId());
                field.setFieldName(itemField.name());
                field.setDataType(itemField.type());
                field.setDescription(itemField.description());
                field.setPrimaryKey(itemField.primaryKey());
                field.setRequiredField(itemField.required());
                field.setRemark(itemField.remark());
                field.setOrdinal(itemField.ordinal());
                entityManager.persist(field);
            }
        }
        entityManager.flush();
        return new DictionaryImportVO(batch.getId().toString(), batch.getTableCount(), batch.getFieldCount());
    }

    public void enrichFields(Long datasourceId, String rawTableName, List<TableField> fields) {
        if (fields == null || fields.isEmpty() || StringUtils.isBlank(rawTableName) || !canRead(datasourceId)) {
            return;
        }
        String tableName = rawTableName;
        String schemaName = null;
        int separator = rawTableName.lastIndexOf('.');
        if (separator > 0) {
            schemaName = rawTableName.substring(0, separator);
            tableName = rawTableName.substring(separator + 1);
        }
        List<CoreDictionaryTable> matches = tableRepository.findByDatasourceIdAndTableName(datasourceId, tableName);
        String finalSchemaName = schemaName;
        List<CoreDictionaryTable> matchingSchema = matches.stream()
                .filter(table -> finalSchemaName == null || table.getSchemaName().equalsIgnoreCase(finalSchemaName))
                .toList();
        if (matchingSchema.size() != 1) {
            return;
        }
        Map<String, CoreDictionaryField> descriptions = new HashMap<>();
        for (CoreDictionaryField field : fieldRepository.findByTableIdOrderByOrdinal(matchingSchema.get(0).getId())) {
            descriptions.put(field.getFieldName().toLowerCase(Locale.ROOT), field);
        }
        for (TableField field : fields) {
            if (field.getOriginName() == null) {
                continue;
            }
            CoreDictionaryField description = descriptions.get(field.getOriginName().toLowerCase(Locale.ROOT));
            if (description != null && StringUtils.isNotBlank(description.getDescription())) {
                field.setName(description.getDescription());
            }
        }
    }

    /** 对已保存的数据集仅更新返回给编辑页的备注，不改动用户自定义字段名。 */
    public void enrichDatasetDescriptions(List<CoreDatasetTable> tables, List<DatasetTableFieldDTO> fields) {
        Map<Long, List<DatasetTableFieldDTO>> fieldsByTable = new HashMap<>();
        for (DatasetTableFieldDTO field : fields) {
            if (field.getDatasetTableId() != null) {
                fieldsByTable.computeIfAbsent(field.getDatasetTableId(), ignored -> new ArrayList<>()).add(field);
            }
        }
        for (CoreDatasetTable table : tables) {
            if (!"db".equalsIgnoreCase(table.getType()) || table.getDatasourceId() == null
                    || !canRead(table.getDatasourceId())) {
                continue;
            }
            List<DatasetTableFieldDTO> tableFields = fieldsByTable.get(table.getId());
            if (tableFields == null || tableFields.isEmpty()) {
                continue;
            }
            if (StringUtils.isBlank(table.getInfo())) {
                continue;
            }
            DatasetTableInfoDTO info = JsonUtil.parseObject(table.getInfo(), DatasetTableInfoDTO.class);
            if (info == null || StringUtils.isBlank(info.getTable())) {
                continue;
            }
            String rawName = info.getTable();
            String name = rawName.substring(rawName.lastIndexOf('.') + 1);
            String schema = rawName.contains(".") ? rawName.substring(0, rawName.lastIndexOf('.')) : null;
            List<CoreDictionaryTable> matches = tableRepository.findByDatasourceIdAndTableName(table.getDatasourceId(), name)
                    .stream().filter(item -> schema == null || item.getSchemaName().equalsIgnoreCase(schema)).toList();
            if (matches.size() != 1) {
                continue;
            }
            Map<String, String> descriptions = new HashMap<>();
            for (CoreDictionaryField field : fieldRepository.findByTableIdOrderByOrdinal(matches.get(0).getId())) {
                descriptions.put(field.getFieldName().toLowerCase(Locale.ROOT), field.getDescription());
            }
            for (DatasetTableFieldDTO field : tableFields) {
                if (field.getOriginName() != null) {
                    String description = descriptions.get(field.getOriginName().toLowerCase(Locale.ROOT));
                    if (StringUtils.isNotBlank(description)) {
                        field.setDescription(description);
                    }
                }
            }
        }
    }

    private CoreDictionaryTable readableTable(Long id) {
        CoreDictionaryTable table = tableRepository.findById(id)
                .orElseThrow(() -> DEException.getException("数据表未收录"));
        if (!canRead(table.getDatasourceId())) {
            DEException.throwException("没有该数据源的读取权限");
        }
        return table;
    }

    private Map<Long, List<String>> matchingFields(List<CoreDictionaryTable> tables, String keyword) {
        if (StringUtils.isBlank(keyword) || tables.isEmpty()) {
            return Map.of();
        }
        List<Long> tableIds = tables.stream().map(CoreDictionaryTable::getId).toList();
        Map<Long, List<String>> matches = new HashMap<>();
        for (CoreDictionaryField field : fieldRepository.findByTableIdInOrderByTableIdAscOrdinalAsc(tableIds)) {
            if (StringUtils.containsIgnoreCase(field.getFieldName(), keyword)
                    || StringUtils.containsIgnoreCase(field.getDescription(), keyword)) {
                List<String> names = matches.computeIfAbsent(field.getTableId(), ignored -> new ArrayList<>(3));
                if (names.size() < 3) {
                    names.add(field.getFieldName());
                }
            }
        }
        return matches;
    }

    private DictionaryTableVO toVO(CoreDictionaryTable table, String datasourceName, List<String> matchingFields) {
        return new DictionaryTableVO(table.getId().toString(), table.getDatasourceId().toString(),
                datasourceName, table.getSchemaName(), table.getTableName(), table.getDisplayName(),
                table.getPurpose(), table.getSourceName(), table.getSubjectName(), table.getRemark(),
                table.getFieldCount(), table.getUpdatedAt(), matchingFields);
    }

    private List<ImportedTable> parseWorkbook(Workbook workbook) {
        Sheet catalog = workbook.getSheet("目录");
        if (catalog == null) {
            DEException.throwException("缺少“目录”工作表");
        }
        DataFormatter formatter = new DataFormatter();
        if (!"表名".equals(cell(catalog.getRow(0), 1, formatter)) ||
                !"schema".equalsIgnoreCase(cell(catalog.getRow(0), 4, formatter))) {
            DEException.throwException("目录表头不符合数据字典模板");
        }
        Map<String, Sheet> sheets = new HashMap<>();
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            Sheet sheet = workbook.getSheetAt(index);
            if (sheet != catalog) {
                sheets.put(sheet.getSheetName(), sheet);
                String qualifiedName = cell(sheet.getRow(0), 1, formatter);
                if (StringUtils.isNotBlank(qualifiedName)) {
                    sheets.put(qualifiedName, sheet);
                }
            }
        }
        List<ImportedTable> result = new ArrayList<>();
        Set<String> seenTables = new HashSet<>();
        for (int rowIndex = 1; rowIndex <= catalog.getLastRowNum(); rowIndex++) {
            Row row = catalog.getRow(rowIndex);
            String tableName = cell(row, 1, formatter);
            if (tableName.isEmpty()) {
                continue;
            }
            String schema = cell(row, 4, formatter);
            // Some exported dictionaries append an AIGC metadata row after the table catalog.
            // Its JSON payload is in the table-name column, but it is not a database table.
            if (StringUtils.startsWithIgnoreCase(cell(row, 0, formatter), "AIGC:")
                    && StringUtils.startsWith(tableName, "{") && schema.isEmpty()) {
                continue;
            }
            if (tableName.length() > 255 || schema.length() > 255 ||
                    !seenTables.add(schema + "\u0000" + tableName)) {
                DEException.throwException("目录中存在重复或过长的表名：" + tableName);
            }
            Sheet sheet = sheets.get(schema + "." + tableName);
            if (sheet == null) {
                sheet = sheets.get(tableName);
            }
            if (sheet == null || !"字段名".equals(cell(sheet.getRow(1), 0, formatter))) {
                DEException.throwException("缺少数据表字段页：" + schema + "." + tableName);
            }
            List<ImportedField> fields = new ArrayList<>();
            Set<String> seenFields = new HashSet<>();
            for (int fieldRow = 2; fieldRow <= sheet.getLastRowNum(); fieldRow++) {
                Row dataRow = sheet.getRow(fieldRow);
                String fieldName = cell(dataRow, 0, formatter);
                if (fieldName.isEmpty()) {
                    continue;
                }
                if (fieldName.length() > 255 || !seenFields.add(fieldName.toLowerCase(Locale.ROOT))) {
                    DEException.throwException("字段重复或过长：" + tableName + "." + fieldName);
                }
                fields.add(new ImportedField(fieldName, cell(dataRow, 1, formatter),
                        cell(dataRow, 2, formatter), "Y".equalsIgnoreCase(cell(dataRow, 3, formatter)),
                        "Y".equalsIgnoreCase(cell(dataRow, 4, formatter)),
                        cell(dataRow, 5, formatter), fields.size() + 1));
            }
            result.add(new ImportedTable(schema, tableName, cell(row, 2, formatter),
                    cell(row, 3, formatter), cell(row, 0, formatter), cell(row, 6, formatter), fields));
        }
        if (result.isEmpty()) {
            DEException.throwException("数据字典中没有可导入的数据表");
        }
        return result;
    }

    private String cell(Row row, int column, DataFormatter formatter) {
        return row == null || row.getCell(column) == null ? "" :
                StringUtils.trimToEmpty(formatter.formatCellValue(row.getCell(column)));
    }

    private record ImportedTable(String schema, String name, String displayName, String sourceName,
                                 String subjectName, String remark, List<ImportedField> fields) {
    }

    private record ImportedField(String name, String type, String description, boolean primaryKey,
                                 boolean required, String remark, int ordinal) {
    }
}
