package com.fafeng.clinic.inventory.export;

import com.fafeng.clinic.inventory.vo.FlowVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryFlowExcelExporterTest {

    private final InventoryFlowExcelExporter exporter = new InventoryFlowExcelExporter();

    @Test
    void exportProducesNonEmptyWorkbook() {
        FlowVO flow = new FlowVO(
                1L,
                10L,
                "测试药",
                20L,
                "B001",
                "INBOUND",
                new BigDecimal("100"),
                new BigDecimal("0"),
                new BigDecimal("100"),
                "片",
                null,
                null,
                null,
                null,
                "admin",
                OffsetDateTime.parse("2026-07-09T10:00:00+08:00"));
        byte[] bytes = exporter.export(List.of(flow));
        assertTrue(bytes.length > 100);
    }
}
