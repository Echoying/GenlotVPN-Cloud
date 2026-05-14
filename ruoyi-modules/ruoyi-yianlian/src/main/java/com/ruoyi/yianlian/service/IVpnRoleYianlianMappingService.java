package com.ruoyi.yianlian.service;

import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;

import java.util.List;

/**
 * VPN角色与易安联角色映射Service接口
 */
public interface IVpnRoleYianlianMappingService {

    List<VpnRoleYianlianMapping> selectByRoleId(Long roleId);

    void insert(VpnRoleYianlianMapping mapping);

    void deleteByRoleId(Long roleId);

    void deleteByRoleIds(Long[] roleIds);
}
