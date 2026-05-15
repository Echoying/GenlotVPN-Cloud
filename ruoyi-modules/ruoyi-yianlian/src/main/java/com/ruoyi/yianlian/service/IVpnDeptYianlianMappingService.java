package com.ruoyi.yianlian.service;

import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;

import java.util.List;

/**
 * VPN部门与易安联部门映射Service接口
 */
public interface IVpnDeptYianlianMappingService {

    List<VpnDeptYianlianMapping> selectByDeptId(Long deptId);

    VpnDeptYianlianMapping selectByDeptIdAndAppId(Long deptId, String appId);

    void insert(VpnDeptYianlianMapping mapping);

    void deleteByDeptId(Long deptId);

    void deleteByDeptIds(Long[] deptIds);
}
