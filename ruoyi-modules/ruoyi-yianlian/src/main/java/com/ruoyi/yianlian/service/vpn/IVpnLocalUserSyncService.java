package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;

/**
 * 本地用户同步到线路（含部门与角色）
 */
public interface IVpnLocalUserSyncService
{
    /**
     * 将本地用户同步或更新到指定线路（维护 vpn_user_role）
     */
    void syncToLine(VpnLocalUserSyncRequest request);
}
