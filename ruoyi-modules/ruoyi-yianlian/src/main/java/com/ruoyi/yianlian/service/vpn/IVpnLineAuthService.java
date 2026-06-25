package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnUser;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * VPN 线路授权（部门/角色/用户授权并集）
 */
public interface IVpnLineAuthService
{
    /**
     * 根据线路用户的部门、角色、用户授权解析可访问的线路 ID 集合
     */
    Set<String> resolveAuthorizedLineIds(VpnUser vpnUser);

    /**
     * 将线路 ID 集合转为客户端可用的线路 VO 列表（含 spaKey MD5）
     */
    List<Map<String, Object>> toAuthorizedLineVos(Set<String> lineIdSet);
}
