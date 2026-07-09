package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.clinic.dto.VisitSearchQuery;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.dto.PatientSearchQuery;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.patient.vo.PatientListItemVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchPatientVisitTool implements AgentTool {

    private final VisitService visitService;
    private final PatientService patientService;
    private final ObjectMapper objectMapper;

    public SearchPatientVisitTool(VisitService visitService,
                                  PatientService patientService,
                                  ObjectMapper objectMapper) {
        this.visitService = visitService;
        this.patientService = patientService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return AgentToolName.SEARCH_PATIENT_VISIT;
    }

    @Override
    public String description() {
        return "查询患者历史病历。参数：patientId 或 keyword（患者姓名关键词）";
    }

    @Override
    public AgentToolResult execute(JsonNode args) {
        Long patientId = SearchMedicineTool.longArg(args, "patientId");
        String keyword = SearchMedicineTool.textArg(args, "keyword");

        if (patientId == null && (keyword == null || keyword.isBlank())) {
            // 无患者条件时，按病历关键词搜索最近记录
            PageVO<VisitListItemVO> visits = visitService.search(
                    new VisitSearchQuery(keyword, null, null, null),
                    1,
                    10);
            return buildVisitResult(visits.records(), visits.total(), "病历");
        }

        if (patientId == null) {
            PageVO<PatientListItemVO> patients = patientService.search(
                    new PatientSearchQuery(keyword, null, null, null, null, null, null, null, null),
                    1,
                    5);
            if (patients.records().isEmpty()) {
                return AgentToolResult.fail("未找到患者：" + keyword);
            }
            if (patients.total() > 1) {
                return AgentToolResult.fail("找到多位患者，请提供更精确的关键词或 patientId");
            }
            patientId = patients.records().getFirst().id();
        }

        List<VisitListItemVO> visits = visitService.listByPatient(patientId);
        return buildVisitResult(visits, visits.size(), "患者病历");
    }

    private AgentToolResult buildVisitResult(List<VisitListItemVO> visits, long total, String label) {
        ArrayNode items = objectMapper.createArrayNode();
        for (VisitListItemVO visit : visits) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("visitId", visit.id());
            node.put("patientId", visit.patientId());
            node.put("patientName", Desensitizer.maskName(visit.patientName()));
            node.put("visitTime", visit.visitTime() == null ? "" : visit.visitTime().toString());
            node.put("chiefComplaint", visit.chiefComplaint());
            node.put("diagnosis", visit.diagnosis());
            items.add(node);
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.set("items", items);
        data.put("total", total);

        String summary = total == 0 ? "未找到病历记录" : "共 " + total + " 条" + label;
        return AgentToolResult.ok(data, summary);
    }
}
