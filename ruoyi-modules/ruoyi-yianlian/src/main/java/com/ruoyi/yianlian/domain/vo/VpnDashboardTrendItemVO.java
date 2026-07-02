package com.ruoyi.yianlian.domain.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 按日统计项（Mapper 查询中间结果）
 */
public class VpnDashboardTrendItemVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Date statDate;

    private Long connectSuccess;

    private Long loginFail;

    public Date getStatDate()
    {
        return statDate;
    }

    public void setStatDate(Date statDate)
    {
        this.statDate = statDate;
    }

    public Long getConnectSuccess()
    {
        return connectSuccess;
    }

    public void setConnectSuccess(Long connectSuccess)
    {
        this.connectSuccess = connectSuccess;
    }

    public Long getLoginFail()
    {
        return loginFail;
    }

    public void setLoginFail(Long loginFail)
    {
        this.loginFail = loginFail;
    }
}
