package com.fafeng.clinic.ai.vo;

import java.util.Map;

public record ExternalServicesOverviewVO(
        Map<String, ExternalServiceItemVO> services,
        boolean dbBacked,
        boolean dbChatChannels,
        boolean dbEmbeddingChannels) {
}
