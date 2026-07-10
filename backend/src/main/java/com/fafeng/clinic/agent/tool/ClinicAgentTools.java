package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.agent.service.AgentToolCallContext;
import com.fafeng.clinic.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI {@code @Tool} 注册层，业务逻辑复用 {@link AgentToolRegistry}。
 */
@Component
@RequiredArgsConstructor
public class ClinicAgentTools {

    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final AgentToolCallContext callContext;

    @Tool(name = "searchMedicine", description = "按名称、拼音或条码搜索药品")
    public String searchMedicine(
            @ToolParam(description = "名称关键词", required = false) String keyword,
            @ToolParam(description = "条码", required = false) String barcode,
            @ToolParam(description = "页码", required = false) Integer page,
            @ToolParam(description = "每页条数", required = false) Integer size) {
        ObjectNode args = objectMapper.createObjectNode();
        putText(args, "keyword", keyword);
        putText(args, "barcode", barcode);
        putInt(args, "page", page);
        putInt(args, "size", size);
        return executeTool(AgentToolName.SEARCH_MEDICINE, args);
    }

    @Tool(name = "queryInventory", description = "查询药品库存数量与批次，需提供 medicineId 或 medicineName")
    public String queryInventory(
            @ToolParam(description = "药品 ID", required = false) Long medicineId,
            @ToolParam(description = "药品名称", required = false) String medicineName) {
        ObjectNode args = objectMapper.createObjectNode();
        if (medicineId != null) {
            args.put("medicineId", medicineId);
        }
        putText(args, "medicineName", medicineName);
        return executeTool(AgentToolName.QUERY_INVENTORY, args);
    }

    @Tool(name = "queryExpiringMedicine", description = "查询临期药品（3个月内到期）")
    public String queryExpiringMedicine() {
        return executeTool(AgentToolName.QUERY_EXPIRING_MEDICINE, objectMapper.createObjectNode());
    }

    @Tool(name = "searchPatient", description = "搜索或列出患者；结果按更新时间倒序。问最近/最新一位患者时不传 keyword，page=1 size=1")
    public String searchPatient(
            @ToolParam(description = "姓名/电话/身份证关键词；查最近患者或统计人数时不要传", required = false) String keyword,
            @ToolParam(description = "页码", required = false) Integer page,
            @ToolParam(description = "每页条数", required = false) Integer size) {
        ObjectNode args = objectMapper.createObjectNode();
        putText(args, "keyword", keyword);
        putInt(args, "page", page);
        putInt(args, "size", size);
        return executeTool(AgentToolName.SEARCH_PATIENT, args);
    }

    @Tool(name = "searchPatientVisit", description = "查询患者历史病历，需提供 patientId 或 keyword")
    public String searchPatientVisit(
            @ToolParam(description = "患者 ID", required = false) Long patientId,
            @ToolParam(description = "患者姓名关键词", required = false) String keyword) {
        ObjectNode args = objectMapper.createObjectNode();
        if (patientId != null) {
            args.put("patientId", patientId);
        }
        putText(args, "keyword", keyword);
        return executeTool(AgentToolName.SEARCH_PATIENT_VISIT, args);
    }

    @Tool(name = "generateOutboundDraft", description = "生成待确认出库清单（写入草稿，不扣库存）。参数：prescriptionId 或 medicineName+quantity")
    public String generateOutboundDraft(
            @ToolParam(description = "处方 ID", required = false) Long prescriptionId,
            @ToolParam(description = "药品名称", required = false) String medicineName,
            @ToolParam(description = "出库数量", required = false) String quantity,
            @ToolParam(description = "单位", required = false) String unit) {
        ObjectNode args = objectMapper.createObjectNode();
        if (prescriptionId != null) {
            args.put("prescriptionId", prescriptionId);
        }
        putText(args, "medicineName", medicineName);
        putText(args, "quantity", quantity);
        putText(args, "unit", unit);
        return executeTool(AgentToolName.GENERATE_OUTBOUND_DRAFT, args);
    }

    private String executeTool(String toolName, ObjectNode args) {
        long start = System.currentTimeMillis();
        AgentToolResult result;
        try {
            result = toolRegistry.execute(toolName, args);
        } catch (BusinessException ex) {
            result = AgentToolResult.fail(ex.getMessage());
        }
        long duration = System.currentTimeMillis() - start;
        callContext.record(toolName, summarizeArgs(args), result, duration);
        return formatToolResponse(result);
    }

    private String formatToolResponse(AgentToolResult result) {
        if (result.data() != null) {
            try {
                return result.summary() + "\n" + objectMapper.writeValueAsString(result.data());
            } catch (Exception ignored) {
                // fall through
            }
        }
        return result.summary();
    }

    private String summarizeArgs(ObjectNode args) {
        try {
            String json = objectMapper.writeValueAsString(args);
            return json.length() > 500 ? json.substring(0, 500) : json;
        } catch (Exception ex) {
            return args.toString();
        }
    }

    private static void putText(ObjectNode args, String field, String value) {
        if (value != null && !value.isBlank()) {
            args.put(field, value);
        }
    }

    private static void putInt(ObjectNode args, String field, Integer value) {
        if (value != null) {
            args.put(field, value);
        }
    }
}
