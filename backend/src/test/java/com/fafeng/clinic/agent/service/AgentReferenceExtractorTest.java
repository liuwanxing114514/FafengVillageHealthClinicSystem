package com.fafeng.clinic.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.agent.tool.AgentToolName;
import com.fafeng.clinic.agent.tool.AgentToolResult;
import com.fafeng.clinic.agent.vo.AgentReferenceVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentReferenceExtractorTest {

    private final AgentReferenceExtractor extractor = new AgentReferenceExtractor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsMedicineFromSearchAndInventory() throws Exception {
        ObjectNode searchData = objectMapper.createObjectNode();
        var items = objectMapper.createArrayNode();
        var item = objectMapper.createObjectNode();
        item.put("id", 10L);
        item.put("name", "阿莫西林");
        item.put("specification", "0.25g*24粒");
        items.add(item);
        searchData.set("items", items);

        ObjectNode invData = objectMapper.createObjectNode();
        invData.put("medicineId", 10L);
        invData.put("medicineName", "阿莫西林");
        invData.put("totalStock", "120");
        invData.put("baseUnit", "粒");

        List<AgentToolCallContext.ToolCallRecord> records = List.of(
                record(AgentToolName.SEARCH_MEDICINE, searchData),
                record(AgentToolName.QUERY_INVENTORY, invData));

        List<AgentReferenceVO> refs = extractor.extract(records);
        assertEquals(1, refs.size());
        assertEquals(AgentReferenceVO.TYPE_MEDICINE, refs.getFirst().refType());
        assertEquals(10L, refs.getFirst().refId());
        assertTrue(refs.getFirst().label().contains("阿莫西林"));
    }

    @Test
    void patientReferenceLabelWithoutRawNamePrefix() throws Exception {
        ObjectNode data = objectMapper.createObjectNode();
        var items = objectMapper.createArrayNode();
        var item = objectMapper.createObjectNode();
        item.put("id", 1L);
        item.put("name", "李**");
        item.put("gender", "F");
        item.put("age", 60);
        items.add(item);
        data.set("items", items);

        List<AgentReferenceVO> refs = extractor.extract(List.of(
                record(AgentToolName.SEARCH_PATIENT, data)));
        assertEquals("查看患者", refs.getFirst().label());
    }

    private AgentToolCallContext.ToolCallRecord record(String toolName, ObjectNode data) {
        return new AgentToolCallContext.ToolCallRecord(
                toolName, "{}", "ok", 1L, true, null, data);
    }
}
