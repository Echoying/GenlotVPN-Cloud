package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.vo.VpnLocalUserBatchSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncLineResult;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnLineBatchSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnLineBatchSyncUserResult;
import com.ruoyi.yianlian.domain.vo.VpnLineSyncContextVO;

import java.util.List;

/**
 * 本地用户同步到线路（含部门与角色）
 */
public interface IVpnLocalUserSyncService
{
    void syncToLine(VpnLocalUserSyncRequest request);

    List<VpnLocalUserSyncLineResult> syncToLines(VpnLocalUserBatchSyncRequest request);

    VpnLineSyncContextVO buildLineSyncContext(String appId);

    List<VpnLineBatchSyncUserResult> syncUsersToLine(VpnLineBatchSyncRequest request);
}
