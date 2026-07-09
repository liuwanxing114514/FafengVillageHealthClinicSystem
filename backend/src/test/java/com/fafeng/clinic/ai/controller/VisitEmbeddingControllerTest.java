package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.service.VisitEmbeddingService;
import com.fafeng.clinic.ai.service.VisitSimilaritySearchService;
import com.fafeng.clinic.ai.vo.SimilarVisitSearchResultVO;
import com.fafeng.clinic.ai.vo.SimilarVisitVO;
import com.fafeng.clinic.ai.vo.VisitEmbeddingStatusVO;
import com.fafeng.clinic.ai.vo.VisitEmbeddingSyncResultVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class VisitEmbeddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitEmbeddingService visitEmbeddingService;

    @MockBean
    private VisitSimilaritySearchService visitSimilaritySearchService;

    @Test
    @WithMockUser(username = "admin")
    void embeddingStatus() throws Exception {
        when(visitEmbeddingService.getStatus()).thenReturn(new VisitEmbeddingStatusVO(
                false, "openai", "BAAI/bge-m3", 1024, false, 10, 5, 3, null));

        mockMvc.perform(get("/api/ai/embeddings/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.pendingCount").value(3));
    }

    @Test
    @WithMockUser(username = "admin")
    void syncIncrementalWhenDisabled() throws Exception {
        when(visitEmbeddingService.syncIncremental())
                .thenThrow(new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "病历向量化未启用"));

        mockMvc.perform(post("/api/ai/embeddings/sync-incremental"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(503));
    }

    @Test
    @WithMockUser(username = "admin")
    void syncFullSuccess() throws Exception {
        when(visitEmbeddingService.syncFull()).thenReturn(new VisitEmbeddingSyncResultVO(
                "full", 2, 2, 0, 0, 120));

        mockMvc.perform(post("/api/ai/embeddings/sync-full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("full"))
                .andExpect(jsonPath("$.data.synced").value(2));
    }

    @Test
    @WithMockUser(username = "admin")
    void searchSimilarReturnsTopItems() throws Exception {
        when(visitSimilaritySearchService.search(any())).thenReturn(new SimilarVisitSearchResultVO(
                true,
                List.of(new SimilarVisitVO(
                        12L,
                        "主诉：咳嗽\n诊断：感冒",
                        0.91,
                        OffsetDateTime.parse("2025-06-01T08:00:00Z")))));

        mockMvc.perform(post("/api/ai/embeddings/search-similar")
                        .contentType("application/json")
                        .content("""
                                {
                                  "chiefComplaint": "咳嗽",
                                  "presentIllness": "3天",
                                  "diagnosis": "感冒",
                                  "patientId": 1,
                                  "excludeVisitId": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.items[0].visitId").value(12))
                .andExpect(jsonPath("$.data.items[0].similarity").value(0.91));
    }
}
