package com.ruoyi.yianlian.domain.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * VPN 登录趋势
 */
public class VpnDashboardLoginTrendVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    private List<String> dates = new ArrayList<>();

    private List<Long> connectSuccess = new ArrayList<>();

    private List<Long> loginFail = new ArrayList<>();

    public List<String> getDates()
    {
        return dates;
    }

    public void setDates(List<String> dates)
    {
        this.dates = dates;
    }

    public List<Long> getConnectSuccess()
    {
        return connectSuccess;
    }

    public void setConnectSuccess(List<Long> connectSuccess)
    {
        this.connectSuccess = connectSuccess;
    }

    public List<Long> getLoginFail()
    {
        return loginFail;
    }

    public void setLoginFail(List<Long> loginFail)
    {
        this.loginFail = loginFail;
    }
}
