package com.ruoyi.yianlian.domain.vo;

import java.io.Serializable;

/**
 * VPN 仪表盘概览指标
 */
public class VpnDashboardOverviewVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    private long onlineTotal;

    private long todayConnectSuccess;

    private long todayLoginFail;

    private long yesterdayConnectSuccess;

    private long yesterdayLoginFail;

    private long localUserTotal;

    private long lineEnabledTotal;

    private long authenticatedNotConnected;

    public long getOnlineTotal()
    {
        return onlineTotal;
    }

    public void setOnlineTotal(long onlineTotal)
    {
        this.onlineTotal = onlineTotal;
    }

    public long getTodayConnectSuccess()
    {
        return todayConnectSuccess;
    }

    public void setTodayConnectSuccess(long todayConnectSuccess)
    {
        this.todayConnectSuccess = todayConnectSuccess;
    }

    public long getTodayLoginFail()
    {
        return todayLoginFail;
    }

    public void setTodayLoginFail(long todayLoginFail)
    {
        this.todayLoginFail = todayLoginFail;
    }

    public long getYesterdayConnectSuccess()
    {
        return yesterdayConnectSuccess;
    }

    public void setYesterdayConnectSuccess(long yesterdayConnectSuccess)
    {
        this.yesterdayConnectSuccess = yesterdayConnectSuccess;
    }

    public long getYesterdayLoginFail()
    {
        return yesterdayLoginFail;
    }

    public void setYesterdayLoginFail(long yesterdayLoginFail)
    {
        this.yesterdayLoginFail = yesterdayLoginFail;
    }

    public long getLocalUserTotal()
    {
        return localUserTotal;
    }

    public void setLocalUserTotal(long localUserTotal)
    {
        this.localUserTotal = localUserTotal;
    }

    public long getLineEnabledTotal()
    {
        return lineEnabledTotal;
    }

    public void setLineEnabledTotal(long lineEnabledTotal)
    {
        this.lineEnabledTotal = lineEnabledTotal;
    }

    public long getAuthenticatedNotConnected()
    {
        return authenticatedNotConnected;
    }

    public void setAuthenticatedNotConnected(long authenticatedNotConnected)
    {
        this.authenticatedNotConnected = authenticatedNotConnected;
    }
}
