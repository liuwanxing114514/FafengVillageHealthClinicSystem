package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.medicine.service.MedicineService;
import com.fafeng.clinic.medicine.vo.MedicineListItemVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import org.springframework.stereotype.Component;

@Component
public class SearchMedicineTool implements AgentTool {

    private final MedicineService medicineService;
    private final ObjectMapper objectMapper;

    public SearchMedicineTool(MedicineService medicineService, ObjectMapper objectMapper) {
        this.medicineService = medicineService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return AgentToolName.SEARCH_MEDICINE;
    }

    @Override
    public String description() {
        return "按名称、拼音或条码搜索药品。参数：keyword（名称关键词）、barcode（条码，可选）、page、size";
    }

    @Override
    public AgentToolResult execute(JsonNode args) {
        String barcode = textArg(args, "barcode");
        if (barcode != null && !barcode.isBlank()) {
            try {
                var medicine = medicineService.findByBarcode(barcode.trim());
                ObjectNode node = objectMapper.createObjectNode();
                node.put("id", medicine.id());
                node.put("name", medicine.name());
                node.put("specification", medicine.specification());
                node.put("baseUnit", medicine.baseUnit());
                return AgentToolResult.ok(node, "找到 1 种药品：" + medicine.name());
            } catch (BusinessException ex) {
                return AgentToolResult.fail("未找到条码对应药品");
            }
        }

        String keyword = textArg(args, "keyword");
        int page = intArg(args, "page", 1);
        int size = intArg(args, "size", 10);
        PageVO<MedicineListItemVO> result = medicineService.search(keyword, "ACTIVE", page, size);

        ArrayNode items = objectMapper.createArrayNode();
        for (MedicineListItemVO item : result.records()) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", item.id());
            node.put("name", item.name());
            node.put("specification", item.specification());
            node.put("baseUnit", item.baseUnit());
            String itemBarcode = item.barcodes() == null || item.barcodes().isEmpty()
                    ? "" : item.barcodes().getFirst();
            node.put("barcode", itemBarcode);
            items.add(node);
        }
        ObjectNode data = objectMapper.createObjectNode();
        data.set("items", items);
        data.put("total", result.total());
        String summary = result.total() == 0
                ? "未找到匹配药品"
                : "找到 " + result.total() + " 种药品，返回前 " + result.records().size() + " 条";
        return AgentToolResult.ok(data, summary);
    }

    static String textArg(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return null;
        }
        return args.get(field).asText();
    }

    static int intArg(JsonNode args, String field, int defaultValue) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return defaultValue;
        }
        return args.get(field).asInt(defaultValue);
    }

    static Long longArg(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return null;
        }
        return args.get(field).asLong();
    }
}
