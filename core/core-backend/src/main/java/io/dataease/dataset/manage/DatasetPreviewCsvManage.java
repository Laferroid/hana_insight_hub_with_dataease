package io.dataease.dataset.manage;

import io.dataease.api.dataset.dto.DataSetExportRequest;
import io.dataease.api.dataset.union.DatasetGroupInfoDTO;
import io.dataease.exception.DEException;
import io.dataease.exportCenter.util.ExportCenterUtils;
import io.dataease.extensions.datasource.dto.DatasetTableFieldDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DatasetPreviewCsvManage {
    private static final int PAGE_SIZE = 1000;

    @Resource
    private DatasetGroupManage datasetGroupManage;
    @Resource
    private DatasetDataManage datasetDataManage;

    public void export(DataSetExportRequest request, HttpServletResponse response) throws Exception {
        DatasetGroupInfoDTO dataset = datasetGroupManage.getDatasetGroupInfoDTO(request.getId(), null);
        if (dataset == null || !"dataset".equalsIgnoreCase(dataset.getNodeType())) {
            throw DEException.getException("数据集不存在");
        }
        String expression = StringUtils.defaultString(request.getExpressionTree());
        Map<String, Object> first = datasetDataManage.previewDataWithLimit(dataset, 0, PAGE_SIZE, true, false, expression);
        long total = Math.max(0, Math.min((Long) first.get("total"), ExportCenterUtils.getExportLimit("dataset")));
        Map<String, Object> firstData = (Map<String, Object>) first.get("data");
        List<DatasetTableFieldDTO> fields = (List<DatasetTableFieldDTO>) firstData.get("fields");

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(dataset.getName() + ".csv", StandardCharsets.UTF_8).replace("+", "%20"));
        PrintWriter writer = response.getWriter();
        writer.write('\ufeff');
        writeRow(writer, fields.stream().map(DatasetTableFieldDTO::getName).collect(Collectors.toList()));
        for (long offset = 0; offset < total; offset += PAGE_SIZE) {
            Map<String, Object> page = offset == 0 ? first : datasetDataManage.previewDataWithLimit(
                    dataset, Math.toIntExact(offset), (int) Math.min(PAGE_SIZE, total - offset), true, false, expression);
            Map<String, Object> preview = (Map<String, Object>) page.get("data");
            List<Map<String, Object>> rows = (List<Map<String, Object>>) preview.get("data");
            for (Map<String, Object> row : rows.subList(0, (int) Math.min(rows.size(), total - offset))) {
                writeRow(writer, fields.stream().map(field -> row.get(StringUtils.defaultIfEmpty(
                        field.getDataeaseName(), field.getOriginName()))).collect(Collectors.toList()));
            }
            writer.flush();
            if (writer.checkError()) {
                throw new java.io.IOException("CSV 下载连接已中断");
            }
        }
    }

    private void writeRow(PrintWriter writer, List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            String value = cells.get(i) == null ? "" : cells.get(i).toString();
            if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0
                    && !value.matches("[+-]?\\d+(\\.\\d+)?([Ee][+-]?\\d+)?")) {
                value = "'" + value;
            }
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.write("\r\n");
    }
}
