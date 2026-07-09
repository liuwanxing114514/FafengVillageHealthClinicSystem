package com.fafeng.clinic.importexcel;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.importexcel.model.MedicineImportParsedRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedicineImportExcelParserTest {

    private final MedicineImportExcelParser parser = new MedicineImportExcelParser();

    @Test
    void parseValidRow() throws Exception {
        byte[] workbook = buildWorkbook(new String[][]{
                MedicineImportExcelParser.TEMPLATE_HEADERS.toArray(String[]::new),
                {
                        "测试导入药", "通用名", "片剂", "100片", "片", "盒", "24",
                        "药厂", "6909999888877", "8.5", "12.0", "120", "B001", "2027-06-01", "48", "备注"
                }
        });

        List<MedicineImportParsedRow> rows = parser.parse(new ByteArrayInputStream(workbook));
        assertEquals(1, rows.size());
        MedicineImportParsedRow row = rows.get(0);
        assertEquals("测试导入药", row.getName());
        assertEquals(Integer.valueOf(24), row.getConversionFactor());
        assertEquals(new BigDecimal("48"), row.getInitialStock());
    }

    @Test
    void rejectInvalidHeaders() throws Exception {
        byte[] workbook = buildWorkbook(new String[][]{
                {"名称", "通用名"},
                {"测试", "x"}
        });
        assertThrows(BusinessException.class, () -> parser.parse(new ByteArrayInputStream(workbook)));
    }

    @Test
    void parseConversionWithLeftQuantity() throws Exception {
        byte[] workbook = buildWorkbook(new String[][]{
                MedicineImportExcelParser.TEMPLATE_HEADERS.toArray(String[]::new),
                {
                        "换算测试药", "", "", "", "粒", "盒", "2盒=48粒",
                        "", "", "1", "1.5", "", "", "", "0", ""
                }
        });

        List<MedicineImportParsedRow> rows = parser.parse(new ByteArrayInputStream(workbook));
        assertEquals(Integer.valueOf(24), rows.get(0).getConversionFactor());
    }

    private byte[] buildWorkbook(String[][] data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("sheet1");
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < data[r].length; c++) {
                    row.createCell(c).setCellValue(data[r][c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
