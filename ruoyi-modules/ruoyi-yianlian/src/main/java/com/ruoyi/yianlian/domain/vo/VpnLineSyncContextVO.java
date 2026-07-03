package com.ruoyi.yianlian.domain.vo;

import com.ruoyi.yianlian.domain.VpnLocalUser;
import lombok.Data;

import java.util.List;

/**
 * 线路批量同步弹窗上下文
 */
@Data
public class VpnLineSyncContextVO
{
    private List<VpnLocalUser> unsyncedUsers;

    private List<VpnLineSyncedGroupVO> syncedGroups;
}
