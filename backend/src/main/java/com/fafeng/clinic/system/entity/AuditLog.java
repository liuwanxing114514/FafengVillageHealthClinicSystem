
package com.fafeng.clinic.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("audit_log")
@Data
@NoArgsConstructor
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String operator;
    private OffsetDateTime createdAt;

}
