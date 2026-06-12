package com.ruoyi.yianlian.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.yianlian.domain.VpnLogininfor;
import com.ruoyi.yianlian.service.IVpnLogininforService;

/**
 * VPN访问记录
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpnlogininfor")
public class VpnLogininforController extends BaseController
{
    @Autowired
    private IVpnLogininforService logininforService;

    @RequiresPermissions("vpn:logininfor:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnLogininfor logininfor)
    {
        startPage();
        List<VpnLogininfor> list = logininforService.selectLogininforList(logininfor);
        return getDataTable(list);
    }

    @Log(title = "VPN登录日志", businessType = BusinessType.EXPORT)
    @RequiresPermissions("vpn:logininfor:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpnLogininfor logininfor)
    {
        List<VpnLogininfor> list = logininforService.selectLogininforList(logininfor);
        ExcelUtil<VpnLogininfor> util = new ExcelUtil<VpnLogininfor>(VpnLogininfor.class);
        util.exportExcel(response, list, "VPN登录日志");
    }

    @RequiresPermissions("vpn:logininfor:remove")
    @Log(title = "VPN登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public AjaxResult remove(@PathVariable Long[] infoIds)
    {
        return toAjax(logininforService.deleteLogininforByIds(infoIds));
    }

    @RequiresPermissions("vpn:logininfor:remove")
    @Log(title = "VPN登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        logininforService.cleanLogininfor();
        return success();
    }

    @InnerAuth
    @PostMapping
    public AjaxResult add(@RequestBody VpnLogininfor logininfor)
    {
        return toAjax(logininforService.insertLogininfor(logininfor));
    }
}
