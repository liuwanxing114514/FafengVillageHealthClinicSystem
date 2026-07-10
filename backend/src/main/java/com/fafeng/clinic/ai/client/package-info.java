/**
 * 对外部 AI 服务的客户端门面；业务与 Agent 只依赖接口，不直接拼 HTTP。
 *
 * <ul>
 *   <li>{@link com.fafeng.clinic.ai.client.AiChatClient} — 对话补全、带 {@code @Tool} 的 Agent 对话</li>
 *   <li>{@link com.fafeng.clinic.ai.client.ResilientAiChatClient} — {@code @Primary} 实现，走 {@link com.fafeng.clinic.ai.channel.ChannelRegistry} 多通道 failover</li>
 *   <li>{@link com.fafeng.clinic.ai.client.ResilientEmbeddingModel} — {@code @Primary} Embedding，同样 failover</li>
 *   <li>{@link com.fafeng.clinic.ai.client.HttpWhisperClient} / {@link com.fafeng.clinic.ai.client.HttpOcrClient} — 读 {@link com.fafeng.clinic.ai.config.ExternalServiceConfigService} 的 URL 开关</li>
 *   <li>{@link com.fafeng.clinic.ai.client.AiClientExceptionMapper} — 将 Spring AI 异常映射为业务错误，并触发 failover 判定</li>
 * </ul>
 */
package com.fafeng.clinic.ai.client;
