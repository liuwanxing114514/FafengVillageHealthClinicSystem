package com.fafeng.clinic.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinyinUtilsTest {

    @Test
    void toAbbr_extractsChineseInitials() {
        assertEquals("amxljn", PinyinUtils.toAbbr("阿莫西林胶囊"));
        assertEquals("gml", PinyinUtils.toAbbr("感冒灵"));
    }

    @Test
    void toAbbr_handlesMixedText() {
        String abbr = PinyinUtils.toAbbr("维生素B12");
        assertTrue(abbr.startsWith("wss"));
    }

    @Test
    void toAbbr_emptyInput() {
        assertEquals("", PinyinUtils.toAbbr(""));
        assertEquals("", PinyinUtils.toAbbr(null));
    }
}
