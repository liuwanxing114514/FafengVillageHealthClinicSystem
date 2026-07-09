
package com.fafeng.clinic.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.system.dto.UpdateSettingRequest;
import com.fafeng.clinic.system.entity.SysSetting;
import com.fafeng.clinic.system.mapper.SysSettingMapper;
import com.fafeng.clinic.system.vo.SettingVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SysSettingMapper sysSettingMapper;
    private final AuditLogService auditLogService;


    public List<SettingVO> listAll() {
        return sysSettingMapper.selectList(new LambdaQueryWrapper<SysSetting>()
                        .orderByAsc(SysSetting::getSettingKey))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public SettingVO update(String key, UpdateSettingRequest request) {
        SysSetting setting = sysSettingMapper.selectOne(new LambdaQueryWrapper<SysSetting>()
                .eq(SysSetting::getSettingKey, key));
        if (setting == null) {
            setting = new SysSetting();
            setting.setSettingKey(key);
            setting.setRemark("");
            setting.setUpdatedAt(OffsetDateTime.now());
            setting.setSettingValue(request.value());
            sysSettingMapper.insert(setting);
        } else {
            setting.setSettingValue(request.value());
            setting.setUpdatedAt(OffsetDateTime.now());
            sysSettingMapper.updateById(setting);
        }

        auditLogService.log("UPDATE_SETTING", "sys_setting", setting.getId(),
                "{\"key\":\"" + key + "\"}");
        return toVO(setting);
    }

    private SettingVO toVO(SysSetting setting) {
        return new SettingVO(setting.getSettingKey(), setting.getSettingValue(), setting.getRemark());
    }
}
