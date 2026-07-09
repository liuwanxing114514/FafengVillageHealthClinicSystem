package com.fafeng.clinic.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.agent.model.OutboundDraftPayload;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.dto.ApproveOutboundDraftRequest;
import com.fafeng.clinic.ai.dto.ApproveOutboundLineRequest;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.mapper.AiDraftMapper;
import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.vo.ApproveOutboundResultVO;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.inventory.dto.OutboundAllocationRequest;
import com.fafeng.clinic.inventory.dto.OutboundConfirmRequest;
import com.fafeng.clinic.inventory.dto.OutboundPreviewRequest;
import com.fafeng.clinic.inventory.service.InventoryService;
import com.fafeng.clinic.inventory.vo.FlowVO;
import com.fafeng.clinic.inventory.vo.OutboundPreviewLineVO;
import com.fafeng.clinic.inventory.vo.OutboundPreviewVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiDraftServiceApproveOutboundTest {

    @Mock
    private AiDraftMapper draftMapper;
    @Mock
    private ClinicAiProperties properties;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private VisitService visitService;
    @Mock
    private AiProvider activeAiProvider;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiDraftService aiDraftService;

    @Test
    void approveOutboundRequiresPrescriptionContext() throws Exception {
        AiDraft draft = pendingOutboundDraft();
        when(draftMapper.selectById(1L)).thenReturn(draft);

        OutboundDraftPayload payload = new OutboundDraftPayload();
        OutboundDraftPayload.OutboundDraftItem item = new OutboundDraftPayload.OutboundDraftItem();
        item.setMedicineId(100L);
        item.setQuantity("10");
        item.setUnit("盒");
        payload.setItems(List.of(item));
        when(objectMapper.readValue(draft.getPayload(), OutboundDraftPayload.class)).thenReturn(payload);

        ApproveOutboundDraftRequest request = new ApproveOutboundDraftRequest(List.of(
                new ApproveOutboundLineRequest(100L, new BigDecimal("10"), "盒",
                        List.of(new OutboundAllocationRequest(1L, new BigDecimal("10"))))));

        assertThrows(BusinessException.class, () -> aiDraftService.approveOutbound(1L, request));
        verify(inventoryService, never()).confirmOutbound(any());
    }

    @Test
    void approveOutboundRejectsInsufficientStock() throws Exception {
        AiDraft draft = pendingOutboundDraft();
        when(draftMapper.selectById(2L)).thenReturn(draft);

        OutboundDraftPayload payload = prescriptionPayload();
        when(objectMapper.readValue(draft.getPayload(), OutboundDraftPayload.class)).thenReturn(payload);
        when(inventoryService.previewOutbound(any(OutboundPreviewRequest.class)))
                .thenReturn(new OutboundPreviewVO(false, List.of()));

        ApproveOutboundDraftRequest request = outboundRequest();

        assertThrows(BusinessException.class, () -> aiDraftService.approveOutbound(2L, request));
        verify(inventoryService, never()).confirmOutbound(any());
        assertEquals(AiDraft.STATUS_PENDING, draft.getStatus());
    }

    @Test
    void approveOutboundCallsInventoryService() throws Exception {
        AiDraft draft = pendingOutboundDraft();
        draft.setId(3L);
        when(draftMapper.selectById(3L)).thenReturn(draft);

        OutboundDraftPayload payload = prescriptionPayload();
        when(objectMapper.readValue(draft.getPayload(), OutboundDraftPayload.class)).thenReturn(payload);
        when(inventoryService.previewOutbound(any(OutboundPreviewRequest.class)))
                .thenReturn(new OutboundPreviewVO(true, List.of(
                        new OutboundPreviewLineVO(100L, "测试胶囊", new BigDecimal("24"), "粒",
                                new BigDecimal("24"), "粒", true, List.of()))));
        when(inventoryService.confirmOutbound(any(OutboundConfirmRequest.class)))
                .thenReturn(List.of(new FlowVO(
                        1L, 100L, "测试胶囊", 1L, "B001", "OUTBOUND",
                        new BigDecimal("-24"), new BigDecimal("100"), new BigDecimal("76"),
                        "粒", 10L, 20L, null, null, "admin", null)));

        ApproveOutboundResultVO result = aiDraftService.approveOutbound(3L, outboundRequest());

        ArgumentCaptor<OutboundConfirmRequest> captor = ArgumentCaptor.forClass(OutboundConfirmRequest.class);
        verify(inventoryService).confirmOutbound(captor.capture());
        assertEquals(20L, captor.getValue().prescriptionId());
        assertEquals(10L, captor.getValue().patientId());
        assertEquals(100L, captor.getValue().medicineId());
        assertEquals(20L, result.prescriptionId());
        assertEquals(1, result.lineCount());
        assertEquals(AiDraft.STATUS_APPROVED, draft.getStatus());
    }

    private ApproveOutboundDraftRequest outboundRequest() {
        return new ApproveOutboundDraftRequest(List.of(
                new ApproveOutboundLineRequest(100L, new BigDecimal("24"), "粒",
                        List.of(new OutboundAllocationRequest(1L, new BigDecimal("24"))))));
    }

    private OutboundDraftPayload prescriptionPayload() {
        OutboundDraftPayload payload = new OutboundDraftPayload();
        payload.setPrescriptionId(20L);
        payload.setPatientId(10L);
        OutboundDraftPayload.OutboundDraftItem item = new OutboundDraftPayload.OutboundDraftItem();
        item.setMedicineId(100L);
        item.setQuantity("24");
        item.setUnit("粒");
        payload.setItems(List.of(item));
        return payload;
    }

    private AiDraft pendingOutboundDraft() throws Exception {
        AiDraft draft = new AiDraft();
        draft.setId(1L);
        draft.setDraftType(AiDraft.TYPE_OUTBOUND);
        draft.setStatus(AiDraft.STATUS_PENDING);
        draft.setPayload(new ObjectMapper().writeValueAsString(new OutboundDraftPayload()));
        return draft;
    }
}
