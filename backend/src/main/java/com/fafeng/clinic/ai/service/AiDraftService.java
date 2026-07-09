package com.fafeng.clinic.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.agent.model.OutboundDraftPayload;
import com.fafeng.clinic.ai.dto.ApproveOutboundDraftRequest;
import com.fafeng.clinic.ai.dto.ApproveOutboundLineRequest;
import com.fafeng.clinic.ai.dto.ApproveVisitDraftRequest;
import com.fafeng.clinic.ai.dto.CreateAiDraftRequest;
import com.fafeng.clinic.ai.dto.UpdateAiDraftPayloadRequest;
import com.fafeng.clinic.ai.dto.UpdateAiDraftStatusRequest;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.mapper.AiDraftMapper;
import com.fafeng.clinic.ai.model.InboundDraftLine;
import com.fafeng.clinic.ai.model.InboundDraftPayload;
import com.fafeng.clinic.ai.model.VisitDraftPayload;
import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.ai.vo.AiStatusVO;
import com.fafeng.clinic.ai.vo.ApproveInboundResultVO;
import com.fafeng.clinic.ai.vo.ApproveOutboundResultVO;
import com.fafeng.clinic.clinic.dto.SaveVisitRequest;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.inventory.dto.OutboundConfirmRequest;
import com.fafeng.clinic.inventory.dto.OutboundLineRequest;
import com.fafeng.clinic.inventory.dto.OutboundPreviewRequest;
import com.fafeng.clinic.inventory.dto.InboundRequest;
import com.fafeng.clinic.inventory.service.InventoryService;
import com.fafeng.clinic.inventory.vo.FlowVO;
import com.fafeng.clinic.inventory.vo.OutboundPreviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiDraftService {

    private static final Set<String> DRAFT_TYPES = Set.of(
            AiDraft.TYPE_INBOUND,
            AiDraft.TYPE_VISIT,
            AiDraft.TYPE_OUTBOUND,
            AiDraft.TYPE_QUERY
    );
    private static final Set<String> STATUSES = Set.of(
            AiDraft.STATUS_PENDING,
            AiDraft.STATUS_APPROVED,
            AiDraft.STATUS_REJECTED
    );

    private final AiDraftMapper draftMapper;
    private final ClinicAiProperties properties;
    private final AiProvider activeAiProvider;
    private final InventoryService inventoryService;
    private final VisitService visitService;
    private final ObjectMapper objectMapper;

    public AiDraftService(AiDraftMapper draftMapper,
                          ClinicAiProperties properties,
                          AiProvider activeAiProvider,
                          InventoryService inventoryService,
                          VisitService visitService,
                          ObjectMapper objectMapper) {
        this.draftMapper = draftMapper;
        this.properties = properties;
        this.activeAiProvider = activeAiProvider;
        this.inventoryService = inventoryService;
        this.visitService = visitService;
        this.objectMapper = objectMapper;
    }

    public AiStatusVO getStatus() {
        return new AiStatusVO(
                properties.isEnabled(),
                activeAiProvider.name(),
                activeAiProvider.isAvailable()
        );
    }

    public List<AiDraftVO> list(String draftType, String status) {
        LambdaQueryWrapper<AiDraft> wrapper = new LambdaQueryWrapper<AiDraft>()
                .orderByDesc(AiDraft::getCreatedAt);
        if (draftType != null && !draftType.isBlank()) {
            wrapper.eq(AiDraft::getDraftType, draftType.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AiDraft::getStatus, status.trim());
        }
        return draftMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public AiDraftVO get(Long id) {
        return toVO(requireDraft(id));
    }

    @Transactional
    public AiDraftVO create(CreateAiDraftRequest request) {
        String draftType = request.draftType().trim();
        validateDraftType(draftType);

        AiDraft draft = new AiDraft();
        draft.setDraftType(draftType);
        draft.setStatus(AiDraft.STATUS_PENDING);
        draft.setPayload(normalizePayload(request.payload()));
        draft.setSource(resolveSource(request.source()));
        draft.setCreatedAt(OffsetDateTime.now());
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.insert(draft);
        return toVO(draft);
    }

    @Transactional
    public AiDraftVO createStructuredDraft(String draftType, String payload) {
        return create(new CreateAiDraftRequest(draftType, payload, activeAiProvider.name()));
    }

    @Transactional
    public AiDraftVO updateStatus(Long id, UpdateAiDraftStatusRequest request) {
        AiDraft draft = requireDraft(id);
        String status = request.status().trim();
        validateStatus(status);
        if (!AiDraft.STATUS_PENDING.equals(draft.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待确认草稿可变更状态");
        }
        draft.setStatus(status);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);
        return toVO(draft);
    }

    @Transactional
    public AiDraftVO updatePayload(Long id, UpdateAiDraftPayloadRequest request) {
        AiDraft draft = requirePendingDraft(id);
        draft.setPayload(normalizePayload(request.payload()));
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);
        return toVO(draft);
    }

    @Transactional
    public ApproveInboundResultVO approveInbound(Long id) {
        AiDraft draft = requirePendingDraft(id);
        if (!AiDraft.TYPE_INBOUND.equals(draft.getDraftType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿类型不是入库");
        }

        InboundDraftPayload payload = readInboundPayload(draft.getPayload());
        List<InboundRequest> requests = payload.getLines().stream()
                .map(line -> toInboundRequest(line, payload.getSupplier(), payload.getRemark()))
                .toList();
        for (InboundRequest request : requests) {
            inventoryService.inbound(request);
        }

        draft.setStatus(AiDraft.STATUS_APPROVED);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);
        return new ApproveInboundResultVO(requests.size(), requests.size());
    }

    @Transactional
    public VisitDetailVO approveVisit(Long id, ApproveVisitDraftRequest request) {
        AiDraft draft = requirePendingDraft(id);
        if (!AiDraft.TYPE_VISIT.equals(draft.getDraftType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿类型不是病历");
        }

        VisitDraftPayload payload = readVisitPayload(draft.getPayload());
        SaveVisitRequest saveRequest = new SaveVisitRequest(
                request.patientId(),
                OffsetDateTime.now(),
                payload.getChiefComplaint(),
                payload.getPresentIllness(),
                payload.getPastHistory(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                payload.getDiagnosis(),
                payload.getTreatment(),
                payload.getRemark(),
                null,
                null
        );
        VisitDetailVO visit = visitService.create(saveRequest);

        draft.setStatus(AiDraft.STATUS_APPROVED);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);
        return visit;
    }

    @Transactional
    public ApproveOutboundResultVO approveOutbound(Long id, ApproveOutboundDraftRequest request) {
        AiDraft draft = requirePendingDraft(id);
        if (!AiDraft.TYPE_OUTBOUND.equals(draft.getDraftType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿类型不是出库");
        }

        OutboundDraftPayload payload = readOutboundPayload(draft.getPayload());
        if (payload.getPrescriptionId() == null || payload.getPatientId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "此出库草稿未关联处方，请使用库存出库页手动操作");
        }
        if (payload.getItems() == null || payload.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "出库明细为空");
        }
        if (request.lines().size() != payload.getItems().size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "出库明细行数与草稿不一致");
        }

        Set<Long> medicineIds = new HashSet<>();
        List<OutboundLineRequest> previewItems = request.lines().stream()
                .map(line -> {
                    if (!medicineIds.add(line.medicineId())) {
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "同一药品请勿重复添加，请合并数量");
                    }
                    return new OutboundLineRequest(line.medicineId(), line.quantity(), line.unit());
                })
                .toList();

        OutboundPreviewVO preview = inventoryService.previewOutbound(new OutboundPreviewRequest(
                payload.getPatientId(), payload.getPrescriptionId(), previewItems));
        if (!preview.sufficient()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "部分药品库存不足，整单无法出库");
        }

        int flowCount = 0;
        for (ApproveOutboundLineRequest line : request.lines()) {
            inventoryService.validateOutboundAllocationTotals(
                    line.medicineId(), line.quantity(), line.unit(), line.allocations());
            List<FlowVO> flows = inventoryService.confirmOutbound(new OutboundConfirmRequest(
                    payload.getPatientId(),
                    payload.getPrescriptionId(),
                    line.medicineId(),
                    line.allocations()));
            flowCount += flows.size();
        }

        draft.setStatus(AiDraft.STATUS_APPROVED);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);
        return new ApproveOutboundResultVO(payload.getPrescriptionId(), request.lines().size(), flowCount);
    }

    private InboundRequest toInboundRequest(InboundDraftLine line, String supplier, String remark) {
        if (line.getMedicineId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择药品");
        }
        if (line.getBatchNo() == null || line.getBatchNo().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "批号不能为空");
        }
        BigDecimal quantity = parseDecimal(line.getQuantity(), "数量");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数量必须大于 0");
        }
        return new InboundRequest(
                line.getMedicineId(),
                quantity,
                blankToDefault(line.getUnit(), "盒"),
                line.getBatchNo().trim(),
                parseDate(line.getExpiryDate()),
                parseOptionalDecimal(line.getPurchasePrice()),
                supplier,
                remark
        );
    }

    private AiDraft requireDraft(Long id) {
        AiDraft draft = draftMapper.selectById(id);
        if (draft == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 草稿不存在");
        }
        return draft;
    }

    private AiDraft requirePendingDraft(Long id) {
        AiDraft draft = requireDraft(id);
        if (!AiDraft.STATUS_PENDING.equals(draft.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待确认草稿可执行此操作");
        }
        return draft;
    }

    private InboundDraftPayload readInboundPayload(String payloadJson) {
        try {
            InboundDraftPayload payload = objectMapper.readValue(payloadJson, InboundDraftPayload.class);
            if (payload.getLines() == null || payload.getLines().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "入库明细为空");
            }
            return payload;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "入库草稿格式无效");
        }
    }

    private VisitDraftPayload readVisitPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, VisitDraftPayload.class);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "病历草稿格式无效");
        }
    }

    private OutboundDraftPayload readOutboundPayload(String payloadJson) {
        try {
            OutboundDraftPayload payload = objectMapper.readValue(payloadJson, OutboundDraftPayload.class);
            if (payload.getItems() == null || payload.getItems().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "出库明细为空");
            }
            return payload;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "出库草稿格式无效");
        }
    }

    private BigDecimal parseDecimal(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "格式无效");
        }
    }

    private BigDecimal parseOptionalDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private void validateDraftType(String draftType) {
        if (!DRAFT_TYPES.contains(draftType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿类型无效");
        }
    }

    private void validateStatus(String status) {
        if (!STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿状态无效");
        }
    }

    private String normalizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return "{}";
        }
        return payload.trim();
    }

    private String resolveSource(String source) {
        if (source != null && !source.isBlank()) {
            return source.trim();
        }
        return activeAiProvider.name();
    }

    private AiDraftVO toVO(AiDraft draft) {
        return new AiDraftVO(
                draft.getId(),
                draft.getDraftType(),
                draft.getStatus(),
                draft.getPayload(),
                draft.getSource(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}
