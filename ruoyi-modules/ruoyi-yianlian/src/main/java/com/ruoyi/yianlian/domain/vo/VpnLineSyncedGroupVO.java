package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 线路下按部门聚合的已同步用户
 */
@Data
public class VpnLineSyncedGroupVO
{
    private Long deptId;

    private String deptName;

    private List<VpnLineSyncedUserVO> users;
}
