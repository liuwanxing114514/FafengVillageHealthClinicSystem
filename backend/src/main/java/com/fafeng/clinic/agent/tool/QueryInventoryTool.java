package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.inventory.service.InventoryService;
import com.fafeng.clinic.inventory.vo.BatchVO;
import com.fafeng.clinic.medicine.service.MedicineService;
import com.fafeng.clinic.medicine.vo.MedicineDetailVO;
import com.fafeng.clinic.medicine.vo.MedicineListItemVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class QueryInventoryTool implements AgentTool {

    private final InventoryService inventoryService;
    private final MedicineService medicineService;
    private final ObjectMapper objectMapper;

    public QueryInventoryTool(InventoryService inventoryService,
                              MedicineService medicineService,
                              ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.medicineService = medicineService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return AgentToolName.QUERY_INVENTORY;
    }

    @Override
    public String description() {
        return "查询药品库存数量与批次。参数：medicineId 或 medicineName（二选一）";
    }

    @Override
    public AgentToolResult execute(JsonNode args) {
        Long medicineId = SearchMedicineTool.longArg(args, "medicineId");
        String medicineName = SearchMedicineTool.textArg(args, "medicineName");

        if (medicineId == null && (medicineName == null || medicineName.isBlank())) {
            return AgentToolResult.fail("请提供 medicineId 或 medicineName");
        }

        if (medicineId == null) {
            PageVO<MedicineListItemVO> search = medicineService.search(medicineName, "ACTIVE", 1, 5);
            if (search.records().isEmpty()) {
                return AgentToolResult.fail("未找到药品：" + medicineName);
            }
            if (search.total() > 1) {
                return AgentToolResult.fail("找到多种药品，请指定更精确的名称或提供 medicineId");
            }
            medicineId = search.records().getFirst().id();
        }

        MedicineDetailVO medicine;
        try {
            medicine = medicineService.getDetail(medicineId);
        } catch (BusinessException ex) {
            return AgentToolResult.fail("药品不存在");
        }

        BigDecimal totalStock = inventoryService.getTotalStock(medicineId);
        List<BatchVO> batches = inventoryService.listBatches(medicineId);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("medicineId", medicineId);
        data.put("medicineName", medicine.name());
        data.put("specification", medicine.specification());
        data.put("baseUnit", medicine.baseUnit());
        data.put("totalStock", totalStock.toPlainString());

        ArrayNode batchNodes = objectMapper.createArrayNode();
        for (BatchVO batch : batches) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("batchId", batch.id());
            node.put("batchNo", batch.batchNo());
            node.put("expiryDate", batch.expiryDate() == null ? "" : batch.expiryDate().toString());
            node.put("quantity", batch.quantity().toPlainString());
            node.put("baseUnit", batch.baseUnit());
            batchNodes.add(node);
        }
        data.set("batches", batchNodes);

        String summary = medicine.name() + " 总库存 " + totalStock.toPlainString() + medicine.baseUnit()
                + "，共 " + batches.size() + " 个批次";
        return AgentToolResult.ok(data, summary);
    }
}
