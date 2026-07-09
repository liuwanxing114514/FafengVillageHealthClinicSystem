
package com.fafeng.clinic.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("medicine")
@Data
@NoArgsConstructor
public class Medicine {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_DELETED = "DELETED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String genericName;
    private String dosageForm;
    private String specification;
    private String baseUnit;
    private String packageUnit;
    private String manufacturer;
    private BigDecimal purchasePrice;
    private BigDecimal suggestedRetailPrice;
    private BigDecimal stockThreshold;
    private String pinyinAbbr;
    private String remark;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
