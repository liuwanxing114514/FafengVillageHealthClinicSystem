package com.fafeng.clinic.ai.dto;

import com.fafeng.clinic.inventory.dto.OutboundAllocationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ApproveOutboundLineRequest(
        @NotNull Long medicineId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @NotBlank String unit,
        @NotEmpty @Valid List<OutboundAllocationRequest> allocations
) {
}
