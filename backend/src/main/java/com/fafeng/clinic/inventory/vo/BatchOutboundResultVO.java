package com.fafeng.clinic.inventory.vo;

import java.util.List;

public record BatchOutboundResultVO(
        int lineCount,
        int flowCount,
        List<FlowVO> flows
) {
}
