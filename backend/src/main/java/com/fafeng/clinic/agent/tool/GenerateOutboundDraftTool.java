package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.agent.model.OutboundDraftPayload;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.service.AiDraftService;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.clinic.service.PrescriptionService;
import com.fafeng.clinic.clinic.vo.OutboundDraftItemVO;
import com.fafeng.clinic.clinic.vo.OutboundDraftVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.medicine.service.MedicineService;
import com.fafeng.clinic.medicine.vo.MedicineDetailVO;
import com.fafeng.clinic.medicine.vo.MedicineListItemVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GenerateOutboundDraftTool implements AgentTool {

    private final PrescriptionService prescriptionService;
    private final MedicineService medicineService;
    private final AiDraftService aiDraftService;
    private final ObjectMapper objectMapper;

    public GenerateOutboundDraftTool(PrescriptionService prescriptionService,
                                     MedicineService medicineService,
                                     AiDraftService aiDraftService,
                                     ObjectMapper objectMapper) {
        this.prescriptionService = prescriptionService;
        this.medicineService = medicineService;
        this.aiDraftService = aiDraftService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return AgentToolName.GENERATE_OUTBOUND_DRAFT;
    }

    @Override
    public String description() {
        return "生成待确认出库清单（写入草稿，不扣库存）。参数：prescriptionId 或 medicineName+quantity（+unit 可选）";
    }

    @Override
    public AgentToolResult execute(JsonNode args) {
        Long prescriptionId = SearchMedicineTool.longArg(args, "prescriptionId");
        try {
            OutboundDraftPayload payload;
            if (prescriptionId != null) {
                payload = fromPrescription(prescriptionId);
            } else {
                payload = fromMedicineRequest(args);
            }

            String payloadJson = objectMapper.writeValueAsString(payload);
            AiDraftVO draft = aiDraftService.createStructuredDraft(AiDraft.TYPE_OUTBOUND, payloadJson);

            ObjectNode data = objectMapper.createObjectNode();
            data.put("draftId", draft.id());
            data.put("draftType", AiDraft.TYPE_OUTBOUND);
            data.put("itemCount", payload.getItems().size());

            String summary = "已生成待确认出库清单（草稿 #" + draft.id() + "），共 "
                    + payload.getItems().size() + " 种药品，请医生确认后才扣库存";
            return AgentToolResult.okWithDraft(data, summary, draft.id());
        } catch (BusinessException ex) {
            return AgentToolResult.fail(ex.getMessage());
        } catch (Exception ex) {
            return AgentToolResult.fail("出库草稿生成失败");
        }
    }

    private OutboundDraftPayload fromPrescription(Long prescriptionId) {
        OutboundDraftVO draft = prescriptionService.generateOutboundDraft(prescriptionId);
        OutboundDraftPayload payload = new OutboundDraftPayload();
        payload.setPrescriptionId(draft.prescriptionId());
        payload.setPatientId(draft.patientId());
        payload.setPatientName(draft.patientName());
        payload.setDiagnosis(draft.diagnosis());
        payload.setRemark("由处方 #" + prescriptionId + " 生成");
        for (OutboundDraftItemVO item : draft.items()) {
            payload.getItems().add(toItem(item.medicineId(), item.medicineName(), item.specification(),
                    item.quantity(), item.unit(), item.usage()));
        }
        return payload;
    }

    private OutboundDraftPayload fromMedicineRequest(JsonNode args) {
        String medicineName = SearchMedicineTool.textArg(args, "medicineName");
        String quantityText = SearchMedicineTool.textArg(args, "quantity");
        if (medicineName == null || medicineName.isBlank()) {
            throw new BusinessException(com.fafeng.clinic.common.ErrorCode.BAD_REQUEST, "请提供 medicineName 或 prescriptionId");
        }
        if (quantityText == null || quantityText.isBlank()) {
            throw new BusinessException(com.fafeng.clinic.common.ErrorCode.BAD_REQUEST, "请提供出库数量 quantity");
        }

        PageVO<MedicineListItemVO> search = medicineService.search(medicineName, "ACTIVE", 1, 5);
        if (search.records().isEmpty()) {
            throw new BusinessException(com.fafeng.clinic.common.ErrorCode.NOT_FOUND, "未找到药品：" + medicineName);
        }
        if (search.total() > 1) {
            throw new BusinessException(com.fafeng.clinic.common.ErrorCode.BAD_REQUEST, "找到多种药品，请指定更精确的名称");
        }

        MedicineDetailVO medicine = medicineService.getDetail(search.records().getFirst().id());
        BigDecimal quantity = new BigDecimal(quantityText.trim());
        String unit = SearchMedicineTool.textArg(args, "unit");
        if (unit == null || unit.isBlank()) {
            unit = medicine.baseUnit();
        }

        OutboundDraftPayload payload = new OutboundDraftPayload();
        payload.setRemark("Agent 请求出库：" + medicine.name());
        payload.getItems().add(toItem(medicine.id(), medicine.name(), medicine.specification(),
                quantity, unit, null));
        return payload;
    }

    private OutboundDraftPayload.OutboundDraftItem toItem(Long medicineId, String name, String spec,
                                                            BigDecimal quantity, String unit, String usage) {
        OutboundDraftPayload.OutboundDraftItem item = new OutboundDraftPayload.OutboundDraftItem();
        item.setMedicineId(medicineId);
        item.setMedicineName(name);
        item.setSpecification(spec);
        item.setQuantity(quantity.toPlainString());
        item.setUnit(unit);
        item.setUsage(usage);
        return item;
    }
}
