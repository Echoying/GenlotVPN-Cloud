package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;
import com.ruoyi.yianlian.mapper.VpnUserYianlianMappingMapper;
import com.ruoyi.yianlian.service.vpn.IVpnUserYianlianMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * VPN用户与易安联用户映射Service实现
 */
@Service
public class VpnUserYianlianMappingServiceImpl implements IVpnUserYianlianMappingService {

    @Autowired
    private VpnUserYianlianMappingMapper mappingMapper;

    @Override
    public List<VpnUserYianlianMapping> selectByUserId(Long userId) {
        return mappingMapper.selectByUserId(userId);
    }

    @Override
    public VpnUserYianlianMapping selectByUserIdAndAppId(Long userId, String appId) {
        return mappingMapper.selectByUserIdAndAppId(userId, appId);
    }

    @Override
    public int insert(VpnUserYianlianMapping mapping) {
        return mappingMapper.insert(mapping);
    }

    @Override
    public int deleteByUserId(Long userId) {
        return mappingMapper.deleteByUserId(userId);
    }

    @Override
    public int deleteByUserIds(Long[] userIds) {
        return mappingMapper.deleteByUserIds(userIds);
    }
}
