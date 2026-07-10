package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.channel.ChannelInvocationHelper;
import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.channel.EmbeddingChannelRuntime;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务层使用的 Embedding 门面（{@code @Primary}，实现 Spring AI {@link EmbeddingModel}）。
 * RAG 向量化与相似病例搜索注入本类；开关与通道链同 Chat。
 */
@Component
@Primary
public class ResilientEmbeddingModel implements EmbeddingModel {

    private final ExternalServiceConfigService externalServiceConfigService;
    private final ChannelRegistry channelRegistry;

    public ResilientEmbeddingModel(ExternalServiceConfigService externalServiceConfigService,
                                   ChannelRegistry channelRegistry) {
        this.externalServiceConfigService = externalServiceConfigService;
        this.channelRegistry = channelRegistry;
    }

    public boolean isConfigured() {
        return externalServiceConfigService.isEmbeddingEnabled() && channelRegistry.hasUsableEmbeddingChannels();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        requireConfigured();
        return invokeOnChannels(model -> model.call(request));
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getFormattedContent(MetadataMode.EMBED));
    }

    @Override
    public float[] embed(String text) {
        requireConfigured();
        return invokeOnChannels(model -> model.embed(text));
    }

    @Override
    public int dimensions() {
        var config = channelRegistry.primaryEmbeddingConfig();
        return config == null ? 0 : config.dimensions();
    }

    private <T> T invokeOnChannels(java.util.function.Function<EmbeddingModel, T> call) {
        List<EmbeddingChannelRuntime> channels = channelRegistry.embeddingChannels();
        List<String> labels = channels.stream()
                .map(c -> c.config().label())
                .collect(Collectors.toList());
        return ChannelInvocationHelper.invokeWithFailover(labels,
                index -> call.apply(channels.get(index).embeddingModel()));
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Embedding 配置不完整，请检查配置");
        }
    }
}
