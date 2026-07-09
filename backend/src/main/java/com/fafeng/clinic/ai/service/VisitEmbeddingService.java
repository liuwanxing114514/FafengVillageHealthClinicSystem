package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.config.ClinicEmbeddingProperties;
import com.fafeng.clinic.ai.mapper.VisitEmbeddingMapper;
import com.fafeng.clinic.ai.vo.VisitEmbeddingStatusVO;
import com.fafeng.clinic.ai.vo.VisitEmbeddingSyncResultVO;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 脱敏病历向量化同步（v2.2）：全量/增量写入 visit_embedding。
 */
@Service
@RequiredArgsConstructor
public class VisitEmbeddingService {

    private final ClinicEmbeddingProperties embeddingProperties;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final VisitEmbeddingMapper visitEmbeddingMapper;
    private final VisitEmbeddingTextBuilder textBuilder;
    private final PatientService patientService;

    public VisitEmbeddingStatusVO getStatus() {
        return new VisitEmbeddingStatusVO(
                embeddingProperties.isEnabled(),
                embeddingProperties.getProvider(),
                embeddingProperties.getModel(),
                embeddingProperties.getDimensions(),
                embeddingProperties.isConfigured() && embeddingModelProvider.getIfAvailable() != null,
                visitEmbeddingMapper.countActiveVisits(),
                visitEmbeddingMapper.countSyncedActiveVisits(),
                visitEmbeddingMapper.countPendingSync(),
                visitEmbeddingMapper.findLatestSyncedAt());
    }

    @Transactional
    public VisitEmbeddingSyncResultVO syncFull() {
        return syncInternal("full", visitEmbeddingMapper.listActiveVisitsForFullSync());
    }

    @Transactional
    public VisitEmbeddingSyncResultVO syncIncremental() {
        visitEmbeddingMapper.deleteVoidVisitEmbeddings();
        return syncInternal("incremental", visitEmbeddingMapper.listActiveVisitsForIncrementalSync());
    }

    private VisitEmbeddingSyncResultVO syncInternal(String mode, List<ClinicVisit> visits) {
        long start = System.currentTimeMillis();
        EmbeddingModel embeddingModel = requireEmbeddingModel();
        long synced = 0;
        long skipped = 0;
        long failed = 0;

        for (ClinicVisit visit : visits) {
            try {
                if (syncOne(embeddingModel, visit)) {
                    synced++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException ex) {
                failed++;
            }
        }

        if ("incremental".equals(mode)) {
            visitEmbeddingMapper.deleteVoidVisitEmbeddings();
        }

        return new VisitEmbeddingSyncResultVO(
                mode,
                visits.size(),
                synced,
                skipped,
                failed,
                System.currentTimeMillis() - start);
    }

    private boolean syncOne(EmbeddingModel embeddingModel, ClinicVisit visit) {
        Patient patient = null;
        if (visit.getPatientId() != null) {
            try {
                patient = patientService.requirePatient(visit.getPatientId());
            } catch (BusinessException ex) {
                patient = null;
            }
        }
        String summary = textBuilder.buildDesensitizedSummary(visit, patient);
        if (summary == null || summary.isBlank()) {
            return false;
        }

        float[] vector = embeddingModel.embed(summary);
        validateDimensions(vector);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime sourceUpdatedAt = visit.getUpdatedAt() == null ? now : visit.getUpdatedAt();
        visitEmbeddingMapper.upsertEmbedding(
                visit.getId(),
                toPgVectorLiteral(vector),
                summary,
                embeddingProperties.getModel(),
                embeddingProperties.getDimensions(),
                sourceUpdatedAt,
                now,
                now);
        return true;
    }

    private void validateDimensions(float[] vector) {
        if (vector == null || vector.length != embeddingProperties.getDimensions()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "向量维度与配置不一致，期望 " + embeddingProperties.getDimensions()
                            + "，实际 " + (vector == null ? 0 : vector.length));
        }
    }

    private EmbeddingModel requireEmbeddingModel() {
        if (!embeddingProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "病历向量化未启用");
        }
        if (!embeddingProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Embedding 配置不完整，请检查 .env");
        }
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Embedding 模型未装配，请检查 provider 与 API 配置");
        }
        return model;
    }

    static String toPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
