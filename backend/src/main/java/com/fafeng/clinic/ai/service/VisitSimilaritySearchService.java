package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.config.ClinicEmbeddingProperties;
import com.fafeng.clinic.ai.dto.SimilarVisitSearchRequest;
import com.fafeng.clinic.ai.mapper.VisitEmbeddingMapper;
import com.fafeng.clinic.ai.model.SimilarVisitMatchRow;
import com.fafeng.clinic.ai.vo.SimilarVisitSearchResultVO;
import com.fafeng.clinic.ai.vo.SimilarVisitVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 相似病例向量检索（v2.3）：脱敏查询 → embed → pgvector 余弦 Top-K。
 * 失败或未启用时返回空结果，不抛异常阻塞病历录入。
 */
@Service
@RequiredArgsConstructor
public class VisitSimilaritySearchService {

    private static final Logger log = LoggerFactory.getLogger(VisitSimilaritySearchService.class);
    private static final int TOP_K = 3;

    private final ClinicEmbeddingProperties embeddingProperties;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final VisitEmbeddingMapper visitEmbeddingMapper;
    private final VisitEmbeddingTextBuilder textBuilder;
    private final PatientService patientService;

    public SimilarVisitSearchResultVO search(SimilarVisitSearchRequest request) {
        if (!isSearchAvailable()) {
            return emptyResult(false);
        }
        Patient patient = resolvePatient(request.patientId());
        String queryText = textBuilder.buildDesensitizedSearchQuery(
                request.chiefComplaint(),
                request.presentIllness(),
                request.diagnosis(),
                patient);
        if (queryText.isBlank()) {
            return emptyResult(true);
        }
        try {
            EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
            if (embeddingModel == null) {
                return emptyResult(false);
            }
            float[] vector = embeddingModel.embed(queryText);
            VisitEmbeddingService.validateDimensions(vector, embeddingProperties.getDimensions());
            String vectorLiteral = VisitEmbeddingService.toPgVectorLiteral(vector);
            List<SimilarVisitMatchRow> rows = visitEmbeddingMapper.searchSimilar(
                    vectorLiteral,
                    request.excludeVisitId(),
                    TOP_K);
            List<SimilarVisitVO> items = rows.stream()
                    .map(this::toVo)
                    .toList();
            return new SimilarVisitSearchResultVO(true, items);
        } catch (RuntimeException ex) {
            log.warn("相似病例检索失败，返回空结果: {}", ex.getMessage());
            return emptyResult(true);
        }
    }

    private boolean isSearchAvailable() {
        return embeddingProperties.isEnabled()
                && embeddingProperties.isConfigured()
                && embeddingModelProvider.getIfAvailable() != null;
    }

    private Patient resolvePatient(Long patientId) {
        if (patientId == null) {
            return null;
        }
        try {
            return patientService.requirePatient(patientId);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private SimilarVisitVO toVo(SimilarVisitMatchRow row) {
        double similarity = row.getSimilarity() == null ? 0.0 : row.getSimilarity();
        return new SimilarVisitVO(
                row.getVisitId(),
                row.getTextSummary(),
                similarity,
                row.getVisitTime());
    }

    private SimilarVisitSearchResultVO emptyResult(boolean available) {
        return new SimilarVisitSearchResultVO(available, List.of());
    }
}
