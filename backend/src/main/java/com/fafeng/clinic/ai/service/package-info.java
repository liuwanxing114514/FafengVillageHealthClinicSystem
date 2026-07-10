/**
 * AI 业务服务：草稿、RAG、OCR 入库、快捷语、通道与外部服务管理等。
 *
 * <p>典型链路：
 * <ul>
 *   <li>病历整理 — {@link com.fafeng.clinic.ai.service.AiVisitStructureService} → {@code ai_draft}</li>
 *   <li>RAG — {@link com.fafeng.clinic.ai.service.VisitEmbeddingService} + {@link com.fafeng.clinic.ai.service.VisitSimilaritySearchService}</li>
 *   <li>设置页 — {@link com.fafeng.clinic.ai.service.AiChannelService} + {@link com.fafeng.clinic.ai.service.ExternalServiceService}</li>
 * </ul>
 */
package com.fafeng.clinic.ai.service;
