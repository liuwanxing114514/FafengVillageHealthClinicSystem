package com.fafeng.clinic.ai.channel;

import java.util.List;

public interface ChannelSource {

    boolean hasDbChatChannels();

    boolean hasDbEmbeddingChannels();

    List<ChatChannelConfig> loadChatChannels();

    List<EmbeddingChannelConfig> loadEmbeddingChannels();
}
