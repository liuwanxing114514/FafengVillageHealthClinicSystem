package com.fafeng.clinic.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafeng.clinic.system.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
