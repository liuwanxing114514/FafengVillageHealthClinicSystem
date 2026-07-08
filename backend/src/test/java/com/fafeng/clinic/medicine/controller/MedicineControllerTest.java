package com.fafeng.clinic.medicine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MedicineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void createSearchAndUpdateMedicine() throws Exception {
        String createBody = """
                {
                  "name": "阿莫西林胶囊",
                  "genericName": "阿莫西林",
                  "dosageForm": "胶囊剂",
                  "specification": "0.25g×24粒",
                  "baseUnit": "粒",
                  "packageUnit": "盒",
                  "manufacturer": "测试药厂",
                  "purchasePrice": 12.50
                }
                """;

        String createResponse = mockMvc.perform(post("/api/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("阿莫西林胶囊"))
                .andExpect(jsonPath("$.data.pinyinAbbr").value("amxljn"))
                .andExpect(jsonPath("$.data.stockThreshold").value(5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/medicines/" + id + "/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromUnit":"盒","toUnit":"粒","factor":24}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.factor").value(24));

        mockMvc.perform(post("/api/medicines/" + id + "/barcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"barcode":"6901234567890","remark":"主条码"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.barcode").value("6901234567890"));

        mockMvc.perform(get("/api/medicines")
                        .param("keyword", "amxljn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.name == '阿莫西林胶囊')].name",
                        hasItem("阿莫西林胶囊")));

        mockMvc.perform(get("/api/medicines")
                        .param("keyword", "6901234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.barcodes[0] == '6901234567890')].barcodes[0]",
                        hasItem("6901234567890")));

        mockMvc.perform(patch("/api/medicines/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(delete("/api/medicines/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/medicines")
                        .param("keyword", "amxljn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.name == '阿莫西林胶囊')]").isEmpty());

        mockMvc.perform(get("/api/medicines/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(username = "admin")
    void deleteRequiresInactiveStatus() throws Exception {
        String createResponse = mockMvc.perform(post("/api/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试删除药品",
                                  "baseUnit": "片",
                                  "packageUnit": "盒",
                                  "purchasePrice": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/medicines/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请先停用药品后再删除"));

        mockMvc.perform(patch("/api/medicines/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/medicines/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/medicines"))
                .andExpect(status().isUnauthorized());
    }
}
