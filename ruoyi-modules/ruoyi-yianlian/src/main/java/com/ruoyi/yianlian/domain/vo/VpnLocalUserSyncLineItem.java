package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class VpnLocalUserSyncLineItem
{
    @NotBlank(message = "线路不能为空")
    private String appId;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    private List<Long> roleIds;
}
