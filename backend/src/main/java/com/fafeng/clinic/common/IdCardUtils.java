package com.fafeng.clinic.common;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public final class IdCardUtils {

    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final int[] CHECKSUM_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECKSUM_CHARS = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private IdCardUtils() {
    }

    public static boolean isValid(String idCard) {
        if (idCard == null) {
            return false;
        }
        String normalized = idCard.trim().toUpperCase();
        if (!ID_CARD_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Character.digit(normalized.charAt(i), 10) * CHECKSUM_WEIGHTS[i];
        }
        char expected = CHECKSUM_CHARS[sum % 11];
        return normalized.charAt(17) == expected;
    }

    public static LocalDate parseBirthDate(String idCard) {
        if (!isValid(idCard)) {
            return null;
        }
        String normalized = idCard.trim().toUpperCase();
        int year = Integer.parseInt(normalized.substring(6, 10));
        int month = Integer.parseInt(normalized.substring(10, 12));
        int day = Integer.parseInt(normalized.substring(12, 14));
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static int calculateAge(LocalDate birthDate, LocalDate referenceDate) {
        if (birthDate == null || referenceDate == null) {
            return 0;
        }
        return Math.max(Period.between(birthDate, referenceDate).getYears(), 0);
    }
}
