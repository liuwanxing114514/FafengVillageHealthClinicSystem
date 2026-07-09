package com.fafeng.clinic.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.system.entity.SysUser;
import com.fafeng.clinic.system.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ClinicUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public ClinicUserDetailsService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!AuthService.USERNAME_ADMIN.equals(username)) {
            throw new UsernameNotFoundException("用户不存在");
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "系统尚未初始化");
        }
        return new ClinicUserDetails(user.getId(), user.getPasswordHash());
    }
}
