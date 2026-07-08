package com.fafeng.clinic.ai.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 出站 AI API 前脱敏。数据库与确认页仍使用原文。
 */
public final class Desensitizer {

    private static final Set<String> COMPOUND_SURNAMES = Set.of(
            "欧阳", "司马", "上官", "诸葛", "东方", "皇甫", "尉迟", "公孙", "慕容", "司徒",
            "司空", "夏侯", "长孙", "宇文", "令狐", "钟离", "闾丘", "子车", "亓官", "司寇"
    );

    private static final Pattern MOBILE_PATTERN = Pattern.compile("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)");
    private static final Pattern LANDLINE_PATTERN = Pattern.compile("(0\\d{2,3}-?)\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<!\\d)(\\d{6})\\d{8}(\\d{3}[0-9Xx])(?!\\d)");
    private static final Pattern ADDRESS_NUMBER_PATTERN = Pattern.compile("(\\d+)(?=号)");
    private static final Pattern ADDRESS_ROOM_PATTERN = Pattern.compile("(\\d+)(?=室)");
    private static final Pattern ADDRESS_BUILDING_PATTERN = Pattern.compile("(\\d+)(?=栋|单元)");

    private Desensitizer() {
    }

    public static String desensitizeText(String text, PatientContext context) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String result = text;
        if (context != null) {
            result = replaceLiteral(result, context.name(), maskName(context.name()));
            result = replaceLiteral(result, context.phone(), maskPhone(context.phone()));
            result = replaceLiteral(result, context.address(), maskAddress(context.address()));
            result = replaceLiteral(result, context.idCard(), maskIdCard(context.idCard()));
        }
        result = maskIdCards(result);
        result = maskMobilePhones(result);
        result = maskLandlinePhones(result);
        result = maskAddresses(result);
        return result;
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String trimmed = name.trim();
        if (trimmed.length() == 1) {
            return trimmed;
        }
        String surname = trimmed.substring(0, 1);
        for (String compound : COMPOUND_SURNAMES) {
            if (trimmed.startsWith(compound) && trimmed.length() > compound.length()) {
                surname = compound;
                break;
            }
        }
        int givenLength = trimmed.length() - surname.length();
        return surname + "*".repeat(Math.max(givenLength, 1));
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("1")) {
            return digits.substring(0, 3) + "****" + digits.substring(7);
        }
        if (digits.length() >= 7) {
            return maskLandlinePhones(phone);
        }
        return phone;
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return idCard;
        }
        String trimmed = idCard.trim();
        if (trimmed.length() != 18) {
            return trimmed;
        }
        return trimmed.substring(0, 6) + "********" + trimmed.substring(14);
    }

    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }
        String result = ADDRESS_NUMBER_PATTERN.matcher(address).replaceAll("**");
        result = ADDRESS_ROOM_PATTERN.matcher(result).replaceAll("***");
        result = ADDRESS_BUILDING_PATTERN.matcher(result).replaceAll("**");
        return result;
    }

    private static String maskMobilePhones(String text) {
        Matcher matcher = MOBILE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, matcher.group(1) + "****" + matcher.group(2));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskLandlinePhones(String text) {
        Matcher matcher = LANDLINE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, matcher.group(1) + "****" + matcher.group(2));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskIdCards(String text) {
        Matcher matcher = ID_CARD_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, matcher.group(1) + "********" + matcher.group(2));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskAddresses(String text) {
        return maskAddress(text);
    }

    private static String replaceLiteral(String text, String literal, String masked) {
        if (literal == null || literal.isBlank() || masked == null) {
            return text;
        }
        return text.replace(literal, masked);
    }

    public record PatientContext(String name, String phone, String address, String idCard) {
        public static PatientContext empty() {
            return new PatientContext(null, null, null, null);
        }

        public static PatientContext of(String name, String phone, String address, String idCard) {
            Set<String> values = new LinkedHashSet<>();
            if (name != null && !name.isBlank()) {
                values.add(name.trim());
            }
            if (phone != null && !phone.isBlank()) {
                values.add(phone.trim());
            }
            if (address != null && !address.isBlank()) {
                values.add(address.trim());
            }
            if (idCard != null && !idCard.isBlank()) {
                values.add(idCard.trim());
            }
            return new PatientContext(name, phone, address, idCard);
        }
    }
}
