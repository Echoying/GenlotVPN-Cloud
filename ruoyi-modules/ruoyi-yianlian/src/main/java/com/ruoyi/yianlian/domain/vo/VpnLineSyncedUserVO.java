package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

/**
 * 线路下已同步的本地用户（只读展示）
 */
@Data
public class VpnLineSyncedUserVO
{
    private Long userId;

    private Long localUserId;

    private String userName;

    private String nickName;

    private Long deptId;

    private String deptName;
}
