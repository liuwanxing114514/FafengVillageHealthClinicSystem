package com.fafeng.clinic.agent.tool;

import java.util.Set;

/**
 * Agent 工具名常量（与 Spring AI {@code @Tool(name=...)} 一致）。
 * 前端中文映射见 {@code frontend/src/utils/agentLabels.ts}。
 */
public final class AgentToolName {

    public static final String SEARCH_MEDICINE = "searchMedicine";
    public static final String QUERY_INVENTORY = "queryInventory";
    public static final String QUERY_EXPIRING_MEDICINE = "queryExpiringMedicine";
    public static final String SEARCH_PATIENT = "searchPatient";
    public static final String SEARCH_PATIENT_VISIT = "searchPatientVisit";
    public static final String GENERATE_OUTBOUND_DRAFT = "generateOutboundDraft";

    private static final Set<String> ALL = Set.of(
            SEARCH_MEDICINE,
            QUERY_INVENTORY,
            QUERY_EXPIRING_MEDICINE,
            SEARCH_PATIENT,
            SEARCH_PATIENT_VISIT,
            GENERATE_OUTBOUND_DRAFT
    );

    private AgentToolName() {
    }

    public static boolean isRegistered(String name) {
        return name != null && ALL.contains(name);
    }

    public static Set<String> all() {
        return ALL;
    }
}
