package com.fafeng.clinic.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesensitizerTest {

    @Test
    void desensitizeName() {
        assertEquals("张*", Desensitizer.desensitizeName("张三"));
        assertEquals("张**", Desensitizer.desensitizeName("张三丰"));
        assertEquals("欧阳**", Desensitizer.desensitizeName("欧阳娜娜"));
    }

    @Test
    void desensitizeMobile() {
        assertEquals("138****5678", Desensitizer.desensitizeMobile("13812345678"));
    }

    @Test
    void desensitizeIdCard() {
        assertEquals("330102********1234", Desensitizer.desensitizeIdCard("330102199001011234"));
    }

    @Test
    void desensitizeAddress() {
        assertEquals("发凤村3组**号", Desensitizer.desensitizeAddress("发凤村3组12号"));
    }

    @Test
    void desensitizeMixedText() {
        String input = "患者张三，电话13812345678，住发凤村3组12号。主诉：咳嗽3天。";
        String expected = "患者张三，电话138****5678，住发凤村3组**号。主诉：咳嗽3天。";
        assertEquals(expected, Desensitizer.desensitizeText(input));
    }
}
