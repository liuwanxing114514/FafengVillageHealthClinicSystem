
package com.fafeng.clinic.patient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("patient")
@Data
@NoArgsConstructor
public class Patient {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_DELETED = "DELETED";

    public static final String GENDER_MALE = "M";
    public static final String GENDER_FEMALE = "F";
    public static final String GENDER_UNKNOWN = "UNKNOWN";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String gender;
    private String idCard;
    private LocalDate birthDate;
    private Integer age;
    private Boolean ageManual;
    private String phone;
    private String address;
    private String remark;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
