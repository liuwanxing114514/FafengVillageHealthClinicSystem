package com.fafeng.clinic.inventory.vo;

import java.util.List;

public record DashboardSummaryVO(
        int lowStockCount,
        int expiringCount,
        List<LowStockAlertVO> lowStockPreview,
        List<ExpiringAlertVO> expiringPreview
) {
}
