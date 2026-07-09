package com.fafeng.clinic.patient.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void createSearchAndUpdatePatient() throws Exception {
        String createBody = """
                {
                  "name": "单元测试李四",
                  "gender": "F",
                  "birthDate": "1988-05-20",
                  "phone": "13999990001",
                  "address": "发凤村一组"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("单元测试李四"))
                .andExpect(jsonPath("$.data.ageManual").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/patients?keyword=单元测试李四"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.name == '单元测试李四')]", hasSize(1)));

        mockMvc.perform(put("/api/patients/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "单元测试李四",
                                  "gender": "F",
                                  "birthDate": "1988-05-20",
                                  "phone": "13999990002",
                                  "address": "发凤村二组"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13999990002"));
    }

    @Test
    @WithMockUser(username = "admin")
    void createVisitForPatient() throws Exception {
        String patientResponse = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "王五",
                                  "gender": "M",
                                  "age": 45,
                                  "phone": "13700003333"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long patientId = objectMapper.readTree(patientResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %d,
                                  "chiefComplaint": "头痛3天",
                                  "diagnosis": "感冒"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientName").value("王五"))
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛3天"));

        mockMvc.perform(get("/api/patients/" + patientId + "/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "admin")
    void searchByAddressAndDeletePatient() throws Exception {
        String createResponse = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "赵六",
                                  "gender": "M",
                                  "age": 30,
                                  "phone": "13600004444",
                                  "address": "发凤村三组",
                                  "remark": "老村民"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/patients?address=三组&remark=老村"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)));

        mockMvc.perform(delete("/api/patients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/patients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(username = "admin")
    void deleteVisit() throws Exception {
        String patientResponse = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "孙七",
                                  "gender": "F",
                                  "age": 28
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long patientId = objectMapper.readTree(patientResponse).path("data").path("id").asLong();

        String visitResponse = mockMvc.perform(post("/api/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %d,
                                  "diagnosis": "待删除"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long visitId = objectMapper.readTree(visitResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/visits/" + visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/patients/" + patientId + "/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void listRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }
}
