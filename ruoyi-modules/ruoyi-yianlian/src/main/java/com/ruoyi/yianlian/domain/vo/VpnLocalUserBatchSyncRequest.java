package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class VpnLocalUserBatchSyncRequest
{
    @NotNull(message = "本地用户ID不能为空")
    private Long localUserId;

    @NotEmpty(message = "请至少配置一条线路")
    @Valid
    private List<VpnLocalUserSyncLineItem> lines;
}
