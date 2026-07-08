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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void inboundOutboundPreviewAndAdjust() throws Exception {
        long medicineId = createMedicine();

        mockMvc.perform(post("/api/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicineId": %d,
                                  "quantity": 100,
                                  "unit": "片",
                                  "batchNo": "B20260708",
                                  "expiryDate": "2027-12-31",
                                  "purchasePrice": 0.5,
                                  "supplier": "测试供应商"
                                }
                                """.formatted(medicineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.flowType").value("INBOUND"));

        long patientId = createPatient();
        long visitId = createVisit(patientId);
        long prescriptionId = createPrescription(patientId, visitId, medicineId);

        String previewResponse = mockMvc.perform(post("/api/inventory/outbound/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %d,
                                  "prescriptionId": %d,
                                  "items": [{"medicineId": %d, "quantity": 10, "unit": "片"}]
                                }
                                """.formatted(patientId, prescriptionId, medicineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sufficient").value(true))
                .andExpect(jsonPath("$.data.lines[0].recommendedAllocations", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long batchId = objectMapper.readTree(previewResponse)
                .path("data").path("lines").get(0)
                .path("recommendedAllocations").get(0)
                .path("batchId").asLong();

        mockMvc.perform(post("/api/inventory/outbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %d,
                                  "prescriptionId": %d,
                                  "medicineId": %d,
                                  "allocations": [{"batchId": %d, "quantity": 10}]
                                }
                                """.formatted(patientId, prescriptionId, medicineId, batchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(post("/api/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicineId": %d,
                                  "batchId": %d,
                                  "quantityChange": -5,
                                  "unit": "片",
                                  "reason": "盘点损耗"
                                }
                                """.formatted(medicineId, batchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowType").value("ADJUST"));

        mockMvc.perform(get("/api/inventory/flows?medicineId=" + medicineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(username = "admin")
    void outboundBlockedWhenInsufficient() throws Exception {
        long medicineId = createMedicine();
        long patientId = createPatient();
        long visitId = createVisit(patientId);
        long prescriptionId = createPrescription(patientId, visitId, medicineId);

        mockMvc.perform(post("/api/inventory/outbound/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %d,
                                  "prescriptionId": %d,
                                  "items": [{"medicineId": %d, "quantity": 10, "unit": "片"}]
                                }
                                """.formatted(patientId, prescriptionId, medicineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sufficient").value(false));
    }

    private long createMedicine() throws Exception {
        String response = mockMvc.perform(post("/api/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "库存测试药",
                                  "dosageForm": "片剂",
                                  "specification": "100片",
                                  "baseUnit": "片",
                                  "packageUnit": "瓶",
                                  "purchasePrice": 8,
                                  "stockThreshold": 50
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createPatient() throws Exception {
        String response = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"库存患者","gender":"M","birthDate":"1990-01-01","phone":"13800000002","address":"发凤村"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createVisit(long patientId) throws Exception {
        String response = mockMvc.perform(post("/api/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"diagnosis":"测试"}
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createPrescription(long patientId, long visitId, long medicineId) throws Exception {
        String response = mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %d,
                                  "visitId": %d,
                                  "items": [{"medicineId": %d, "quantity": 10, "unit": "片"}]
                                }
                                """.formatted(patientId, visitId, medicineId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }
}
