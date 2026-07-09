
package com.fafeng.clinic.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("inventory_batch")
@Data
@NoArgsConstructor
public class InventoryBatch {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DEPLETED = "DEPLETED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicineId;
    private String batchNo;
    private LocalDate expiryDate;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private String supplier;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
