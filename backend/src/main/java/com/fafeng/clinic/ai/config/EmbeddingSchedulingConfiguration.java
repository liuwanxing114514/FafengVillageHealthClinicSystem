package com.fafeng.clinic.ai.config;

import com.fafeng.clinic.ai.service.VisitEmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 可选定时增量同步（{@code clinic.ai.embedding.sync-cron} 非空时启用）。
 */
@Configuration
@EnableScheduling
@ConditionalOnExpression("!'${clinic.ai.embedding.sync-cron:}'.trim().isEmpty()")
public class EmbeddingSchedulingConfiguration {

    private final VisitEmbeddingService visitEmbeddingService;

    public EmbeddingSchedulingConfiguration(VisitEmbeddingService visitEmbeddingService) {
        this.visitEmbeddingService = visitEmbeddingService;
    }

    @Scheduled(cron = "${clinic.ai.embedding.sync-cron}")
    public void scheduledIncrementalSync() {
        visitEmbeddingService.syncIncremental();
    }
}
