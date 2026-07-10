package com.fafeng.clinic.ai.channel;

import com.fafeng.clinic.ai.client.AiClientExceptionMapper;
import com.fafeng.clinic.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * 按通道顺序调用，限流/拥堵时自动切换下一通道。
 */
public final class ChannelInvocationHelper {

    private static final Logger log = LoggerFactory.getLogger(ChannelInvocationHelper.class);

    private ChannelInvocationHelper() {
    }

    public static <T> T invokeWithFailover(List<String> channelLabels, Function<Integer, T> invoker) {
        if (channelLabels.isEmpty()) {
            throw new BusinessException(com.fafeng.clinic.common.ErrorCode.SERVICE_UNAVAILABLE, "AI 服务未配置");
        }
        Exception lastEx = null;
        for (int i = 0; i < channelLabels.size(); i++) {
            try {
                return invoker.apply(i);
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                lastEx = ex;
                if (i + 1 < channelLabels.size() && AiChannelFailoverPolicy.isFallbackEligible(ex)) {
                    log.warn("AI channel {} failed ({}), switching to next",
                            channelLabels.get(i), ex.getMessage());
                    continue;
                }
                log.warn("AI channel {} failed: {}", channelLabels.get(i), ex.getMessage());
                throw AiClientExceptionMapper.toBusinessException(ex);
            }
        }
        throw AiClientExceptionMapper.toBusinessException(lastEx != null ? lastEx : new IllegalStateException("no channel"));
    }
}
