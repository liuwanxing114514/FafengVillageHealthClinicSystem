package com.fafeng.clinic.common;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public final class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private PinyinUtils() {
    }

    /** 提取中文拼音首字母缩写，用于搜索。 */
    public static String toAbbr(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : text.trim().toCharArray()) {
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (ch < 128) {
                if (Character.isLetterOrDigit(ch)) {
                    sb.append(Character.toLowerCase(ch));
                }
                continue;
            }
            try {
                String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
                if (pinyins != null && pinyins.length > 0 && !pinyins[0].isEmpty()) {
                    sb.append(pinyins[0].charAt(0));
                }
            } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                // skip unmapped char
            }
        }
        return sb.toString();
    }
}
