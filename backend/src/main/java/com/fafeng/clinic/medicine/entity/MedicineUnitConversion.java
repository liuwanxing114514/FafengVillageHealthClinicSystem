
package com.fafeng.clinic.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("medicine_unit_conversion")
@Data
@NoArgsConstructor
public class MedicineUnitConversion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicineId;
    private String fromUnit;
    private String toUnit;
    private Integer factor;
    private OffsetDateTime createdAt;

}
