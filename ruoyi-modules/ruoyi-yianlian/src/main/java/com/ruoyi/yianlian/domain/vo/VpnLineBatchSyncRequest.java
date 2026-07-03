package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 按线路批量同步本地用户请求
 */
@Data
public class VpnLineBatchSyncRequest
{
    @NotBlank(message = "线路不能为空")
    private String appId;

    @NotEmpty(message = "请至少配置一个部门分组")
    @Valid
    private List<VpnLineSyncGroupItem> groups;
}
