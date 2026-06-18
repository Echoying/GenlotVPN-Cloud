package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 本地用户同步到线路请求
 */
@Data
public class VpnLocalUserSyncRequest
{
    @NotNull(message = "本地用户ID不能为空")
    private Long localUserId;

    @NotBlank(message = "线路不能为空")
    private String appId;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    private List<Long> roleIds;
}
