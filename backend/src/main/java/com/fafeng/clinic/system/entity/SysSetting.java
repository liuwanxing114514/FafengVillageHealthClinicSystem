
package com.fafeng.clinic.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("sys_setting")
@Data
@NoArgsConstructor
public class SysSetting {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String settingKey;
    private String settingValue;
    private String remark;
    private OffsetDateTime updatedAt;

}
