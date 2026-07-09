package com.fafeng.clinic.clinic.vo;

import java.math.BigDecimal;
import java.util.List;

public record OutboundDraftVO(
        Long prescriptionId,
        Long patientId,
        String patientName,
        String diagnosis,
        List<OutboundDraftItemVO> items
) {
}
