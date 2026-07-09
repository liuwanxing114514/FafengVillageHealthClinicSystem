package com.fafeng.clinic.agent.model;

import java.util.ArrayList;
import java.util.List;

public class OutboundDraftPayload {

    private Long prescriptionId;
    private Long patientId;
    private String patientName;
    private String diagnosis;
    private String remark;
    private List<OutboundDraftItem> items = new ArrayList<>();

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<OutboundDraftItem> getItems() {
        return items;
    }

    public void setItems(List<OutboundDraftItem> items) {
        this.items = items;
    }

    public static class OutboundDraftItem {
        private Long medicineId;
        private String medicineName;
        private String specification;
        private String quantity;
        private String unit;
        private String usage;

        public Long getMedicineId() {
            return medicineId;
        }

        public void setMedicineId(Long medicineId) {
            this.medicineId = medicineId;
        }

        public String getMedicineName() {
            return medicineName;
        }

        public void setMedicineName(String medicineName) {
            this.medicineName = medicineName;
        }

        public String getSpecification() {
            return specification;
        }

        public void setSpecification(String specification) {
            this.specification = specification;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }
    }
}
