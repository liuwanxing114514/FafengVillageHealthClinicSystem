package com.fafeng.clinic.inventory.vo;

import java.util.List;

public record OutboundPreviewVO(
        boolean sufficient,
        List<OutboundPreviewLineVO> lines
) {
}
