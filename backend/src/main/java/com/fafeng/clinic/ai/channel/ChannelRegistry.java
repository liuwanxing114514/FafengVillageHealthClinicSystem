package com.fafeng.clinic.ai.channel;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 运行时 Chat / Embedding 通道链（内存快照）。
 *
 * <p>启动与设置页变更时 {@link #refresh()}：经 {@link EnvBootstrapChannelSource} 加载配置，
 * 再由 Factory 建成 {@link ChatChannelRuntime} / {@link EmbeddingChannelRuntime} 列表。
 * {@link com.fafeng.clinic.ai.client.ResilientAiChatClient} 按 priority 顺序调用，失败时 failover。
 */
@Component
public class ChannelRegistry {

    private final EnvBootstrapChannelSource channelSource;
    private final ChatChannelFactory chatChannelFactory;
    private final EmbeddingChannelFactory embeddingChannelFactory;

    private volatile List<ChatChannelRuntime> chatChannels = List.of();
    private volatile List<EmbeddingChannelRuntime> embeddingChannels = List.of();
    private volatile boolean dbChatBacked;
    private volatile boolean dbEmbeddingBacked;

    public ChannelRegistry(EnvBootstrapChannelSource channelSource,
                           ChatChannelFactory chatChannelFactory,
                           EmbeddingChannelFactory embeddingChannelFactory) {
        this.channelSource = channelSource;
        this.chatChannelFactory = chatChannelFactory;
        this.embeddingChannelFactory = embeddingChannelFactory;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    public synchronized void refresh() {
        dbChatBacked = channelSource.hasDbChatChannels();
        dbEmbeddingBacked = channelSource.hasDbEmbeddingChannels();
        List<ChatChannelConfig> chatConfigs = channelSource.loadChatChannels();
        List<EmbeddingChannelConfig> embeddingConfigs = channelSource.loadEmbeddingChannels();
        chatChannels = chatChannelFactory.buildRuntimes(chatConfigs);
        embeddingChannels = embeddingChannelFactory.buildRuntimes(embeddingConfigs);
    }

    public List<ChatChannelRuntime> chatChannels() {
        return chatChannels;
    }

    public List<EmbeddingChannelRuntime> embeddingChannels() {
        return embeddingChannels;
    }

    public boolean hasUsableChatChannels() {
        return !chatChannels.isEmpty();
    }

    public boolean hasUsableEmbeddingChannels() {
        return !embeddingChannels.isEmpty();
    }

    public boolean isDbChatBacked() {
        return dbChatBacked;
    }

    public boolean isDbEmbeddingBacked() {
        return dbEmbeddingBacked;
    }

    public int chatChannelCount() {
        return channelSource.loadChatChannels().size();
    }

    public int embeddingChannelCount() {
        return channelSource.loadEmbeddingChannels().size();
    }

    public EmbeddingChannelConfig primaryEmbeddingConfig() {
        List<EmbeddingChannelConfig> configs = channelSource.loadEmbeddingChannels();
        return configs.isEmpty() ? null : configs.get(0);
    }
}
