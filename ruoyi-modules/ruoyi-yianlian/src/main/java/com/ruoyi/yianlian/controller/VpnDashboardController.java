package com.ruoyi.yianlian.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.yianlian.domain.VpnLogininfor;
import com.ruoyi.yianlian.domain.vo.VpnDashboardDistributionsVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardLoginTrendVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardOverviewVO;
import com.ruoyi.yianlian.service.IVpnDashboardService;

/**
 * VPN 仪表盘
 */
@RestController
@RequestMapping("/vpn/dashboard")
public class VpnDashboardController extends BaseController
{
    @Autowired
    private IVpnDashboardService vpnDashboardService;

    @RequiresPermissions("vpn:dashboard:view")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        VpnDashboardOverviewVO data = vpnDashboardService.getOverview();
        return success(data);
    }

    @RequiresPermissions("vpn:dashboard:view")
    @GetMapping("/login-trend")
    public AjaxResult loginTrend(@RequestParam(value = "days", defaultValue = "7") int days)
    {
        VpnDashboardLoginTrendVO data = vpnDashboardService.getLoginTrend(days);
        return success(data);
    }

    @RequiresPermissions("vpn:dashboard:view")
    @GetMapping("/distributions")
    public AjaxResult distributions(@RequestParam(value = "days", defaultValue = "7") int days)
    {
        VpnDashboardDistributionsVO data = vpnDashboardService.getDistributions(days);
        return success(data);
    }

    @RequiresPermissions("vpn:dashboard:view")
    @GetMapping("/recent-events")
    public AjaxResult recentEvents(@RequestParam(value = "limit", defaultValue = "10") int limit)
    {
        List<VpnLogininfor> data = vpnDashboardService.getRecentEvents(limit);
        return success(data);
    }
}
