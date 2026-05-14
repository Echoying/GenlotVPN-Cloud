package com.ruoyi.yianlian.service.impl;

import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;
import com.ruoyi.yianlian.mapper.VpnRoleYianlianMappingMapper;
import com.ruoyi.yianlian.service.IVpnRoleYianlianMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * VPN角色与易安联角色映射Service业务层处理
 */
@Service
public class VpnRoleYianlianMappingServiceImpl implements IVpnRoleYianlianMappingService {

    @Autowired
    private VpnRoleYianlianMappingMapper mappingMapper;

    @Override
    public List<VpnRoleYianlianMapping> selectByRoleId(Long roleId) {
        return mappingMapper.selectByRoleId(roleId);
    }

    @Override
    public void insert(VpnRoleYianlianMapping mapping) {
        mappingMapper.insert(mapping);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        mappingMapper.deleteByRoleId(roleId);
    }

    @Override
    public void deleteByRoleIds(Long[] roleIds) {
        mappingMapper.deleteByRoleIds(roleIds);
    }
}
