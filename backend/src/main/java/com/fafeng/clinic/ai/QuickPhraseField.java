package com.fafeng.clinic.ai;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum QuickPhraseField {

    CHIEF_COMPLAINT("chief_complaint", "主诉"),
    PRESENT_ILLNESS("present_illness", "现病史"),
    PAST_HISTORY("past_history", "既往史"),
    ALLERGY_HISTORY("allergy_history", "过敏史"),
    DIAGNOSIS("diagnosis", "诊断"),
    TREATMENT("treatment", "处理意见"),
    REMARK("remark", "备注"),
    PRESCRIPTION_USAGE("prescription_usage", "用法");

    private final String key;
    private final String label;

    QuickPhraseField(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public static QuickPhraseField require(String fieldKey) {
        return fromKey(fieldKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "不支持的快捷语字段：" + fieldKey));
    }

    public static Optional<QuickPhraseField> fromKey(String fieldKey) {
        if (fieldKey == null || fieldKey.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(field -> field.key.equals(fieldKey.trim()))
                .findFirst();
    }

    public static List<QuickPhraseField> all() {
        return List.of(values());
    }
}
