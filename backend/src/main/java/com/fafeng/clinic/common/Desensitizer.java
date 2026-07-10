package com.fafeng.clinic.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 出站 AI API 请求前的隐私脱敏。数据库与确认页仍使用原文。
 */
public final class Desensitizer {

    private static final Pattern MOBILE = Pattern.compile("(?<![0-9])1[3-9]\\d{9}(?![0-9])");
    private static final Pattern LANDLINE = Pattern.compile("(?<![0-9])0\\d{2,3}-\\d{7,8}(?![0-9])");
    private static final Pattern ID_CARD = Pattern.compile("(?<![0-9])[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx](?![0-9])");
    private static final Pattern ADDRESS_DETAIL = Pattern.compile(
            "([\\u4e00-\\u9fa5A-Za-z0-9]+(?:村|组|路|街|巷|道|里|弄|区|镇|乡|县|市|省))([0-9]+(?:号|栋|单元|室|楼)?[^\\s，,。；;]*)");
    private static final Pattern INBOUND_SENSITIVE_LINE = Pattern.compile(
            "联系人|电话|手机|地址|传真|邮编|单位|供货|供应商|联系");

    private Desensitizer() {
    }

    /**
     * 进货单 OCR 专用：仅对含供应商联系人/电话/地址等关键词的行脱敏，药品表格行原文保留。
     */
    public static String desensitizeInboundDocument(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String normalized = text.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            String line = lines[i];
            if (INBOUND_SENSITIVE_LINE.matcher(line).find()) {
                sb.append(desensitizeText(line));
            } else {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    public static String desensitizeText(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String result = text;
        result = desensitizeIdCards(result);
        result = desensitizeMobiles(result);
        result = desensitizeLandlines(result);
        result = desensitizeAddresses(result);
        return result;
    }

    public static String desensitizeName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 1) {
            return trimmed;
        }
        int surnameLen = isCompoundSurname(trimmed) ? 2 : 1;
        return trimmed.substring(0, surnameLen) + "*".repeat(trimmed.length() - surnameLen);
    }

    public static String desensitizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return mobile;
        }
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return mobile;
        }
        return digits.substring(0, 3) + "****" + digits.substring(7);
    }

    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return idCard;
        }
        String trimmed = idCard.trim();
        if (trimmed.length() != 18) {
            return trimmed;
        }
        return trimmed.substring(0, 6) + "********" + trimmed.substring(14);
    }

    public static String desensitizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }
        Matcher matcher = ADDRESS_DETAIL.matcher(address);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String detail = matcher.group(2);
            String masked = prefix + maskAddressDetail(detail);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String desensitizeIdCards(String text) {
        Matcher matcher = ID_CARD.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(desensitizeIdCard(matcher.group())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String desensitizeMobiles(String text) {
        Matcher matcher = MOBILE.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(desensitizeMobile(matcher.group())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String desensitizeLandlines(String text) {
        Matcher matcher = LANDLINE.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String line = matcher.group();
            int dash = line.indexOf('-');
            String masked = line.substring(0, dash + 1) + "****" + line.substring(line.length() - 4);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String desensitizeAddresses(String text) {
        Matcher matcher = ADDRESS_DETAIL.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String detail = matcher.group(2);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + maskAddressDetail(detail)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String maskAddressDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return "**号";
        }
        return detail.replaceAll("[0-9]", "*");
    }

    private static boolean isCompoundSurname(String name) {
        if (name.length() < 2) {
            return false;
        }
        String two = name.substring(0, 2);
        return "欧阳".equals(two) || "司马".equals(two) || "上官".equals(two) || "诸葛".equals(two)
                || "东方".equals(two) || "皇甫".equals(two) || "尉迟".equals(two) || "公孙".equals(two);
    }
}
