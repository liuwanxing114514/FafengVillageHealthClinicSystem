package com.fafeng.clinic.inventory.export;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.inventory.vo.FlowVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class InventoryFlowExcelExporter {

    private static final List<String> HEADERS = List.of(
            "时间", "药品", "批号", "类型", "变动", "结余", "操作人");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(List<FlowVO> flows) {
        try (Workbook workbook = WorkbookFactory.create(true);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("库存流水");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                headerRow.createCell(i).setCellValue(HEADERS.get(i));
            }
            int rowIndex = 1;
            for (FlowVO flow : flows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(formatDateTime(flow.createdAt()));
                row.createCell(1).setCellValue(nullToDash(flow.medicineName()));
                row.createCell(2).setCellValue(nullToDash(flow.batchNo()));
                row.createCell(3).setCellValue(flowTypeLabel(flow.flowType()));
                row.createCell(4).setCellValue(formatQuantity(flow.quantityChange(), flow.unit()));
                row.createCell(5).setCellValue(formatQuantity(flow.quantityAfter(), flow.unit()));
                row.createCell(6).setCellValue(nullToDash(flow.operator()));
            }
            for (int i = 0; i < HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成 Excel 失败");
        }
    }

    private String formatDateTime(OffsetDateTime value) {
        if (value == null) {
            return "—";
        }
        return value.format(DATE_TIME_FORMATTER);
    }

    private String formatQuantity(BigDecimal quantity, String unit) {
        if (quantity == null) {
            return "—";
        }
        String safeUnit = unit == null || unit.isBlank() ? "" : " " + unit.trim();
        return quantity.stripTrailingZeros().toPlainString() + safeUnit;
    }

    private String flowTypeLabel(String flowType) {
        if (flowType == null) {
            return "—";
        }
        return switch (flowType) {
            case "INBOUND" -> "入库";
            case "OUTBOUND" -> "出库";
            case "ADJUST" -> "盘点";
            default -> flowType;
        };
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
