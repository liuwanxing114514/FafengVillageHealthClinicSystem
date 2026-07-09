
package com.fafeng.clinic.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("medicine_barcode")
@Data
@NoArgsConstructor
public class MedicineBarcode {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicineId;
    private String barcode;
    private String remark;
    private OffsetDateTime createdAt;

}
