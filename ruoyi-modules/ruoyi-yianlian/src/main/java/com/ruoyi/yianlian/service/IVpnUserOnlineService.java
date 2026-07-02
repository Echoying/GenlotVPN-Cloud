package com.ruoyi.yianlian.service;

import com.ruoyi.yianlian.api.domain.VpnUserOnline;

/**
 * VPN 在线用户 服务层
 */
public interface IVpnUserOnlineService
{
    /**
     * 按条件筛选在线用户列表（已按 loginTime 降序）
     */
    java.util.List<VpnUserOnline> selectOnlineList(String ipaddr, String userName, String appIds);
}
