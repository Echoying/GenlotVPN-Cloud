package com.ruoyi.yianlian.service;

import java.util.List;
import com.ruoyi.yianlian.domain.VpnLogininfor;
import com.ruoyi.yianlian.domain.vo.VpnDashboardDistributionsVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardLoginTrendVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardOverviewVO;

/**
 * VPN 仪表盘 服务层
 */
public interface IVpnDashboardService
{
    VpnDashboardOverviewVO getOverview();

    VpnDashboardLoginTrendVO getLoginTrend(int days);

    VpnDashboardDistributionsVO getDistributions(int days);

    List<VpnLogininfor> getRecentEvents(int limit);
}
