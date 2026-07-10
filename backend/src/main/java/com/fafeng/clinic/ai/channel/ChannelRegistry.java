package com.fafeng.clinic.ai.channel;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

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
