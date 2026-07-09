
package com.fafeng.clinic.system.service;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.common.PasswordValidator;
import com.fafeng.clinic.system.dto.ChangePasswordRequest;
import com.fafeng.clinic.system.dto.LoginRequest;
import com.fafeng.clinic.system.entity.SysUser;
import com.fafeng.clinic.system.mapper.SysUserMapper;
import com.fafeng.clinic.system.vo.SessionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String USERNAME_ADMIN = "admin";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final AuthenticationManager authenticationManager;


    public void login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(USERNAME_ADMIN, request.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        httpRequest.getSession(true);

        SysUser user = getSingleUser();
        auditLogService.log("LOGIN", "sys_user", user.getId(), "{\"message\":\"登录成功\"}");
    }

    public void logout(HttpServletRequest httpRequest) {
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        auditLogService.log("LOGOUT", "sys_user", null, "{\"message\":\"登出\"}");
    }

    public SessionVO currentSession() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof ClinicUserDetails)) {
            return new SessionVO(false, null);
        }
        return new SessionVO(true, USERNAME_ADMIN);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的新密码不一致");
        }
        PasswordValidator.validate(request.newPassword());

        SysUser user = getSingleUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(OffsetDateTime.now());
        sysUserMapper.updateById(user);

        auditLogService.log("CHANGE_PASSWORD", "sys_user", user.getId(), "{\"message\":\"修改密码\"}");
    }

    private SysUser getSingleUser() {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "系统尚未初始化，请先设置密码");
        }
        return user;
    }
}
