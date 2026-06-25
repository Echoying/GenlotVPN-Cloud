package com.ruoyi.yianlian.domain.vo;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 离线登录导出请求
 */
public class VpnOfflineLoginExportRequest
{
    @NotNull(message = "本地用户ID不能为空")
    private Long localUserId;

    @NotEmpty(message = "请至少选择一条线路")
    private List<String> appIds;

    @NotNull(message = "有效时间不能为空")
    private String validity;

    public Long getLocalUserId()
    {
        return localUserId;
    }

    public void setLocalUserId(Long localUserId)
    {
        this.localUserId = localUserId;
    }

    public List<String> getAppIds()
    {
        return appIds;
    }

    public void setAppIds(List<String> appIds)
    {
        this.appIds = appIds;
    }

    public String getValidity()
    {
        return validity;
    }

    public void setValidity(String validity)
    {
        this.validity = validity;
    }
}
