package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.inventory.service.InventoryService;
import com.fafeng.clinic.inventory.vo.ExpiringAlertVO;
import com.fafeng.clinic.inventory.vo.InventoryAlertsVO;
import org.springframework.stereotype.Component;

@Component
public class QueryExpiringMedicineTool implements AgentTool {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public QueryExpiringMedicineTool(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return AgentToolName.QUERY_EXPIRING_MEDICINE;
    }

    @Override
    public String description() {
        return "查询临期药品（3个月内到期）。无参数。";
    }

    @Override
    public AgentToolResult execute(JsonNode args) {
        InventoryAlertsVO alerts = inventoryService.getAlerts();

        ArrayNode items = objectMapper.createArrayNode();
        for (ExpiringAlertVO alert : alerts.expiring()) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("medicineId", alert.medicineId());
            node.put("medicineName", alert.medicineName());
            node.put("batchNo", alert.batchNo());
            node.put("expiryDate", alert.expiryDate() == null ? "" : alert.expiryDate().toString());
            node.put("quantity", alert.quantity().toPlainString());
            node.put("baseUnit", alert.baseUnit());
            items.add(node);
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.set("items", items);
        data.put("count", alerts.expiring().size());

        String summary = alerts.expiring().isEmpty()
                ? "当前无临期药品"
                : "共 " + alerts.expiring().size() + " 条临期记录";
        return AgentToolResult.ok(data, summary);
    }
}
