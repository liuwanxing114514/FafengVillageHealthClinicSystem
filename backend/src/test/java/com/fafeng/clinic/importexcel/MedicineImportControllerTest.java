package com.fafeng.clinic.importexcel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MedicineImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void templatePreviewAndConfirm() throws Exception {
        mockMvc.perform(get("/api/import/medicine/template"))
                .andExpect(status().isOk());

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String barcode = "6908888" + suffix.replace("-", "").substring(0, 6);
        byte[] workbook = buildWorkbook(new String[][]{
                MedicineImportExcelParser.TEMPLATE_HEADERS.toArray(String[]::new),
                {
                        "Excel导入药" + suffix, "通用名", "片剂", "100片", "片", "盒", "24",
                        "测试药厂", barcode, "6.5", "120", "B-EX-" + suffix, "2027-12-31", "48", "导入测试"
                }
        });

        String previewResponse = mockMvc.perform(multipart("/api/import/medicine/preview")
                        .file(new MockMultipartFile(
                                "file",
                                "import.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                workbook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.canConfirm").value(true))
                .andExpect(jsonPath("$.data.rows", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String previewId = objectMapper.readTree(previewResponse).path("data").path("previewId").asText();

        mockMvc.perform(post("/api/import/medicine/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"previewId\":\"" + previewId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.medicineCount").value(1))
                .andExpect(jsonPath("$.data.inventoryCount").value(1));

        mockMvc.perform(get("/api/medicines/by-barcode/" + barcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Excel导入药" + suffix));
    }

    @Test
    @WithMockUser(username = "admin")
    void previewMarksDuplicateBarcodeInFile() throws Exception {
        byte[] workbook = buildWorkbook(new String[][]{
                MedicineImportExcelParser.TEMPLATE_HEADERS.toArray(String[]::new),
                {
                        "重复A", "", "", "", "片", "盒", "24", "", "6907777000001", "1", "", "B1", "", "10", ""
                },
                {
                        "重复B", "", "", "", "片", "盒", "24", "", "6907777000001", "1", "", "B2", "", "10", ""
                }
        });

        mockMvc.perform(multipart("/api/import/medicine/preview")
                        .file(new MockMultipartFile(
                                "file",
                                "import.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                workbook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canConfirm").value(false))
                .andExpect(jsonPath("$.data.errorCount").value(1));
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
