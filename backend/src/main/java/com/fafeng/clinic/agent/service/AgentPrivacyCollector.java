package com.fafeng.clinic.agent.service;

import com.fafeng.clinic.ai.util.Desensitizer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 单次 Agent 请求内收集患者 PII，供 answer 展示脱敏。
 */
@Component
public class AgentPrivacyCollector {

    private final ThreadLocal<Set<String>> names = ThreadLocal.withInitial(LinkedHashSet::new);
    private final ThreadLocal<Set<String>> phones = ThreadLocal.withInitial(LinkedHashSet::new);
    private final ThreadLocal<Set<String>> addresses = ThreadLocal.withInitial(LinkedHashSet::new);
    private final ThreadLocal<Set<String>> idCards = ThreadLocal.withInitial(LinkedHashSet::new);

    public void activate() {
        names.set(new LinkedHashSet<>());
        phones.set(new LinkedHashSet<>());
        addresses.set(new LinkedHashSet<>());
        idCards.set(new LinkedHashSet<>());
    }

    public void clear() {
        names.remove();
        phones.remove();
        addresses.remove();
        idCards.remove();
    }

    public void recordPatient(String name, String phone, String address, String idCard) {
        addIfPresent(names.get(), name);
        addIfPresent(phones.get(), phone);
        addIfPresent(addresses.get(), address);
        addIfPresent(idCards.get(), idCard);
    }

    public Desensitizer.PatientContext buildContext() {
        return new Desensitizer.PatientContext(
                first(names.get()),
                first(phones.get()),
                first(addresses.get()),
                first(idCards.get()));
    }

    public String desensitizeText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String result = Desensitizer.desensitizeText(text, buildContext());
        for (String name : names.get()) {
            if (name != null && !name.isBlank()) {
                result = result.replace(name, Desensitizer.maskName(name));
            }
        }
        return result;
    }

    private static void addIfPresent(Set<String> set, String value) {
        if (value != null && !value.isBlank()) {
            set.add(value.trim());
        }
    }

    private static String first(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        return set.iterator().next();
    }
}
