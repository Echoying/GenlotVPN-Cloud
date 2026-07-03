package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

/**
 * 线路批量同步单用户结果
 */
@Data
public class VpnLineBatchSyncUserResult
{
    private Long localUserId;

    private String userName;

    private String nickName;

    private Long deptId;

    private String deptName;

    private boolean success;

    private String message;
}
