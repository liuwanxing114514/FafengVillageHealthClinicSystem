package com.fafeng.clinic.inventory.controller;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.inventory.dto.AdjustRequest;
import com.fafeng.clinic.inventory.dto.BatchOutboundConfirmRequest;
import com.fafeng.clinic.inventory.dto.BatchOutboundPreviewRequest;
import com.fafeng.clinic.inventory.dto.InboundRequest;
import com.fafeng.clinic.inventory.dto.OutboundConfirmRequest;
import com.fafeng.clinic.inventory.dto.OutboundPreviewRequest;
import com.fafeng.clinic.inventory.service.InventoryService;
import com.fafeng.clinic.inventory.vo.BatchOutboundResultVO;
import com.fafeng.clinic.inventory.vo.BatchVO;
import com.fafeng.clinic.inventory.vo.FlowVO;
import com.fafeng.clinic.inventory.vo.InventoryAlertsVO;
import com.fafeng.clinic.inventory.vo.OutboundPreviewVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存入出库、盘点与预警 API。
 * <p>出库须先预览批次（FEFO 推荐），医生确认后才扣减；库存不足时阻止出库。</p>
 */
@Tag(name = "库存", description = "入库、出库、盘点、流水与预警")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "手动入库", description = "增加库存批次并写入流水")
    @PostMapping("/inbound")
    public Result<FlowVO> inbound(@Valid @RequestBody InboundRequest request) {
        return Result.ok(inventoryService.inbound(request));
    }

    @Operation(summary = "出库预览", description = "按 FEFO 推荐默认批次，须确认后才扣减")
    @PostMapping("/outbound/preview")
    public Result<OutboundPreviewVO> previewOutbound(@Valid @RequestBody OutboundPreviewRequest request) {
        return Result.ok(inventoryService.previewOutbound(request));
    }

    @Operation(summary = "确认出库")
    @PostMapping("/outbound")
    public Result<List<FlowVO>> confirmOutbound(@Valid @RequestBody OutboundConfirmRequest request) {
        return Result.ok(inventoryService.confirmOutbound(request));
    }

    @Operation(summary = "批量出库预览")
    @PostMapping("/outbound/batch/preview")
    public Result<OutboundPreviewVO> previewBatchOutbound(@Valid @RequestBody BatchOutboundPreviewRequest request) {
        return Result.ok(inventoryService.previewBatchOutbound(request));
    }

    @Operation(summary = "确认批量出库")
    @PostMapping("/outbound/batch")
    public Result<BatchOutboundResultVO> confirmBatchOutbound(@Valid @RequestBody BatchOutboundConfirmRequest request) {
        return Result.ok(inventoryService.confirmBatchOutbound(request));
    }

    @Operation(summary = "盘点修正", description = "须填写原因并生成流水")
    @PostMapping("/adjust")
    public Result<FlowVO> adjust(@Valid @RequestBody AdjustRequest request) {
        return Result.ok(inventoryService.adjust(request));
    }

    @Operation(summary = "查询库存批次")
    @GetMapping("/batches")
    public Result<List<BatchVO>> listBatches(@RequestParam(required = false) Long medicineId) {
        return Result.ok(inventoryService.listBatches(medicineId));
    }

    @Operation(summary = "库存流水列表")
    @GetMapping("/flows")
    public Result<PageVO<FlowVO>> listFlows(
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) String flowType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(inventoryService.listFlows(medicineId, flowType, page, size));
    }

    @Operation(summary = "库存预警", description = "库存不足与临期（3 个月内）药品")
    @GetMapping("/alerts")
    public Result<InventoryAlertsVO> alerts() {
        return Result.ok(inventoryService.getAlerts());
    }
}
