/**
 * AI 运行时配置：Spring {@code @ConfigurationProperties} +  DB 优先的聚合服务。
 *
 * <ul>
 *   <li>{@link com.fafeng.clinic.ai.config.ClinicAiProperties} — {@code CLINIC_AI_*}, {@code DEEPSEEK_*}</li>
 *   <li>{@link com.fafeng.clinic.ai.config.ClinicEmbeddingProperties} — {@code CLINIC_EMBEDDING_*}</li>
 *   <li>{@link com.fafeng.clinic.ai.config.ClinicVoiceProperties} / {@link com.fafeng.clinic.ai.config.ClinicOcrProperties} — Whisper/OCR URL</li>
 *   <li>{@link com.fafeng.clinic.ai.config.ExternalServiceConfigService} — 四类外部服务开关快照（DB 有行读 DB，否则 bootstrap env）</li>
 *   <li>{@link com.fafeng.clinic.ai.config.SecretEncryptor} — 设置页 API Key 的 AES-256-GCM，密钥来自 {@code CLINIC_SETTINGS_ENCRYPTION_KEY}</li>
 *   <li>{@link com.fafeng.clinic.ai.config.AiConfiguration} — 注册 {@link com.fafeng.clinic.ai.provider.AiProvider}  beans</li>
 * </ul>
 */
package com.fafeng.clinic.ai.config;
