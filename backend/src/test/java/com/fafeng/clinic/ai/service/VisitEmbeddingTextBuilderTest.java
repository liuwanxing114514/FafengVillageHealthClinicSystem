package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.patient.entity.Patient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitEmbeddingTextBuilderTest {

    private final VisitEmbeddingTextBuilder builder = new VisitEmbeddingTextBuilder();

    @Test
    void buildDesensitizedSummary_masksPatientContextAndJoinsFields() {
        ClinicVisit visit = new ClinicVisit();
        visit.setChiefComplaint("咳嗽3天");
        visit.setPresentIllness("患者张三电话13812345678");
        visit.setDiagnosis("上呼吸道感染");
        visit.setTemperature(new BigDecimal("37.5"));
        visit.setRemark("随访");

        Patient patient = new Patient();
        patient.setName("张三");
        patient.setPhone("13812345678");
        patient.setAddress("发凤村3组12号");
        patient.setIdCard("330102199001011234");

        String summary = builder.buildDesensitizedSummary(visit, patient);

        assertTrue(summary.contains("主诉：咳嗽3天"));
        assertTrue(summary.contains("诊断：上呼吸道感染"));
        assertTrue(summary.contains("体温 37.5"));
        assertFalse(summary.contains("张三"));
        assertTrue(summary.contains("张*"));
        assertTrue(summary.contains("138****5678"));
        assertFalse(summary.contains("330102199001011234"));
    }

    @Test
    void buildDesensitizedSummary_returnsEmptyWhenNoContent() {
        ClinicVisit visit = new ClinicVisit();
        visit.setUpdatedAt(OffsetDateTime.now());
        assertTrue(builder.buildDesensitizedSummary(visit, null).isBlank());
    }
}
