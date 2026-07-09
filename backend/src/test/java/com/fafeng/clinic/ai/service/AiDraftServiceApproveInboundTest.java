package com.fafeng.clinic.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.model.InboundDraftLine;
import com.fafeng.clinic.ai.model.InboundDraftPayload;
import com.fafeng.clinic.ai.vo.ApproveInboundResultVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.inventory.dto.InboundRequest;
import com.fafeng.clinic.inventory.service.InventoryService;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.mapper.AiDraftMapper;
import com.fafeng.clinic.ai.provider.AiProvider;
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
class AiDraftServiceApproveInboundTest {

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
    void approveInboundRequiresMedicineId() throws Exception {
        AiDraft draft = pendingInboundDraft();
        when(draftMapper.selectById(1L)).thenReturn(draft);

        InboundDraftPayload payload = new InboundDraftPayload();
        InboundDraftLine line = new InboundDraftLine();
        line.setMedicineName("阿莫西林");
        line.setQuantity("10");
        line.setUnit("盒");
        line.setBatchNo("B001");
        payload.setLines(List.of(line));
        when(objectMapper.readValue(draft.getPayload(), InboundDraftPayload.class)).thenReturn(payload);

        assertThrows(BusinessException.class, () -> aiDraftService.approveInbound(1L));
        verify(inventoryService, never()).inbound(any());
    }

    @Test
    void approveInboundCallsInventoryService() throws Exception {
        AiDraft draft = pendingInboundDraft();
        when(draftMapper.selectById(2L)).thenReturn(draft);

        InboundDraftPayload payload = new InboundDraftPayload();
        payload.setSupplier("测试供应商");
        InboundDraftLine line = new InboundDraftLine();
        line.setMedicineId(100L);
        line.setMedicineName("阿莫西林");
        line.setQuantity("10");
        line.setUnit("盒");
        line.setBatchNo("B001");
        payload.setLines(List.of(line));
        when(objectMapper.readValue(draft.getPayload(), InboundDraftPayload.class)).thenReturn(payload);

        ApproveInboundResultVO result = aiDraftService.approveInbound(2L);

        ArgumentCaptor<InboundRequest> captor = ArgumentCaptor.forClass(InboundRequest.class);
        verify(inventoryService).inbound(captor.capture());
        assertEquals(100L, captor.getValue().medicineId());
        assertEquals(new BigDecimal("10"), captor.getValue().quantity());
        assertEquals("B001", captor.getValue().batchNo());
        assertEquals(1, result.successCount());
        assertEquals(AiDraft.STATUS_APPROVED, draft.getStatus());
    }

    private AiDraft pendingInboundDraft() throws Exception {
        AiDraft draft = new AiDraft();
        draft.setId(1L);
        draft.setDraftType(AiDraft.TYPE_INBOUND);
        draft.setStatus(AiDraft.STATUS_PENDING);
        draft.setPayload(new ObjectMapper().writeValueAsString(new InboundDraftPayload()));
        return draft;
    }
}
