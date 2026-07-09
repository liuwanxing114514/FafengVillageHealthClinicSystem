
package com.fafeng.clinic.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.clinic.entity.Prescription;
import com.fafeng.clinic.clinic.mapper.PrescriptionMapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.inventory.dto.AdjustRequest;
import com.fafeng.clinic.inventory.dto.BatchOutboundConfirmLineRequest;
import com.fafeng.clinic.inventory.dto.BatchOutboundConfirmRequest;
import com.fafeng.clinic.inventory.dto.BatchOutboundPreviewRequest;
import com.fafeng.clinic.inventory.dto.InboundRequest;
import com.fafeng.clinic.inventory.dto.OutboundAllocationRequest;
import com.fafeng.clinic.inventory.dto.OutboundConfirmRequest;
import com.fafeng.clinic.inventory.dto.OutboundLineRequest;
import com.fafeng.clinic.inventory.dto.OutboundPreviewRequest;
import com.fafeng.clinic.inventory.entity.InventoryBatch;
import com.fafeng.clinic.inventory.entity.InventoryFlow;
import com.fafeng.clinic.inventory.mapper.InventoryBatchMapper;
import com.fafeng.clinic.inventory.mapper.InventoryFlowMapper;
import com.fafeng.clinic.inventory.util.InventoryUnitConverter;
import com.fafeng.clinic.inventory.vo.BatchAllocationVO;
import com.fafeng.clinic.inventory.vo.BatchOutboundResultVO;
import com.fafeng.clinic.inventory.vo.BatchVO;
import com.fafeng.clinic.inventory.vo.DashboardSummaryVO;
import com.fafeng.clinic.inventory.vo.ExpiringAlertVO;
import com.fafeng.clinic.inventory.vo.FlowVO;
import com.fafeng.clinic.inventory.vo.InventoryAlertsVO;
import com.fafeng.clinic.inventory.vo.LowStockAlertVO;
import com.fafeng.clinic.inventory.vo.OutboundPreviewLineVO;
import com.fafeng.clinic.inventory.vo.OutboundPreviewVO;
import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.entity.MedicineUnitConversion;
import com.fafeng.clinic.medicine.mapper.MedicineMapper;
import com.fafeng.clinic.medicine.mapper.MedicineUnitConversionMapper;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;

/**
 * 库存业务核心：入库、出库、盘点、流水与预警。
 * <p>所有数量变更必须写入 {@code inventory_flow}；出库按 FEFO 推荐批次，须用户确认后才扣减；
 * 库存不足时抛出业务异常阻止出库。</p>
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int EXPIRING_MONTHS = 3;
    private static final int DASHBOARD_PREVIEW_SIZE = 5;

    private final InventoryBatchMapper batchMapper;
    private final InventoryFlowMapper flowMapper;
    private final MedicineMapper medicineMapper;
    private final MedicineUnitConversionMapper conversionMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PatientService patientService;
    private final AuditLogService auditLogService;


    @Transactional
    public FlowVO inbound(InboundRequest request) {
        Medicine medicine = requireMedicine(request.medicineId());
        List<MedicineUnitConversion> conversions = listConversions(medicine.getId());
        BigDecimal baseQuantity = InventoryUnitConverter.toBaseQuantity(
                medicine, conversions, request.quantity(), request.unit());

        BigDecimal totalBefore = getTotalStock(medicine.getId());

        InventoryBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<InventoryBatch>()
                .eq(InventoryBatch::getMedicineId, medicine.getId())
                .eq(InventoryBatch::getBatchNo, request.batchNo().trim()));
        OffsetDateTime now = OffsetDateTime.now();
        if (batch == null) {
            batch = new InventoryBatch();
            batch.setMedicineId(medicine.getId());
            batch.setBatchNo(request.batchNo().trim());
            batch.setExpiryDate(request.expiryDate());
            batch.setQuantity(baseQuantity);
            batch.setPurchasePrice(request.purchasePrice());
            batch.setSupplier(trimToEmpty(request.supplier()));
            batch.setStatus(InventoryBatch.STATUS_ACTIVE);
            batch.setCreatedAt(now);
            batch.setUpdatedAt(now);
            batchMapper.insert(batch);
        } else {
            batch.setQuantity(batch.getQuantity().add(baseQuantity));
            if (request.expiryDate() != null) {
                batch.setExpiryDate(request.expiryDate());
            }
            if (request.purchasePrice() != null) {
                batch.setPurchasePrice(request.purchasePrice());
            }
            if (request.supplier() != null && !request.supplier().isBlank()) {
                batch.setSupplier(request.supplier().trim());
            }
            batch.setStatus(InventoryBatch.STATUS_ACTIVE);
            batch.setUpdatedAt(now);
            batchMapper.updateById(batch);
        }

        BigDecimal totalAfter = totalBefore.add(baseQuantity);
        InventoryFlow flow = insertFlow(
                medicine,
                batch.getId(),
                InventoryFlow.TYPE_INBOUND,
                baseQuantity,
                totalBefore,
                totalAfter,
                medicine.getBaseUnit(),
                null,
                null,
                null,
                request.remark());

        auditLogService.log("INBOUND", "inventory_batch", batch.getId(),
                "{\"medicineId\":" + medicine.getId() + ",\"quantity\":" + baseQuantity + "}");
        return toFlowVO(flow, medicine, batch);
    }

    public OutboundPreviewVO previewOutbound(OutboundPreviewRequest request) {
        validatePrescriptionContext(request.patientId(), request.prescriptionId());
        List<OutboundPreviewLineVO> lines = new ArrayList<>();
        boolean allSufficient = true;
        for (OutboundLineRequest item : request.items()) {
            OutboundPreviewLineVO line = buildPreviewLine(item);
            lines.add(line);
            if (!line.sufficient()) {
                allSufficient = false;
            }
        }
        return new OutboundPreviewVO(allSufficient, lines);
    }

    public OutboundPreviewVO previewBatchOutbound(BatchOutboundPreviewRequest request) {
        validateBatchItems(request.items());
        List<OutboundPreviewLineVO> lines = new ArrayList<>();
        boolean allSufficient = true;
        for (OutboundLineRequest item : request.items()) {
            OutboundPreviewLineVO line = buildPreviewLine(item);
            lines.add(line);
            if (!line.sufficient()) {
                allSufficient = false;
            }
        }
        return new OutboundPreviewVO(allSufficient, lines);
    }

    @Transactional
    public BatchOutboundResultVO confirmBatchOutbound(BatchOutboundConfirmRequest request) {
        String reason = request.reason().trim();
        if (reason.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写出库原因");
        }
        validateBatchConfirmLines(request.lines());

        List<OutboundLineRequest> previewItems = request.lines().stream()
                .map(line -> new OutboundLineRequest(line.medicineId(), line.quantity(), line.unit()))
                .toList();
        OutboundPreviewVO preview = previewBatchOutbound(new BatchOutboundPreviewRequest(previewItems));
        if (!preview.sufficient()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "部分药品库存不足，整单无法出库");
        }

        List<FlowVO> allFlows = new ArrayList<>();
        for (BatchOutboundConfirmLineRequest line : request.lines()) {
            Medicine medicine = requireMedicine(line.medicineId());
            List<MedicineUnitConversion> conversions = listConversions(medicine.getId());
            BigDecimal baseNeeded = InventoryUnitConverter.toBaseQuantity(
                    medicine, conversions, line.quantity(), line.unit());
            BigDecimal allocated = line.allocations().stream()
                    .map(OutboundAllocationRequest::quantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (allocated.compareTo(baseNeeded) != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "「" + medicine.getName() + "」批次分配数量与出库数量不一致");
            }
            allFlows.addAll(deductOutbound(medicine, line.allocations(), null, null, reason));
        }

        auditLogService.log("BATCH_OUTBOUND", "inventory_flow", null,
                "{\"lineCount\":" + request.lines().size()
                        + ",\"flowCount\":" + allFlows.size()
                        + ",\"reason\":\"" + escapeJson(reason) + "\"}");
        return new BatchOutboundResultVO(request.lines().size(), allFlows.size(), allFlows);
    }

    public void validateOutboundAllocationTotals(Long medicineId,
                                                  BigDecimal quantity,
                                                  String unit,
                                                  List<OutboundAllocationRequest> allocations) {
        Medicine medicine = requireMedicine(medicineId);
        List<MedicineUnitConversion> conversions = listConversions(medicine.getId());
        BigDecimal baseNeeded = InventoryUnitConverter.toBaseQuantity(
                medicine, conversions, quantity, unit);
        BigDecimal allocated = allocations.stream()
                .map(OutboundAllocationRequest::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocated.compareTo(baseNeeded) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "「" + medicine.getName() + "」批次分配数量与出库数量不一致");
        }
    }

    @Transactional
    public List<FlowVO> confirmOutbound(OutboundConfirmRequest request) {
        validatePrescriptionContext(request.patientId(), request.prescriptionId());
        Medicine medicine = requireMedicine(request.medicineId());
        List<FlowVO> flows = deductOutbound(
                medicine,
                request.allocations(),
                request.patientId(),
                request.prescriptionId(),
                null);
        auditLogService.log("OUTBOUND", "prescription", request.prescriptionId(),
                "{\"medicineId\":" + medicine.getId() + "}");
        return flows;
    }

    @Transactional
    public FlowVO adjust(AdjustRequest request) {
        if (request.quantityChange().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "调整数量不能为 0");
        }
        Medicine medicine = requireMedicine(request.medicineId());
        List<MedicineUnitConversion> conversions = listConversions(medicine.getId());
        BigDecimal signedChange = request.quantityChange();
        BigDecimal baseChange = InventoryUnitConverter.toBaseQuantity(
                medicine, conversions, signedChange.abs(), request.unit());
        if (signedChange.signum() < 0) {
            baseChange = baseChange.negate();
        }
        InventoryBatch batch = requireBatch(request.batchId());
        if (!batch.getMedicineId().equals(medicine.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "批次与药品不匹配");
        }

        BigDecimal totalBefore = getTotalStock(medicine.getId());
        BigDecimal newBatchQty = batch.getQuantity().add(baseChange);
        if (newBatchQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "批次库存不足，无法扣减");
        }
        batch.setQuantity(newBatchQty);
        batch.setStatus(newBatchQty.compareTo(BigDecimal.ZERO) == 0
                ? InventoryBatch.STATUS_DEPLETED
                : InventoryBatch.STATUS_ACTIVE);
        batch.setUpdatedAt(OffsetDateTime.now());
        batchMapper.updateById(batch);

        BigDecimal totalAfter = totalBefore.add(baseChange);
        InventoryFlow flow = insertFlow(
                medicine,
                batch.getId(),
                InventoryFlow.TYPE_ADJUST,
                baseChange,
                totalBefore,
                totalAfter,
                medicine.getBaseUnit(),
                null,
                null,
                request.reason().trim(),
                request.remark());

        auditLogService.log("INVENTORY_ADJUST", "inventory_batch", batch.getId(),
                "{\"reason\":\"" + escapeJson(request.reason()) + "\"}");
        return toFlowVO(flow, medicine, batch);
    }

    public List<BatchVO> listBatches(Long medicineId) {
        LambdaQueryWrapper<InventoryBatch> wrapper = new LambdaQueryWrapper<InventoryBatch>()
                .orderByAsc(InventoryBatch::getExpiryDate)
                .orderByAsc(InventoryBatch::getId);
        if (medicineId != null) {
            wrapper.eq(InventoryBatch::getMedicineId, medicineId);
        }
        return batchMapper.selectList(wrapper).stream()
                .map(batch -> toBatchVO(batch, requireMedicine(batch.getMedicineId())))
                .toList();
    }

    public PageVO<FlowVO> listFlows(Long medicineId, String flowType, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = flowMapper.searchPage(
                new Page<>(safePage, safeSize),
                medicineId,
                flowType == null || flowType.isBlank() ? null : flowType.trim());
        List<FlowVO> records = result.getRecords().stream()
                .map(flow -> {
                    Medicine medicine = medicineMapper.selectById(flow.getMedicineId());
                    InventoryBatch batch = flow.getBatchId() == null ? null : batchMapper.selectById(flow.getBatchId());
                    return toFlowVO(flow, medicine, batch);
                })
                .toList();
        return new PageVO<>(records, result.getTotal(), safePage, safeSize);
    }

    public InventoryAlertsVO getAlerts() {
        return new InventoryAlertsVO(listLowStockAlerts(), listExpiringAlerts());
    }

    public DashboardSummaryVO getDashboardSummary() {
        List<LowStockAlertVO> lowStock = listLowStockAlerts();
        List<ExpiringAlertVO> expiring = listExpiringAlerts();
        return new DashboardSummaryVO(
                lowStock.size(),
                expiring.size(),
                lowStock.stream().limit(DASHBOARD_PREVIEW_SIZE).toList(),
                expiring.stream().limit(DASHBOARD_PREVIEW_SIZE).toList());
    }

    public BigDecimal getTotalStock(Long medicineId) {
        return batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                        .eq(InventoryBatch::getMedicineId, medicineId)
                        .eq(InventoryBatch::getStatus, InventoryBatch.STATUS_ACTIVE))
                .stream()
                .map(InventoryBatch::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OutboundPreviewLineVO buildPreviewLine(OutboundLineRequest item) {
        Medicine medicine = requireMedicine(item.medicineId());
        List<MedicineUnitConversion> conversions = listConversions(medicine.getId());
        BigDecimal baseNeeded = InventoryUnitConverter.toBaseQuantity(
                medicine, conversions, item.quantity(), item.unit());
        BigDecimal totalStock = getTotalStock(medicine.getId());
        boolean sufficient = totalStock.compareTo(baseNeeded) >= 0;
        List<BatchAllocationVO> allocations = allocateFefo(medicine.getId(), baseNeeded);
        return new OutboundPreviewLineVO(
                medicine.getId(),
                medicine.getName(),
                item.quantity(),
                item.unit(),
                baseNeeded,
                medicine.getBaseUnit(),
                sufficient,
                allocations);
    }

    private List<BatchAllocationVO> allocateFefo(Long medicineId, BigDecimal baseNeeded) {
        List<InventoryBatch> batches = batchMapper.listAvailableForFefo(medicineId);
        BigDecimal remaining = baseNeeded;
        List<BatchAllocationVO> allocations = new ArrayList<>();
        for (InventoryBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal take = batch.getQuantity().min(remaining);
            allocations.add(new BatchAllocationVO(
                    batch.getId(),
                    batch.getBatchNo(),
                    batch.getExpiryDate(),
                    batch.getQuantity(),
                    take));
            remaining = remaining.subtract(take);
        }
        return allocations;
    }

    private List<FlowVO> deductOutbound(Medicine medicine,
                                        List<OutboundAllocationRequest> allocations,
                                        Long patientId,
                                        Long prescriptionId,
                                        String reason) {
        List<FlowVO> flows = new ArrayList<>();
        for (OutboundAllocationRequest allocation : allocations) {
            InventoryBatch batch = requireBatch(allocation.batchId());
            if (!batch.getMedicineId().equals(medicine.getId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "批次与药品不匹配");
            }
            BigDecimal deduct = allocation.quantity();
            if (deduct.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "出库数量必须大于 0");
            }
            if (batch.getQuantity().compareTo(deduct) < 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "批次「" + batch.getBatchNo() + "」库存不足");
            }
            BigDecimal totalBefore = getTotalStock(medicine.getId());
            batch.setQuantity(batch.getQuantity().subtract(deduct));
            if (batch.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                batch.setQuantity(BigDecimal.ZERO);
                batch.setStatus(InventoryBatch.STATUS_DEPLETED);
            }
            batch.setUpdatedAt(OffsetDateTime.now());
            batchMapper.updateById(batch);

            BigDecimal totalAfter = totalBefore.subtract(deduct);
            InventoryFlow flow = insertFlow(
                    medicine,
                    batch.getId(),
                    InventoryFlow.TYPE_OUTBOUND,
                    deduct.negate(),
                    totalBefore,
                    totalAfter,
                    medicine.getBaseUnit(),
                    patientId,
                    prescriptionId,
                    reason,
                    null);
            flows.add(toFlowVO(flow, medicine, batch));
        }
        return flows;
    }

    private void validateBatchItems(List<OutboundLineRequest> items) {
        Set<Long> medicineIds = new HashSet<>();
        for (OutboundLineRequest item : items) {
            if (!medicineIds.add(item.medicineId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "同一药品请勿重复添加，请合并数量");
            }
        }
    }

    private void validateBatchConfirmLines(List<BatchOutboundConfirmLineRequest> lines) {
        Set<Long> medicineIds = new HashSet<>();
        for (BatchOutboundConfirmLineRequest line : lines) {
            if (!medicineIds.add(line.medicineId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "同一药品请勿重复添加，请合并数量");
            }
        }
    }

    private void validatePrescriptionContext(Long patientId, Long prescriptionId) {
        patientService.requirePatient(patientId);
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null || Prescription.STATUS_VOID.equals(prescription.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "处方不存在");
        }
        if (!prescription.getPatientId().equals(patientId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "处方与患者不匹配");
        }
    }

    private InventoryFlow insertFlow(Medicine medicine,
                                       Long batchId,
                                       String flowType,
                                       BigDecimal quantityChange,
                                       BigDecimal quantityBefore,
                                       BigDecimal quantityAfter,
                                       String unit,
                                       Long patientId,
                                       Long prescriptionId,
                                       String reason,
                                       String remark) {
        InventoryFlow flow = new InventoryFlow();
        flow.setMedicineId(medicine.getId());
        flow.setBatchId(batchId);
        flow.setFlowType(flowType);
        flow.setQuantityChange(quantityChange);
        flow.setQuantityBefore(quantityBefore);
        flow.setQuantityAfter(quantityAfter);
        flow.setUnit(unit);
        flow.setPatientId(patientId);
        flow.setPrescriptionId(prescriptionId);
        flow.setReason(reason);
        flow.setRemark(trimToNull(remark));
        flow.setOperator(currentOperator());
        flow.setCreatedAt(OffsetDateTime.now());
        flowMapper.insert(flow);
        return flow;
    }

    private List<LowStockAlertVO> listLowStockAlerts() {
        List<Medicine> medicines = medicineMapper.selectList(new LambdaQueryWrapper<Medicine>()
                .ne(Medicine::getStatus, Medicine.STATUS_DELETED));
        List<LowStockAlertVO> alerts = new ArrayList<>();
        for (Medicine medicine : medicines) {
            if (!Medicine.STATUS_ACTIVE.equals(medicine.getStatus())) {
                continue;
            }
            BigDecimal total = getTotalStock(medicine.getId());
            BigDecimal threshold = medicine.getStockThreshold() == null
                    ? BigDecimal.ZERO
                    : medicine.getStockThreshold();
            if (total.compareTo(threshold) < 0) {
                alerts.add(new LowStockAlertVO(
                        medicine.getId(),
                        medicine.getName(),
                        medicine.getSpecification(),
                        total,
                        threshold,
                        medicine.getBaseUnit()));
            }
        }
        return alerts;
    }

    private List<ExpiringAlertVO> listExpiringAlerts() {
        LocalDate deadline = LocalDate.now().plusMonths(EXPIRING_MONTHS);
        List<InventoryBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .eq(InventoryBatch::getStatus, InventoryBatch.STATUS_ACTIVE)
                .gt(InventoryBatch::getQuantity, BigDecimal.ZERO)
                .isNotNull(InventoryBatch::getExpiryDate)
                .le(InventoryBatch::getExpiryDate, deadline)
                .orderByAsc(InventoryBatch::getExpiryDate));
        return batches.stream()
                .map(batch -> {
                    Medicine medicine = medicineMapper.selectById(batch.getMedicineId());
                    return new ExpiringAlertVO(
                            batch.getId(),
                            batch.getMedicineId(),
                            medicine == null ? "—" : medicine.getName(),
                            batch.getBatchNo(),
                            batch.getExpiryDate(),
                            batch.getQuantity(),
                            medicine == null ? "" : medicine.getBaseUnit());
                })
                .toList();
    }

    private BatchVO toBatchVO(InventoryBatch batch, Medicine medicine) {
        return new BatchVO(
                batch.getId(),
                batch.getMedicineId(),
                medicine.getName(),
                batch.getBatchNo(),
                batch.getExpiryDate(),
                batch.getQuantity(),
                medicine.getBaseUnit(),
                batch.getPurchasePrice(),
                batch.getSupplier(),
                batch.getStatus(),
                batch.getCreatedAt());
    }

    private FlowVO toFlowVO(InventoryFlow flow, Medicine medicine, InventoryBatch batch) {
        return new FlowVO(
                flow.getId(),
                flow.getMedicineId(),
                medicine == null ? "—" : medicine.getName(),
                flow.getBatchId(),
                batch == null ? null : batch.getBatchNo(),
                flow.getFlowType(),
                flow.getQuantityChange(),
                flow.getQuantityBefore(),
                flow.getQuantityAfter(),
                flow.getUnit(),
                flow.getPatientId(),
                flow.getPrescriptionId(),
                flow.getReason(),
                flow.getRemark(),
                flow.getOperator(),
                flow.getCreatedAt());
    }

    private Medicine requireMedicine(Long id) {
        Medicine medicine = medicineMapper.selectById(id);
        if (medicine == null || Medicine.STATUS_DELETED.equals(medicine.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "药品不存在");
        }
        return medicine;
    }

    private InventoryBatch requireBatch(Long id) {
        InventoryBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
        }
        return batch;
    }

    private List<MedicineUnitConversion> listConversions(Long medicineId) {
        return conversionMapper.selectList(new LambdaQueryWrapper<MedicineUnitConversion>()
                .eq(MedicineUnitConversion::getMedicineId, medicineId));
    }

    private String currentOperator() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return AuditLogService.OPERATOR_ADMIN;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
