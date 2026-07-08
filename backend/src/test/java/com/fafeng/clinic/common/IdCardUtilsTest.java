package com.fafeng.clinic.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdCardUtilsTest {

    @Test
    void rejectsInvalidIdCard() {
        assertFalse(IdCardUtils.isValid(null));
        assertFalse(IdCardUtils.isValid(""));
        assertFalse(IdCardUtils.isValid("123456789012345678"));
    }

    @Test
    void parsesBirthDateFromValidIdCard() {
        String idCard = "110101199003075517";
        assertTrue(IdCardUtils.isValid(idCard));
        assertEquals(LocalDate.of(1990, 3, 7), IdCardUtils.parseBirthDate(idCard));
    }

    @Test
    void calculatesAgeInYears() {
        LocalDate birthDate = LocalDate.of(1990, 3, 7);
        assertEquals(36, IdCardUtils.calculateAge(birthDate, LocalDate.of(2026, 7, 8)));
    }
}
