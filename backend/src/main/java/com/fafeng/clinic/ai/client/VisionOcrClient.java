package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.channel.ChannelInvocationHelper;
import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.channel.ChatChannelConfig;
import com.fafeng.clinic.ai.channel.ChatChannelFactory;
import com.fafeng.clinic.ai.channel.ChatChannelRuntime;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 多模态 Chat API：图片 → 纯文本（不生成 JSON）。整理仍由 {@link AiInboundOcrService} 走文本 Chat。
 */
@Component
public class VisionOcrClient {

    private static final Logger log = LoggerFactory.getLogger(VisionOcrClient.class);

    private static final String OCR_SYSTEM_PROMPT = """
            你是进货单据 OCR 助手。请识别用户图片中的全部文字。
            按从上到下、从左到右的阅读顺序输出纯文本。
            不要解释、不要 markdown、不要 JSON，只输出识别到的文字内容。
            """;

    private final ExternalServiceConfigService externalServiceConfigService;
    private final ChannelRegistry channelRegistry;
    private final ChatChannelFactory chatChannelFactory;

    public VisionOcrClient(ExternalServiceConfigService externalServiceConfigService,
                           ChannelRegistry channelRegistry,
                           ChatChannelFactory chatChannelFactory) {
        this.externalServiceConfigService = externalServiceConfigService;
        this.channelRegistry = channelRegistry;
        this.chatChannelFactory = chatChannelFactory;
    }

    public boolean isConfigured() {
        return externalServiceConfigService.isOcrEnabled()
                && externalServiceConfigService.isChatEnabled()
                && channelRegistry.hasUsableChatChannels();
    }

    public String recognize(byte[] imageBytes, String filename, String contentType) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Vision OCR 未配置，请开启 OCR 与对话服务并配置视觉模型");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件为空");
        }

        String visionModel = externalServiceConfigService.getOcrVisionModel();
        MimeType mimeType = resolveMimeType(contentType, filename);
        Media media = buildMedia(imageBytes, mimeType);

        List<ChatChannelRuntime> channels = channelRegistry.chatChannels();
        List<String> labels = channels.stream()
                .map(c -> c.config().label())
                .collect(Collectors.toList());

        try {
            return ChannelInvocationHelper.invokeWithFailover(labels, index -> {
                ChatChannelConfig config = channels.get(index).config();
                ChatClient client = chatChannelFactory.buildClientWithModel(config, visionModel);
                String text = client.prompt()
                        .system(OCR_SYSTEM_PROMPT)
                        .user(u -> u.text("请识别图片中的全部文字。").media(media))
                        .call()
                        .content();
                return requireText(text);
            });
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Vision OCR failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Vision OCR 暂不可用，请稍后重试或切换本地 PaddleOCR");
        }
    }

    private static Media buildMedia(byte[] imageBytes, MimeType mimeType) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;
        return Media.builder().mimeType(mimeType).data(dataUrl).build();
    }

    private static MimeType resolveMimeType(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MimeTypeUtils.parseMimeType(contentType);
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) {
                return MimeTypeUtils.IMAGE_PNG;
            }
            if (lower.endsWith(".webp")) {
                return MimeTypeUtils.parseMimeType("image/webp");
            }
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "未识别到有效文字，请确认是清晰的打印版单据");
        }
        return text.trim();
    }
}
