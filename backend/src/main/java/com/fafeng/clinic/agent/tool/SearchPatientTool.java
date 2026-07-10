package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.agent.service.AgentPrivacyCollector;
import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.dto.PatientSearchQuery;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.patient.vo.PatientListItemVO;
import org.springframework.stereotype.Component;

@Component
public class SearchPatientTool implements AgentTool {

    private final PatientService patientService;
    private final ObjectMapper objectMapper;
    private final AgentPrivacyCollector privacyCollector;

    public SearchPatientTool(PatientService patientService,
                             ObjectMapper objectMapper,
                             AgentPrivacyCollector privacyCollector) {
        this.patientService = patientService;
        this.objectMapper = objectMapper;
        this.privacyCollector = privacyCollector;
    }

    @Override
    public String name() {
        return AgentToolName.SEARCH_PATIENT;
    }

    @Override
    public String description() {
        return "搜索或列出患者（按更新时间倒序）。参数：keyword（可选）、page、size；问最近一位患者时不传 keyword，page=1 size=1";
    }

    @Override
    public AgentToolResult execute(JsonNode args) {
        String keyword = SearchMedicineTool.textArg(args, "keyword");
        int page = SearchMedicineTool.intArg(args, "page", 1);
        int size = SearchMedicineTool.intArg(args, "size", 10);

        PageVO<PatientListItemVO> result = patientService.search(
                new PatientSearchQuery(keyword, null, null, null, null, null, null, null, null),
                page,
                size);

        ArrayNode items = objectMapper.createArrayNode();
        for (PatientListItemVO patient : result.records()) {
            privacyCollector.recordPatient(patient.name(), patient.phone(), null, null);
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", patient.id());
            node.put("name", Desensitizer.maskName(patient.name()));
            node.put("gender", patient.gender());
            node.put("age", patient.age());
            node.put("phone", Desensitizer.maskPhone(patient.phone()));
            items.add(node);
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.set("items", items);
        data.put("total", result.total());

        String summary = result.total() == 0
                ? "未找到匹配患者"
                : "找到 " + result.total() + " 位患者";
        return AgentToolResult.ok(data, summary);
    }
}
