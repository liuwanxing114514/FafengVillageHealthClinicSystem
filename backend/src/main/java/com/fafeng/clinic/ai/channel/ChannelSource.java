package com.fafeng.clinic.ai.channel;

import java.util.List;

/**
 * 通道配置加载抽象：DB 有记录则 {@link DbChannelSource}，否则 env bootstrap。
 */
public interface ChannelSource {

    boolean hasDbChatChannels();

    boolean hasDbEmbeddingChannels();

    List<ChatChannelConfig> loadChatChannels();

    List<EmbeddingChannelConfig> loadEmbeddingChannels();
}
