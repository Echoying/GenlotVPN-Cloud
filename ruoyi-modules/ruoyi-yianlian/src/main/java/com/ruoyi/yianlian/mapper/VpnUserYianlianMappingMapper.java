package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * VPN用户与易安联用户映射Mapper接口
 */
public interface VpnUserYianlianMappingMapper
{
    List<VpnUserYianlianMapping> selectByUserId(Long userId);

    VpnUserYianlianMapping selectByUserIdAndAppId(@Param("userId") Long userId, @Param("appId") String appId);

    int insert(VpnUserYianlianMapping mapping);

    int deleteByUserId(Long userId);

    int deleteByUserIds(Long[] userIds);
}
