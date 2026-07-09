
package com.fafeng.clinic.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("sys_user")
@Data
@NoArgsConstructor
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String passwordHash;
    private Boolean mustChangePassword;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
