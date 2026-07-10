package com.fafeng.clinic.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fafeng.clinic.agent.tool.AgentToolName;
import com.fafeng.clinic.agent.vo.AgentReferenceVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentReferenceExtractor {

    public List<AgentReferenceVO> extract(List<AgentToolCallContext.ToolCallRecord> records) {
        Map<String, AgentReferenceVO> refs = new LinkedHashMap<>();
        for (AgentToolCallContext.ToolCallRecord record : records) {
            if (!record.success() || record.data() == null) {
                continue;
            }
            if (AgentToolName.SEARCH_PATIENT.equals(record.toolName())) {
                addPatients(refs, record.data().get("items"));
            } else if (AgentToolName.SEARCH_PATIENT_VISIT.equals(record.toolName())) {
                addVisits(refs, record.data().get("items"));
            } else if (AgentToolName.SEARCH_MEDICINE.equals(record.toolName())) {
                addMedicinesFromItems(refs, record.data().get("items"));
                addMedicineSingle(refs, record.data());
            } else if (AgentToolName.QUERY_INVENTORY.equals(record.toolName())) {
                addMedicineFromInventory(refs, record.data());
            } else if (AgentToolName.QUERY_EXPIRING_MEDICINE.equals(record.toolName())) {
                addMedicinesFromExpiring(refs, record.data().get("items"));
            }
        }
        return new ArrayList<>(refs.values());
    }

    private void addPatients(Map<String, AgentReferenceVO> refs, JsonNode items) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            Long id = longValue(item, "id");
            if (id == null) {
                continue;
            }
            String key = AgentReferenceVO.TYPE_PATIENT + ":" + id;
            refs.putIfAbsent(key, new AgentReferenceVO(
                    AgentReferenceVO.TYPE_PATIENT,
                    id,
                    "查看患者",
                    buildPersonHint(item)));
        }
    }

    private void addVisits(Map<String, AgentReferenceVO> refs, JsonNode items) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            Long patientId = longValue(item, "patientId");
            if (patientId != null) {
                String patientKey = AgentReferenceVO.TYPE_PATIENT + ":" + patientId;
                refs.putIfAbsent(patientKey, new AgentReferenceVO(
                        AgentReferenceVO.TYPE_PATIENT,
                        patientId,
                        "查看患者",
                        ""));
            }
            Long visitId = longValue(item, "visitId");
            if (visitId != null) {
                String visitKey = AgentReferenceVO.TYPE_VISIT + ":" + visitId;
                String time = textValue(item, "visitTime");
                String complaint = textValue(item, "chiefComplaint");
                String hint = time.isBlank() ? complaint : (complaint.isBlank() ? time : time + " · " + complaint);
                if (hint.length() > 40) {
                    hint = hint.substring(0, 40) + "…";
                }
                refs.putIfAbsent(visitKey, new AgentReferenceVO(
                        AgentReferenceVO.TYPE_VISIT,
                        visitId,
                        "查看病历",
                        hint));
            }
        }
    }

    private void addMedicinesFromItems(Map<String, AgentReferenceVO> refs, JsonNode items) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            addMedicineNode(refs, item, "id", "name", "specification");
        }
    }

    private void addMedicineSingle(Map<String, AgentReferenceVO> refs, JsonNode data) {
        if (data == null || !data.has("id")) {
            return;
        }
        addMedicineNode(refs, data, "id", "name", "specification");
    }

    private void addMedicineFromInventory(Map<String, AgentReferenceVO> refs, JsonNode data) {
        if (data == null) {
            return;
        }
        Long id = longValue(data, "medicineId");
        if (id == null) {
            return;
        }
        String name = textValue(data, "medicineName");
        String stock = textValue(data, "totalStock");
        String unit = textValue(data, "baseUnit");
        String hint = stock.isBlank() ? textValue(data, "specification")
                : "库存 " + stock + unit;
        putMedicine(refs, id, name, hint);
    }

    private void addMedicinesFromExpiring(Map<String, AgentReferenceVO> refs, JsonNode items) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            Long id = longValue(item, "medicineId");
            if (id == null) {
                continue;
            }
            String name = textValue(item, "medicineName");
            String hint = textValue(item, "expiryDate");
            if (!textValue(item, "batchNo").isBlank()) {
                hint = textValue(item, "batchNo") + (hint.isBlank() ? "" : " · " + hint);
            }
            putMedicine(refs, id, name, hint);
        }
    }

    private void addMedicineNode(Map<String, AgentReferenceVO> refs, JsonNode item,
                               String idField, String nameField, String specField) {
        Long id = longValue(item, idField);
        if (id == null) {
            return;
        }
        String name = textValue(item, nameField);
        String hint = textValue(item, specField);
        putMedicine(refs, id, name, hint);
    }

    private void putMedicine(Map<String, AgentReferenceVO> refs, Long id, String name, String hint) {
        String key = AgentReferenceVO.TYPE_MEDICINE + ":" + id;
        String label = name.isBlank() ? "查看药品" : "查看药品 " + name;
        refs.putIfAbsent(key, new AgentReferenceVO(
                AgentReferenceVO.TYPE_MEDICINE,
                id,
                label,
                hint));
    }

    private static String buildPersonHint(JsonNode item) {
        String gender = formatGender(textValue(item, "gender"));
        JsonNode ageNode = item.get("age");
        if (ageNode != null && ageNode.isNumber()) {
            return gender.isBlank() ? ageNode.asInt() + "岁" : gender + " · " + ageNode.asInt() + "岁";
        }
        return gender;
    }

    private static String formatGender(String code) {
        if ("M".equalsIgnoreCase(code)) {
            return "男";
        }
        if ("F".equalsIgnoreCase(code)) {
            return "女";
        }
        return code;
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asLong();
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }
}
