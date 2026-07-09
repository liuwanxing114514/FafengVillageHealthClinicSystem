package com.fafeng.clinic.clinic.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void createPrintAndOutboundDraft() throws Exception {
        long patientId = createPatient();
        long visitId = createVisit(patientId);
        long medicineId = createMedicine();

        String prescriptionBody = """
                {
                  "patientId": %d,
                  "visitId": %d,
                  "prescriptionDate": "2026-07-08",
                  "diagnosis": "上呼吸道感染",
                  "items": [
                    {
                      "medicineId": %d,
                      "quantity": 24,
                      "unit": "粒",
                      "usage": "口服，每日三次"
                    }
                  ]
                }
                """.formatted(patientId, visitId, medicineId);

        String createResponse = mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prescriptionBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.diagnosis").value("上呼吸道感染"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long prescriptionId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/prescriptions/" + prescriptionId + "/print"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("发凤村卫生室处方签"))
                .andExpect(jsonPath("$.data.patientName").exists())
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        mockMvc.perform(post("/api/prescriptions/" + prescriptionId + "/outbound-draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.draftType").value("OUTBOUND"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.payload").exists());
    }

    private long createPatient() throws Exception {
        String response = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "处方测试患者",
                                  "gender": "M",
                                  "birthDate": "1990-01-01",
                                  "phone": "13800000001",
                                  "address": "发凤村"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createVisit(long patientId) throws Exception {
        String response = mockMvc.perform(post("/api/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %d,
                                  "chiefComplaint": "咳嗽",
                                  "diagnosis": "上呼吸道感染"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createMedicine() throws Exception {
        String response = mockMvc.perform(post("/api/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试胶囊",
                                  "dosageForm": "胶囊剂",
                                  "specification": "0.25g×12粒",
                                  "baseUnit": "粒",
                                  "packageUnit": "盒",
                                  "purchasePrice": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long medicineId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(post("/api/medicines/" + medicineId + "/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromUnit":"盒","toUnit":"粒","factor":12}
                                """))
                .andExpect(status().isOk());

        return medicineId;
    }
}
