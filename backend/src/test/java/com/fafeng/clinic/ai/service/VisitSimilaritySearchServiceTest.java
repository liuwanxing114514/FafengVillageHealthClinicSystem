package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.client.ResilientEmbeddingModel;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.channel.EmbeddingChannelConfig;
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
    private ExternalServiceConfigService externalServiceConfigService;
    @Mock
    private ResilientEmbeddingModel resilientEmbeddingModel;
    @Mock
    private ChannelRegistry channelRegistry;
    @Mock
    private VisitEmbeddingMapper visitEmbeddingMapper;
    @Mock
    private VisitEmbeddingTextBuilder textBuilder;
    @Mock
    private PatientService patientService;

    @InjectMocks
    private VisitSimilaritySearchService service;

    @Test
    void search_returnsUnavailableWhenEmbeddingDisabled() {
        when(externalServiceConfigService.isEmbeddingEnabled()).thenReturn(false);

        SimilarVisitSearchResultVO result = service.search(sampleRequest());

        assertFalse(result.available());
        assertTrue(result.items().isEmpty());
        verify(resilientEmbeddingModel, never()).embed(anyString());
    }

    @Test
    void search_returnsEmptyWhenQueryBlank() {
        when(externalServiceConfigService.isEmbeddingEnabled()).thenReturn(true);
        when(resilientEmbeddingModel.isConfigured()).thenReturn(true);
        when(textBuilder.buildDesensitizedSearchQuery(any(), any(), any(), any())).thenReturn("");

        SimilarVisitSearchResultVO result = service.search(sampleRequest());

        assertTrue(result.available());
        assertTrue(result.items().isEmpty());
        verify(resilientEmbeddingModel, never()).embed(anyString());
    }

    @Test
    void search_returnsTopMatchesWhenConfigured() {
        when(externalServiceConfigService.isEmbeddingEnabled()).thenReturn(true);
        when(resilientEmbeddingModel.isConfigured()).thenReturn(true);
        when(channelRegistry.primaryEmbeddingConfig()).thenReturn(
                new EmbeddingChannelConfig("main", "主", 1, true, "url", "key", "model", 3));
        when(patientService.requirePatient(1L)).thenReturn(new Patient());
        when(textBuilder.buildDesensitizedSearchQuery(any(), any(), any(), any())).thenReturn("主诉：咳嗽");
        when(resilientEmbeddingModel.embed("主诉：咳嗽")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

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
        when(externalServiceConfigService.isEmbeddingEnabled()).thenReturn(true);
        when(resilientEmbeddingModel.isConfigured()).thenReturn(true);
        when(textBuilder.buildDesensitizedSearchQuery(any(), any(), any(), any())).thenReturn("主诉：发热");
        when(resilientEmbeddingModel.embed(anyString())).thenThrow(new RuntimeException("api down"));

        SimilarVisitSearchResultVO result = service.search(sampleRequest());

        assertTrue(result.available());
        assertTrue(result.items().isEmpty());
        verify(visitEmbeddingMapper, never()).searchSimilar(anyString(), anyLong(), anyInt());
    }

    private SimilarVisitSearchRequest sampleRequest() {
        return new SimilarVisitSearchRequest("咳嗽", "3天", "感冒", null, null);
    }
}
