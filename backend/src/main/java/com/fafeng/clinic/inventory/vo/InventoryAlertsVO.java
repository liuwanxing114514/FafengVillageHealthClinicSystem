package com.fafeng.clinic.inventory.vo;

import java.util.List;

public record InventoryAlertsVO(
        List<LowStockAlertVO> lowStock,
        List<ExpiringAlertVO> expiring
) {
}
