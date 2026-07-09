package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.config.ClinicEmbeddingProperties;
import com.fafeng.clinic.ai.dto.SimilarVisitSearchRequest;
import com.fafeng.clinic.ai.mapper.VisitEmbeddingMapper;
import com.fafeng.clinic.ai.model.SimilarVisitMatchRow;
import com.fafeng.clinic.ai.vo.SimilarVisitSearchResultVO;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitSimilaritySearchServiceTest {

    @Mock
    private ClinicEmbeddingProperties embeddingProperties;
    @Mock
    private ObjectProvider<EmbeddingModel> embeddingModelProvider;
    @Mock
    private VisitEmbeddingMapper visitEmbeddingMapper;
    @Mock
    private VisitEmbeddingTextBuilder textBuilder;
    @Mock
    private PatientService patientService;
    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private VisitSimilaritySearchService service;

    @Test
    void search_returnsUnavailableWhenEmbeddingDisabled() {
        when(embeddingProperties.isEnabled()).thenReturn(false);

        SimilarVisitSearchResultVO result = service.search(sampleRequest());

        assertFalse(result.available());
        assertTrue(result.items().isEmpty());
        verify(embeddingModelProvider, never()).getIfAvailable();
    }

    @Test
    void search_returnsEmptyWhenQueryBlank() {
        when(embeddingProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.isConfigured()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(textBuilder.buildDesensitizedSearchQuery(any(), any(), any(), any())).thenReturn("");

        SimilarVisitSearchResultVO result = service.search(sampleRequest());

        assertTrue(result.available());
        assertTrue(result.items().isEmpty());
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    void search_returnsTopMatchesWhenConfigured() {
        when(embeddingProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.isConfigured()).thenReturn(true);
        when(embeddingProperties.getDimensions()).thenReturn(3);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(patientService.requirePatient(1L)).thenReturn(new Patient());
        when(textBuilder.buildDesensitizedSearchQuery(any(), any(), any(), any())).thenReturn("主诉：咳嗽");
        when(embeddingModel.embed("主诉：咳嗽")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        SimilarVisitMatchRow row = new SimilarVisitMatchRow();
        row.setVisitId(9L);
        row.setTextSummary("主诉：咳嗽\n诊断：感冒");
        row.setSimilarity(0.88);
        row.setVisitTime(OffsetDateTime.parse("2025-01-02T10:00:00Z"));
        when(visitEmbeddingMapper.searchSimilar(anyString(), eq(5L), eq(3))).thenReturn(List.of(row));

        SimilarVisitSearchRequest request = new SimilarVisitSearchRequest(
                "咳嗽", "", "感冒", 1L, 5L);
        SimilarVisitSearchResultVO result = service.search(request);

        assertTrue(result.available());
        assertEquals(1, result.items().size());
        assertEquals(9L, result.items().getFirst().visitId());
        assertEquals(0.88, result.items().getFirst().similarity());
    }

    @Test
    void search_swallowsEmbeddingFailure() {
        when(embeddingProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.isConfigured()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(textBuilder.buildDesensitizedSearchQuery(any(), any(), any(), any())).thenReturn("主诉：发热");
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("api down"));

        SimilarVisitSearchResultVO result = service.search(sampleRequest());

        assertTrue(result.available());
        assertTrue(result.items().isEmpty());
        verify(visitEmbeddingMapper, never()).searchSimilar(anyString(), anyLong(), anyInt());
    }

    private SimilarVisitSearchRequest sampleRequest() {
        return new SimilarVisitSearchRequest("咳嗽", "3天", "感冒", null, null);
    }
}
