package com.ruoyi.yianlian.domain.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * VPN 仪表盘分布数据
 */
public class VpnDashboardDistributionsVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    private List<VpnDashboardNameValueVO> onlineByLine = new ArrayList<>();

    private List<VpnDashboardNameValueVO> clientOs = new ArrayList<>();

    private List<VpnDashboardNameValueVO> failReasons = new ArrayList<>();

    public List<VpnDashboardNameValueVO> getOnlineByLine()
    {
        return onlineByLine;
    }

    public void setOnlineByLine(List<VpnDashboardNameValueVO> onlineByLine)
    {
        this.onlineByLine = onlineByLine;
    }

    public List<VpnDashboardNameValueVO> getClientOs()
    {
        return clientOs;
    }

    public void setClientOs(List<VpnDashboardNameValueVO> clientOs)
    {
        this.clientOs = clientOs;
    }

    public List<VpnDashboardNameValueVO> getFailReasons()
    {
        return failReasons;
    }

    public void setFailReasons(List<VpnDashboardNameValueVO> failReasons)
    {
        this.failReasons = failReasons;
    }
}
