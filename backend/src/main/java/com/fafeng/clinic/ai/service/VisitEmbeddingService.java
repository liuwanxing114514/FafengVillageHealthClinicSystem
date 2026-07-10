package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.channel.EmbeddingChannelConfig;
import com.fafeng.clinic.ai.client.ResilientEmbeddingModel;
import com.fafeng.clinic.ai.config.ClinicEmbeddingProperties;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
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

    private final ExternalServiceConfigService externalServiceConfigService;
    private final ClinicEmbeddingProperties embeddingProperties;
    private final ResilientEmbeddingModel resilientEmbeddingModel;
    private final ChannelRegistry channelRegistry;
    private final VisitEmbeddingMapper visitEmbeddingMapper;
    private final VisitEmbeddingTextBuilder textBuilder;
    private final PatientService patientService;

    public VisitEmbeddingStatusVO getStatus() {
        EmbeddingChannelConfig primary = channelRegistry.primaryEmbeddingConfig();
        String provider = embeddingProperties.getProvider();
        String model = primary != null ? primary.model() : embeddingProperties.getModel();
        int dimensions = primary != null ? primary.dimensions() : embeddingProperties.getDimensions();
        return new VisitEmbeddingStatusVO(
                externalServiceConfigService.isEmbeddingEnabled(),
                provider,
                model,
                dimensions,
                resilientEmbeddingModel.isConfigured(),
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
        int expectedDimensions = resolveDimensions();
        long synced = 0;
        long skipped = 0;
        long failed = 0;

        for (ClinicVisit visit : visits) {
            try {
                if (syncOne(embeddingModel, visit, expectedDimensions)) {
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

    private boolean syncOne(EmbeddingModel embeddingModel, ClinicVisit visit, int expectedDimensions) {
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
        validateDimensions(vector, expectedDimensions);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime sourceUpdatedAt = visit.getUpdatedAt() == null ? now : visit.getUpdatedAt();
        EmbeddingChannelConfig primary = channelRegistry.primaryEmbeddingConfig();
        String modelName = primary != null ? primary.model() : embeddingProperties.getModel();
        visitEmbeddingMapper.upsertEmbedding(
                visit.getId(),
                toPgVectorLiteral(vector),
                summary,
                modelName,
                expectedDimensions,
                sourceUpdatedAt,
                now,
                now);
        return true;
    }

    static void validateDimensions(float[] vector, int expectedDimensions) {
        if (vector == null || vector.length != expectedDimensions) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "向量维度与配置不一致，期望 " + expectedDimensions
                            + "，实际 " + (vector == null ? 0 : vector.length));
        }
    }

    private EmbeddingModel requireEmbeddingModel() {
        if (!externalServiceConfigService.isEmbeddingEnabled()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "病历向量化未启用");
        }
        if (!resilientEmbeddingModel.isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Embedding 配置不完整，请在系统设置中配置");
        }
        return resilientEmbeddingModel;
    }

    private int resolveDimensions() {
        EmbeddingChannelConfig primary = channelRegistry.primaryEmbeddingConfig();
        return primary != null ? primary.dimensions() : embeddingProperties.getDimensions();
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
