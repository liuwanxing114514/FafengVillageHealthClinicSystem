package com.fafeng.clinic.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void desensitizeInboundDocumentPreservesMedicineLines() {
        String input = """
                联系人：张三 电话13812345678
                地址：发凤村3组12号
                阿莫西林胶囊 0.25g*24粒 数量100 批号13812345678 单价12.5
                金额1250.00 有效期2026-12""";
        String result = Desensitizer.desensitizeInboundDocument(input);
        assertTrue(result.contains("联系人：张三 电话138****5678"));
        assertTrue(result.contains("地址：发凤村3组**号"));
        assertTrue(result.contains("阿莫西林胶囊 0.25g*24粒 数量100 批号13812345678 单价12.5"));
        assertTrue(result.contains("金额1250.00 有效期2026-12"));
    }
}
