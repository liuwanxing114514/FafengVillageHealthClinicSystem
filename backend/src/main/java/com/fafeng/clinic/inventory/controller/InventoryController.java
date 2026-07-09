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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inbound")
    public Result<FlowVO> inbound(@Valid @RequestBody InboundRequest request) {
        return Result.ok(inventoryService.inbound(request));
    }

    @PostMapping("/outbound/preview")
    public Result<OutboundPreviewVO> previewOutbound(@Valid @RequestBody OutboundPreviewRequest request) {
        return Result.ok(inventoryService.previewOutbound(request));
    }

    @PostMapping("/outbound")
    public Result<List<FlowVO>> confirmOutbound(@Valid @RequestBody OutboundConfirmRequest request) {
        return Result.ok(inventoryService.confirmOutbound(request));
    }

    @PostMapping("/outbound/batch/preview")
    public Result<OutboundPreviewVO> previewBatchOutbound(@Valid @RequestBody BatchOutboundPreviewRequest request) {
        return Result.ok(inventoryService.previewBatchOutbound(request));
    }

    @PostMapping("/outbound/batch")
    public Result<BatchOutboundResultVO> confirmBatchOutbound(@Valid @RequestBody BatchOutboundConfirmRequest request) {
        return Result.ok(inventoryService.confirmBatchOutbound(request));
    }

    @PostMapping("/adjust")
    public Result<FlowVO> adjust(@Valid @RequestBody AdjustRequest request) {
        return Result.ok(inventoryService.adjust(request));
    }

    @GetMapping("/batches")
    public Result<List<BatchVO>> listBatches(@RequestParam(required = false) Long medicineId) {
        return Result.ok(inventoryService.listBatches(medicineId));
    }

    @GetMapping("/flows")
    public Result<PageVO<FlowVO>> listFlows(
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) String flowType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(inventoryService.listFlows(medicineId, flowType, page, size));
    }

    @GetMapping("/alerts")
    public Result<InventoryAlertsVO> alerts() {
        return Result.ok(inventoryService.getAlerts());
    }
}
