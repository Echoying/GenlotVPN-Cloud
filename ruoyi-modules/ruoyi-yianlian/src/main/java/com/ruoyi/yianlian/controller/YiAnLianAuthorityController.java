package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.yianlian.client.dto.YiAnLianAuthorityListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianAuthorityListResp;
import com.ruoyi.yianlian.client.dto.YiAnLianGroupAuthRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserAuthRequest;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 易安联权限管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/yianlian/authority")
public class YiAnLianAuthorityController extends BaseController
{
    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IYiAnLianAuthorityService yiAnLianAuthorityService;

    /**
     * 查询权限列表
     */
    @RequiresPermissions("yianlian:authority:list")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody YiAnLianAuthorityListRequest request)
    {
        List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
        List<YiAnLianAuthorityListResp> resultList = new ArrayList<>();
        for (LineApp lineApp : lineApps) {
            request.setAppId(lineApp.getAppId());
            YiAnLianAuthorityListResp resp = yiAnLianAuthorityService.getAuthorityList(request);
            if (resp != null) {
                resultList.add(resp);
            }
        }
        return success(resultList);
    }

    /**
     * 授予用户权限
     */
    @RequiresPermissions("yianlian:authority:edit")
    @Log(title = "易安联用户授权", businessType = BusinessType.UPDATE)
    @PostMapping("/user")
    public AjaxResult grantUserAuthority(@Validated @RequestBody List<YiAnLianUserAuthRequest> requestList)
    {
        List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
        for (LineApp lineApp : lineApps) {
            Boolean ret = yiAnLianAuthorityService.grantUserAuthority(lineApp.getAppId(), requestList);
            if (!Boolean.TRUE.equals(ret)) {
                return error("授予用户权限失败，线路: " + lineApp.getAppId());
            }
        }
        return success();
    }

    /**
     * 授予组织权限
     */
    @RequiresPermissions("yianlian:authority:edit")
    @Log(title = "易安联组织授权", businessType = BusinessType.UPDATE)
    @PostMapping("/group")
    public AjaxResult grantGroupAuthority(@Validated @RequestBody List<YiAnLianGroupAuthRequest> requestList)
    {
        List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
        for (LineApp lineApp : lineApps) {
            Boolean ret = yiAnLianAuthorityService.grantGroupAuthority(lineApp.getAppId(), requestList);
            if (!Boolean.TRUE.equals(ret)) {
                return error("授予组织权限失败，线路: " + lineApp.getAppId());
            }
        }
        return success();
    }
}
