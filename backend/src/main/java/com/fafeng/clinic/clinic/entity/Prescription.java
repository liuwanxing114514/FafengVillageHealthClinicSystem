
package com.fafeng.clinic.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("prescription")
@Data
@NoArgsConstructor
public class Prescription {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_VOID = "VOID";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long visitId;
    private LocalDate prescriptionDate;
    private String diagnosis;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
