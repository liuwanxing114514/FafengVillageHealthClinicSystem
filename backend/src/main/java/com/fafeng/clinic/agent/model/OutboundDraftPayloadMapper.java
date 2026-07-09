package com.fafeng.clinic.agent.model;

import com.fafeng.clinic.clinic.vo.OutboundDraftItemVO;
import com.fafeng.clinic.clinic.vo.OutboundDraftVO;

import java.math.BigDecimal;

public final class OutboundDraftPayloadMapper {

    private OutboundDraftPayloadMapper() {
    }

    public static OutboundDraftPayload fromPrescriptionDraft(OutboundDraftVO draft, String remark) {
        OutboundDraftPayload payload = new OutboundDraftPayload();
        payload.setPrescriptionId(draft.prescriptionId());
        payload.setPatientId(draft.patientId());
        payload.setPatientName(draft.patientName());
        payload.setDiagnosis(draft.diagnosis());
        payload.setRemark(remark);
        for (OutboundDraftItemVO item : draft.items()) {
            payload.getItems().add(toItem(
                    item.medicineId(),
                    item.medicineName(),
                    item.specification(),
                    item.quantity(),
                    item.unit(),
                    item.usage()));
        }
        return payload;
    }

    public static OutboundDraftPayload.OutboundDraftItem toItem(Long medicineId,
                                                                 String name,
                                                                 String spec,
                                                                 BigDecimal quantity,
                                                                 String unit,
                                                                 String usage) {
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
