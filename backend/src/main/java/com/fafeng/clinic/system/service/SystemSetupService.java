package com.fafeng.clinic.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.common.PasswordValidator;
import com.fafeng.clinic.system.dto.SetupPasswordRequest;
import com.fafeng.clinic.system.entity.SysUser;
import com.fafeng.clinic.system.mapper.SysUserMapper;
import com.fafeng.clinic.system.vo.SetupStatusVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SystemSetupService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public SystemSetupService(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public SetupStatusVO getSetupStatus() {
        long count = sysUserMapper.selectCount(new LambdaQueryWrapper<>());
        return new SetupStatusVO(count == 0);
    }

    @Transactional
    public void setupPassword(SetupPasswordRequest request) {
        if (!getSetupStatus().needSetup()) {
            throw new BusinessException(ErrorCode.CONFLICT, "系统已完成初始化，不能重复设置密码");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        PasswordValidator.validate(request.password());

        OffsetDateTime now = OffsetDateTime.now();
        SysUser user = new SysUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setMustChangePassword(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        sysUserMapper.insert(user);

        auditLogService.log("SETUP_PASSWORD", "sys_user", user.getId(), "{\"message\":\"首次设置密码\"}");
    }
}
