package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 线路同步部门分组项
 */
@Data
public class VpnLineSyncGroupItem
{
    @NotNull(message = "部门不能为空")
    private Long deptId;

    /** 待同步本地用户ID，可为空（仅展示已同步用户的分组） */
    private List<Long> localUserIds;
}
