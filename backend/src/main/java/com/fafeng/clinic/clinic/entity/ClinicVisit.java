
package com.fafeng.clinic.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("clinic_visit")
@Data
@NoArgsConstructor
public class ClinicVisit {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_VOID = "VOID";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private OffsetDateTime visitTime;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private BigDecimal temperature;
    private String bloodPressure;
    private BigDecimal spo2;
    private BigDecimal etco2;
    private Integer heartRate;
    private String pulse;
    private String allergyHistory;
    private String diagnosis;
    private String treatment;
    private String remark;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
