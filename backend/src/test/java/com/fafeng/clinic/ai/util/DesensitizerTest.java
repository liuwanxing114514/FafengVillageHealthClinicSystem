package com.fafeng.clinic.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesensitizerTest {

    @Test
    void maskNameExamples() {
        assertEquals("张*", Desensitizer.maskName("张三"));
        assertEquals("张**", Desensitizer.maskName("张三丰"));
        assertEquals("欧阳**", Desensitizer.maskName("欧阳娜娜"));
    }

    @Test
    void maskPhoneExamples() {
        assertEquals("138****5678", Desensitizer.maskPhone("13812345678"));
        assertEquals("0571-****5678", Desensitizer.maskPhone("0571-12345678"));
    }

    @Test
    void maskIdCardExample() {
        assertEquals("330102********1234", Desensitizer.maskIdCard("330102199001011234"));
    }

    @Test
    void maskAddressExample() {
        assertEquals("发凤村3组**号", Desensitizer.maskAddress("发凤村3组12号"));
        assertEquals("XX路**号***室", Desensitizer.maskAddress("XX路88号301室"));
    }

    @Test
    void desensitizeMixedTextWithPatientContext() {
        String text = "患者张三，电话13812345678，住发凤村3组12号。主诉：咳嗽3天。";
        Desensitizer.PatientContext context = Desensitizer.PatientContext.of(
                "张三", "13812345678", "发凤村3组12号", "330102199001011234");
        String result = Desensitizer.desensitizeText(text, context);
        assertEquals("患者张*，电话138****5678，住发凤村3组**号。主诉：咳嗽3天。", result);
    }
}
