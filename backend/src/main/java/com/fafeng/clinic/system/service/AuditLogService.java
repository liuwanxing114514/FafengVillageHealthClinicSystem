
package com.fafeng.clinic.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.system.entity.AuditLog;
import com.fafeng.clinic.system.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    public static final String OPERATOR_ADMIN = "admin";

    private final AuditLogMapper auditLogMapper;


    public void log(String action, String targetType, Long targetId, String detailJson) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detailJson);
        log.setOperator(OPERATOR_ADMIN);
        log.setCreatedAt(OffsetDateTime.now());
        auditLogMapper.insert(log);
    }
}
