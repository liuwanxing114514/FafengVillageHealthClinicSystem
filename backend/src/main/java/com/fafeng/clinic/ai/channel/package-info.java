/**
 * Chat / Embedding 多通道：从 DB 或 env 加载配置，构建 Spring AI 客户端链，支持按 priority  failover。
 *
 * <h2>核心类</h2>
 * <ul>
 *   <li>{@link com.fafeng.clinic.ai.channel.ChannelRegistry} — 内存中的通道链，设置页保存后 {@code refresh()}</li>
 *   <li>{@link com.fafeng.clinic.ai.channel.EnvBootstrapChannelSource} — 统一入口：有 DB 通道则委托 {@link com.fafeng.clinic.ai.channel.DbChannelSource}，否则读 env</li>
 *   <li>{@link com.fafeng.clinic.ai.channel.ChatChannelFactory} / {@link com.fafeng.clinic.ai.channel.EmbeddingChannelFactory} — 配置 → 可运行的 Spring AI 客户端</li>
 *   <li>{@link com.fafeng.clinic.ai.channel.AiChannelFailoverPolicy} — 429/5xx 等是否切换下一通道</li>
 * </ul>
 *
 * <h2>failover 顺序</h2>
 * {@code priority} 数字越小越优先；当前通道失败且 policy 允许时，尝试下一通道。
 */
package com.fafeng.clinic.ai.channel;
