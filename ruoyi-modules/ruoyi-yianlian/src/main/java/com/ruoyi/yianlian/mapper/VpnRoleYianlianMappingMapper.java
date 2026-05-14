package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * VPN角色与易安联角色映射Mapper接口
 */
public interface VpnRoleYianlianMappingMapper {

    List<VpnRoleYianlianMapping> selectByRoleId(Long roleId);

    VpnRoleYianlianMapping selectByRoleIdAndAppId(@Param("roleId") Long roleId, @Param("appId") String appId);

    int insert(VpnRoleYianlianMapping mapping);

    int deleteByRoleId(Long roleId);

    int deleteByRoleIds(Long[] roleIds);
}
