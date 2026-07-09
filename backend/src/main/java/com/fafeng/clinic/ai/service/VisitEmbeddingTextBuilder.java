package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.patient.entity.Patient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 拼接病历字段并脱敏，供向量化出站使用。
 */
@Component
public class VisitEmbeddingTextBuilder {

    public String buildDesensitizedSummary(ClinicVisit visit, Patient patient) {
        if (visit == null) {
            return "";
        }
        String raw = joinSections(visit);
        return desensitize(raw, patient);
    }

    /**
     * 相似检索查询文本：仅主诉、现病史、诊断（v2.3）。
     */
    public String buildDesensitizedSearchQuery(String chiefComplaint,
                                               String presentIllness,
                                               String diagnosis,
                                               Patient patient) {
        List<String> sections = new ArrayList<>();
        append(sections, "主诉", chiefComplaint);
        append(sections, "现病史", presentIllness);
        append(sections, "诊断", diagnosis);
        return desensitize(String.join("\n", sections), patient);
    }

    private String desensitize(String raw, Patient patient) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        Desensitizer.PatientContext context = patient == null
                ? Desensitizer.PatientContext.empty()
                : Desensitizer.PatientContext.of(
                patient.getName(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getIdCard());
        return Desensitizer.desensitizeText(raw, context);
    }

    private String joinSections(ClinicVisit visit) {
        List<String> sections = new ArrayList<>();
        append(sections, "主诉", visit.getChiefComplaint());
        append(sections, "现病史", visit.getPresentIllness());
        append(sections, "既往史", visit.getPastHistory());
        append(sections, "过敏史", visit.getAllergyHistory());
        appendVitals(sections, visit);
        append(sections, "诊断", visit.getDiagnosis());
        append(sections, "处理意见", visit.getTreatment());
        append(sections, "备注", visit.getRemark());
        return String.join("\n", sections);
    }

    private void appendVitals(List<String> sections, ClinicVisit visit) {
        List<String> vitals = new ArrayList<>();
        appendInline(vitals, "体温", formatDecimal(visit.getTemperature()));
        appendInline(vitals, "血压", visit.getBloodPressure());
        appendInline(vitals, "血氧", formatDecimal(visit.getSpo2()));
        appendInline(vitals, "呼末二氧化碳", formatDecimal(visit.getEtco2()));
        appendInline(vitals, "心率", visit.getHeartRate() == null ? null : visit.getHeartRate().toString());
        appendInline(vitals, "脉象", visit.getPulse());
        if (!vitals.isEmpty()) {
            sections.add("生命体征：" + String.join("；", vitals));
        }
    }

    private void append(List<String> sections, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sections.add(label + "：" + value.trim());
    }

    private void appendInline(List<String> parts, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        parts.add(label + " " + value.trim());
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
