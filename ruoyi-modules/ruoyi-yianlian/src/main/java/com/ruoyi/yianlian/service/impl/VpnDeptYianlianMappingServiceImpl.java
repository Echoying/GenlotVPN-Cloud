package com.ruoyi.yianlian.service.impl;

import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.mapper.VpnDeptYianlianMappingMapper;
import com.ruoyi.yianlian.service.IVpnDeptYianlianMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * VPN部门与易安联部门映射Service业务层处理
 */
@Service
public class VpnDeptYianlianMappingServiceImpl implements IVpnDeptYianlianMappingService {

    @Autowired
    private VpnDeptYianlianMappingMapper mappingMapper;

    @Override
    public List<VpnDeptYianlianMapping> selectByDeptId(Long deptId) {
        return mappingMapper.selectByDeptId(deptId);
    }

    @Override
    public VpnDeptYianlianMapping selectByDeptIdAndAppId(Long deptId, String appId) {
        return mappingMapper.selectByDeptIdAndAppId(deptId, appId);
    }

    @Override
    public void insert(VpnDeptYianlianMapping mapping) {
        mappingMapper.insert(mapping);
    }

    @Override
    public void deleteByDeptId(Long deptId) {
        mappingMapper.deleteByDeptId(deptId);
    }

    @Override
    public void deleteByDeptIds(Long[] deptIds) {
        mappingMapper.deleteByDeptIds(deptIds);
    }
}
