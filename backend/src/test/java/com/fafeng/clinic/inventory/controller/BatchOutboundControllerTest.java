package com.fafeng.clinic.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BatchOutboundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void batchOutboundPreviewAndConfirm() throws Exception {
        long medicineId1 = createMedicine("批量出库药A");
        long medicineId2 = createMedicine("批量出库药B");

        inbound(medicineId1, "B-A1", 100);
        inbound(medicineId2, "B-B1", 50);

        String previewResponse = mockMvc.perform(post("/api/inventory/outbound/batch/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"medicineId": %d, "quantity": 10, "unit": "片"},
                                    {"medicineId": %d, "quantity": 5, "unit": "片"}
                                  ]
                                }
                                """.formatted(medicineId1, medicineId2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sufficient").value(true))
                .andExpect(jsonPath("$.data.lines", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long batchId1 = objectMapper.readTree(previewResponse)
                .path("data").path("lines").get(0)
                .path("recommendedAllocations").get(0)
                .path("batchId").asLong();
        long batchId2 = objectMapper.readTree(previewResponse)
                .path("data").path("lines").get(1)
                .path("recommendedAllocations").get(0)
                .path("batchId").asLong();

        mockMvc.perform(post("/api/inventory/outbound/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "盘点损耗",
                                  "lines": [
                                    {
                                      "medicineId": %d,
                                      "quantity": 10,
                                      "unit": "片",
                                      "allocations": [{"batchId": %d, "quantity": 10}]
                                    },
                                    {
                                      "medicineId": %d,
                                      "quantity": 5,
                                      "unit": "片",
                                      "allocations": [{"batchId": %d, "quantity": 5}]
                                    }
                                  ]
                                }
                                """.formatted(medicineId1, batchId1, medicineId2, batchId2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineCount").value(2))
                .andExpect(jsonPath("$.data.flowCount").value(2))
                .andExpect(jsonPath("$.data.flows", hasSize(2)))
                .andExpect(jsonPath("$.data.flows[0].reason").value("盘点损耗"))
                .andExpect(jsonPath("$.data.flows[0].flowType").value("OUTBOUND"));
    }

    @Test
    @WithMockUser(username = "admin")
    void batchOutboundBlockedWhenInsufficient() throws Exception {
        long medicineId = createMedicine("库存不足药");

        mockMvc.perform(post("/api/inventory/outbound/batch/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [{"medicineId": %d, "quantity": 10, "unit": "片"}]
                                }
                                """.formatted(medicineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sufficient").value(false));

        mockMvc.perform(post("/api/inventory/outbound/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "盘点损耗",
                                  "lines": [
                                    {
                                      "medicineId": %d,
                                      "quantity": 10,
                                      "unit": "片",
                                      "allocations": [{"batchId": 1, "quantity": 10}]
                                    }
                                  ]
                                }
                                """.formatted(medicineId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin")
    void batchOutboundRejectsDuplicateMedicine() throws Exception {
        long medicineId = createMedicine("重复药品");

        mockMvc.perform(post("/api/inventory/outbound/batch/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"medicineId": %d, "quantity": 1, "unit": "片"},
                                    {"medicineId": %d, "quantity": 2, "unit": "片"}
                                  ]
                                }
                                """.formatted(medicineId, medicineId)))
                .andExpect(status().isBadRequest());
    }

    private long createMedicine(String name) throws Exception {
        String response = mockMvc.perform(post("/api/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "dosageForm": "片剂",
                                  "specification": "100片",
                                  "baseUnit": "片",
                                  "packageUnit": "瓶",
                                  "purchasePrice": 8,
                                  "stockThreshold": 50
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void inbound(long medicineId, String batchNo, int quantity) throws Exception {
        mockMvc.perform(post("/api/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicineId": %d,
                                  "quantity": %d,
                                  "unit": "片",
                                  "batchNo": "%s",
                                  "expiryDate": "2027-12-31",
                                  "purchasePrice": 0.5,
                                  "supplier": "测试供应商"
                                }
                                """.formatted(medicineId, quantity, batchNo)))
                .andExpect(status().isOk());
    }
}
