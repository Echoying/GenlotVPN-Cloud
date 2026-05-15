package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;

import java.util.List;

/**
 * VPN用户与易安联用户映射Service接口
 */
public interface IVpnUserYianlianMappingService {

    /**
     * 根据本地用户ID查询映射列表
     */
    List<VpnUserYianlianMapping> selectByUserId(Long userId);

    /**
     * 根据本地用户ID和appId查询映射
     */
    VpnUserYianlianMapping selectByUserIdAndAppId(Long userId, String appId);

    /**
     * 新增映射
     */
    int insert(VpnUserYianlianMapping mapping);

    /**
     * 根据本地用户ID删除映射
     */
    int deleteByUserId(Long userId);

    /**
     * 根据本地用户ID批量删除映射
     */
    int deleteByUserIds(Long[] userIds);
}
