
package com.fafeng.clinic.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("prescription_item")
@Data
@NoArgsConstructor
public class PrescriptionItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private Long medicineId;
    private String dosageForm;
    private String medicineName;
    private String specification;
    private BigDecimal quantity;
    private String unit;
    private String usage;
    private Integer sortOrder;
    private OffsetDateTime createdAt;

}
