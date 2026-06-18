package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;

/**
 * 本地用户同步到线路
 */
public interface IVpnLocalUserSyncService
{
    /**
     * 将本地用户同步到指定线路
     */
    void syncToLine(VpnLocalUserSyncRequest request);
}
