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
                    "查看患者 " + textValue(item, "name"),
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
                        "查看患者 " + textValue(item, "patientName"),
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

    private static String buildPersonHint(JsonNode item) {
        String gender = textValue(item, "gender");
        JsonNode ageNode = item.get("age");
        if (ageNode != null && ageNode.isNumber()) {
            return gender.isBlank() ? ageNode.asInt() + "岁" : gender + " · " + ageNode.asInt() + "岁";
        }
        return gender;
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
