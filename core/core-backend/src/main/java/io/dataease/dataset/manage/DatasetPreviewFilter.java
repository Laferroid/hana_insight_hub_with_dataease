package io.dataease.dataset.manage;

import io.dataease.api.permissions.dataset.dto.DataSetRowPermissionsTreeDTO;
import io.dataease.exception.DEException;
import io.dataease.extensions.datasource.dto.DatasetTableFieldDTO;
import io.dataease.extensions.view.dto.DatasetRowPermissionsTreeItem;
import io.dataease.extensions.view.dto.DatasetRowPermissionsTreeObj;
import io.dataease.utils.JsonUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Validates a preview filter against fields the current user may read. */
public final class DatasetPreviewFilter {
    private static final Set<String> TERMS = Set.of("eq", "not_eq", "lt", "gt", "le", "ge",
            "like", "not like", "null", "not_null", "empty", "not_empty", "in", "not in");

    private DatasetPreviewFilter() {
    }

    public static DataSetRowPermissionsTreeDTO parse(String expressionTree, List<DatasetTableFieldDTO> visibleFields) {
        if (StringUtils.isBlank(expressionTree)) {
            return null;
        }
        if (expressionTree.length() > 65536) {
            throw DEException.getException("筛选条件过长");
        }
        DatasetRowPermissionsTreeObj tree = JsonUtil.parseObject(expressionTree, DatasetRowPermissionsTreeObj.class);
        Map<Long, DatasetTableFieldDTO> fields = visibleFields.stream()
                .collect(Collectors.toMap(DatasetTableFieldDTO::getId, Function.identity(), (first, second) -> first));
        validate(tree, fields, 0);
        DataSetRowPermissionsTreeDTO filter = new DataSetRowPermissionsTreeDTO();
        filter.setTree(tree);
        filter.setExportData(true);
        return filter;
    }

    private static void validate(DatasetRowPermissionsTreeObj tree, Map<Long, DatasetTableFieldDTO> fields, int depth) {
        if (tree == null || depth > 8 || !Set.of("and", "or").contains(StringUtils.defaultString(StringUtils.lowerCase(tree.getLogic())))
                || tree.getItems() == null || tree.getItems().size() > 100) {
            throw DEException.getException("无效的筛选条件");
        }
        for (DatasetRowPermissionsTreeItem item : tree.getItems()) {
            if (item == null) {
                throw DEException.getException("无效的筛选条件");
            }
            if ("tree".equals(item.getType())) {
                validate(item.getSubTree(), fields, depth + 1);
            } else if ("item".equals(item.getType())) {
                DatasetTableFieldDTO field = fields.get(item.getFieldId());
                if (field == null || !Set.of("logic", "enum").contains(StringUtils.defaultString(item.getFilterType()))
                        || ("logic".equals(item.getFilterType()) && !TERMS.contains(StringUtils.defaultString(item.getTerm())))) {
                    throw DEException.getException("筛选字段或操作符无效");
                }
                item.setField(field);
            } else {
                throw DEException.getException("无效的筛选条件");
            }
        }
    }
}
