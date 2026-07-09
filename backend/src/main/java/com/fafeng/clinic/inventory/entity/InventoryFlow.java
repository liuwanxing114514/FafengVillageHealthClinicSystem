
package com.fafeng.clinic.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("inventory_flow")
@Data
@NoArgsConstructor
public class InventoryFlow {

    public static final String TYPE_INBOUND = "INBOUND";
    public static final String TYPE_OUTBOUND = "OUTBOUND";
    public static final String TYPE_ADJUST = "ADJUST";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicineId;
    private Long batchId;
    private String flowType;
    private BigDecimal quantityChange;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private String unit;
    private Long patientId;
    private Long prescriptionId;
    private String reason;
    private String remark;
    private String operator;
    private OffsetDateTime createdAt;

}
