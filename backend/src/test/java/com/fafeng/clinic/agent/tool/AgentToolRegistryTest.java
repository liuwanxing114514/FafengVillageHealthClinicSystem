package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgentToolRegistryTest {

    @Autowired
    private AgentToolRegistry toolRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin")
    void searchMedicineAndQueryInventory() throws Exception {
        long medicineId = createMedicine("阿莫西林胶囊");

        ObjectNode searchArgs = objectMapper.createObjectNode();
        searchArgs.put("keyword", "阿莫西林");
        AgentToolResult searchResult = toolRegistry.execute(AgentToolName.SEARCH_MEDICINE, searchArgs);
        assertTrue(searchResult.success());
        assertTrue(searchResult.data().path("total").asInt() >= 1);

        inbound(medicineId, 50);

        ObjectNode inventoryArgs = objectMapper.createObjectNode();
        inventoryArgs.put("medicineId", medicineId);
        AgentToolResult inventoryResult = toolRegistry.execute(AgentToolName.QUERY_INVENTORY, inventoryArgs);
        assertTrue(inventoryResult.success());
        assertTrue(inventoryResult.summary().contains("50"));
    }

    @Test
    @WithMockUser(username = "admin")
    void queryExpiringMedicine() throws Exception {
        long medicineId = createMedicine("临期测试药");
        inbound(medicineId, 10, java.time.LocalDate.now().plusMonths(1));

        AgentToolResult result = toolRegistry.execute(AgentToolName.QUERY_EXPIRING_MEDICINE, objectMapper.createObjectNode());
        assertTrue(result.success());
        assertTrue(result.data().path("count").asInt() >= 1);
    }

    @Test
    @WithMockUser(username = "admin")
    void generateOutboundDraftDoesNotDeductStock() throws Exception {
        long medicineId = createMedicine("出库草稿药");
        inbound(medicineId, 100);

        ObjectNode args = objectMapper.createObjectNode();
        args.put("medicineName", "出库草稿药");
        args.put("quantity", "5");
        args.put("unit", "片");

        AgentToolResult result = toolRegistry.execute(AgentToolName.GENERATE_OUTBOUND_DRAFT, args);
        assertTrue(result.success());
        assertNotNull(result.pendingDraftId());

        ObjectNode inventoryArgs = objectMapper.createObjectNode();
        inventoryArgs.put("medicineId", medicineId);
        AgentToolResult inventory = toolRegistry.execute(AgentToolName.QUERY_INVENTORY, inventoryArgs);
        assertEquals(0, new java.math.BigDecimal("100").compareTo(
                new java.math.BigDecimal(inventory.data().path("totalStock").asText())));
    }

    private long createMedicine(String name) throws Exception {
        String response = mockMvc.perform(post("/api/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "dosageForm": "胶囊",
                                  "specification": "0.25g*24粒",
                                  "baseUnit": "片",
                                  "packageUnit": "盒",
                                  "purchasePrice": 12,
                                  "stockThreshold": 50
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void inbound(long medicineId, int quantity) throws Exception {
        inbound(medicineId, quantity, java.time.LocalDate.now().plusYears(1));
    }

    private void inbound(long medicineId, int quantity, java.time.LocalDate expiry) throws Exception {
        mockMvc.perform(post("/api/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicineId": %d,
                                  "quantity": %d,
                                  "unit": "片",
                                  "batchNo": "B-TEST",
                                  "expiryDate": "%s",
                                  "purchasePrice": 0.5,
                                  "supplier": "测试"
                                }
                                """.formatted(medicineId, quantity, expiry)))
                .andExpect(status().isOk());
    }
}
