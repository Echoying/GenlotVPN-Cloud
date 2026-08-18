package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnLineAuthView;

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

    /**
     * 线路用户各线路权限只读汇总（含应用组/应用/来源）
     */
    VpnLineAuthView buildLineAuthView(Long userId);

    /**
     * 本地用户：按已同步线路用户汇总权限
     */
    VpnLineAuthView buildLocalLineAuthView(Long localUserId);
}
