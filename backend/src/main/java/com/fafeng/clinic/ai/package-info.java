/**
 * AI 能力包（对话、向量、语音、OCR、草稿、外部服务配置）。
 *
 * <h2>分层（由外到内）</h2>
 * <ul>
 *   <li>{@code controller} — HTTP API，Session 鉴权</li>
 *   <li>{@code service} — 业务编排：草稿审批、RAG、OCR 入库、通道 CRUD</li>
 *   <li>{@code client} — 对外部模型的统一门面（{@link com.fafeng.clinic.ai.client.ResilientAiChatClient} 等）</li>
 *   <li>{@code channel} — 多 API 通道加载、排序、failover</li>
 *   <li>{@code config} — env 绑定、DB 优先的运行时开关、密钥加密</li>
 *   <li>{@code provider} — 业务层 {@link com.fafeng.clinic.ai.provider.AiProvider} 抽象（noop/deepseek/local）</li>
 * </ul>
 *
 * <h2>配置优先级（v2.5+）</h2>
 * <pre>
 *   设置页 / 数据库  &gt;  .env 环境变量  &gt;  application.yml 默认值
 * </pre>
 * 通道表 {@code ai_chat_channel} / {@code ai_embedding_channel} 有行则读库；否则从
 * {@code DEEPSEEK_*} / {@code CLINIC_EMBEDDING_*} bootstrap。
 * 四类开关 {@code external_service} 同理（Chat / Embedding / Whisper / OCR）。
 *
 * <h2>与 agent 包的关系</h2>
 * Agent 助手（{@code com.fafeng.clinic.agent}）通过 {@link com.fafeng.clinic.ai.client.AiChatClient#chatWithTools}
 * 调用大模型 + {@code @Tool}；业务数据仍走各业务 Service，不直接改库。
 *
 * <h2>安全</h2>
 * 调用外部 API 前用 {@link com.fafeng.clinic.ai.util.Desensitizer} 脱敏；设置页存的 Key 用
 * {@link com.fafeng.clinic.ai.config.SecretEncryptor} AES 加密。
 *
 * @see com.fafeng.clinic.agent
 */
package com.fafeng.clinic.ai;
