package com.fafeng.clinic.importexcel;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.importexcel.model.MedicineImportParsedRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MedicineImportExcelParser {

    static final List<String> TEMPLATE_HEADERS = List.of(
            "药品名称", "通用名", "剂型", "规格", "基本单位", "包装单位", "换算关系",
            "生产厂家", "条码", "进货单价", "库存下限", "批号", "有效期", "初始库存数量", "备注"
    );

    private static final Pattern CONVERSION_PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s*([^=\\d\\s]+?)\\s*=\\s*(\\d+)\\s*([^=\\d\\s]+?)\\s*$");
    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d")
    );

    public byte[] buildTemplateWorkbook() {
        try (Workbook workbook = WorkbookFactory.create(true);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("药品导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                header.createCell(i).setCellValue(TEMPLATE_HEADERS.get(i));
            }
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("阿莫西林胶囊");
            example.createCell(1).setCellValue("阿莫西林");
            example.createCell(2).setCellValue("胶囊剂");
            example.createCell(3).setCellValue("0.25g×24粒");
            example.createCell(4).setCellValue("粒");
            example.createCell(5).setCellValue("盒");
            example.createCell(6).setCellValue("1盒=24粒");
            example.createCell(7).setCellValue("示例药厂");
            example.createCell(8).setCellValue("6901234567890");
            example.createCell(9).setCellValue("12.5");
            example.createCell(10).setCellValue("120");
            example.createCell(11).setCellValue("B20260101");
            example.createCell(12).setCellValue("2027-12-31");
            example.createCell(13).setCellValue("48");
            example.createCell(14).setCellValue("示例行，导入前请删除");
            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成模板失败");
        }
    }

    public List<MedicineImportParsedRow> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel 文件为空");
            }
            validateHeaders(sheet.getRow(0));

            List<MedicineImportParsedRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (isBlankRow(row)) {
                    continue;
                }
                MedicineImportParsedRow parsed = parseRow(row, i + 1);
                rows.add(parsed);
            }
            if (rows.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到可导入的数据行");
            }
            return rows;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析 Excel 文件，请使用系统模板");
        }
    }

    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少表头行，请使用系统提供的模板");
        }
        for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
            String expected = TEMPLATE_HEADERS.get(i);
            String actual = cellText(headerRow.getCell(i)).trim();
            if (!expected.equals(actual)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "表头第 " + (i + 1) + " 列应为「" + expected + "」，实际为「" + actual + "」");
            }
        }
    }

    private MedicineImportParsedRow parseRow(Row row, int rowNumber) {
        MedicineImportParsedRow parsed = new MedicineImportParsedRow();
        parsed.setRowNumber(rowNumber);
        parsed.setName(cellText(row.getCell(0)).trim());
        parsed.setGenericName(cellText(row.getCell(1)).trim());
        parsed.setDosageForm(cellText(row.getCell(2)).trim());
        parsed.setSpecification(cellText(row.getCell(3)).trim());
        parsed.setBaseUnit(cellText(row.getCell(4)).trim());
        parsed.setPackageUnit(cellText(row.getCell(5)).trim());
        parsed.setConversionText(cellText(row.getCell(6)).trim());
        parsed.setManufacturer(cellText(row.getCell(7)).trim());
        parsed.setBarcode(cellText(row.getCell(8)).trim());
        parsed.setPurchasePrice(parseDecimal(cellText(row.getCell(9)).trim(), null));
        parsed.setStockThreshold(parseDecimal(cellText(row.getCell(10)).trim(), null));
        parsed.setBatchNo(cellText(row.getCell(11)).trim());
        String expiryText = cellText(row.getCell(12)).trim();
        parsed.setExpiryProvided(!expiryText.isEmpty());
        parsed.setExpiryDate(parseDate(row.getCell(12)));
        parsed.setInitialStock(parseDecimal(cellText(row.getCell(13)).trim(), BigDecimal.ZERO));
        parsed.setRemark(cellText(row.getCell(14)).trim());
        parseConversion(parsed);
        return parsed;
    }

    private void parseConversion(MedicineImportParsedRow row) {
        String text = row.getConversionText();
        if (text.isEmpty()) {
            return;
        }
        Matcher matcher = CONVERSION_PATTERN.matcher(text);
        if (matcher.matches()) {
            int fromQuantity = Integer.parseInt(matcher.group(1));
            int toQuantity = Integer.parseInt(matcher.group(3));
            if (fromQuantity <= 0 || toQuantity <= 0) {
                row.addError("换算关系数量必须大于 0");
                return;
            }
            if (toQuantity % fromQuantity != 0) {
                row.addError("换算关系无法整除，示例：1盒=24粒 或 2盒=48粒");
                return;
            }
            row.setConversionFactor(toQuantity / fromQuantity);
            return;
        }
        if (text.matches("\\d+")) {
            row.setConversionFactor(Integer.parseInt(text));
            return;
        }
        row.addError("换算关系格式无效，示例：1盒=24粒 或 24");
    }

    private BigDecimal parseDecimal(String text, BigDecimal defaultValue) {
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = cellText(cell).trim();
        if (text.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
            if (!cellText(row.getCell(i)).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell);
    }
}
