package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceListRequest;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceVO;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.service.vpn.IVpnServiceGroupService;
import com.ruoyi.yianlian.service.vpn.IVpnServiceService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * VPN应用管理
 */
@RestController
@RequestMapping("/vpn/service")
public class VpnServiceController extends BaseController
{
    @Autowired
    private IVpnServiceService vpnServiceService;

    @Autowired
    private IVpnServiceGroupService serviceGroupService;

    @Autowired
    private IYiAnLianServiceService yiAnLianServiceService;

    @RequiresPermissions("yianlian:service:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnService service)
    {
        startPage();
        List<VpnService> list = vpnServiceService.selectServiceList(service);
        return getDataTable(list);
    }

    @RequiresPermissions("yianlian:service:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(vpnServiceService.selectServiceById(id));
    }

    @RequiresPermissions("yianlian:service:add")
    @Log(title = "VPN应用管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnService service)
    {
        service.setCreateBy(SecurityUtils.getUsername());
        // 先同步创建到易安联，成功后再插入本地数据库
        YiAnLianServiceVO vo = buildYiAnLianVO(service);
        if (vo == null)
        {
            return error("应用组不存在或未同步到易安联");
        }

        Boolean syncResult = yiAnLianServiceService.create(service.getAppId(), vo);
        if (syncResult == null || !syncResult)
        {
            logger.error("同步创建应用到易安联失败, appId: {}, name: {}", service.getAppId(), service.getName());
            return error("同步创建应用到易安联失败");
        }

        // 创建成功后，通过list接口查询获取易安联应用ID
        String yianlianKey = queryYiAnLianServiceId(service.getAppId(), service.getName(), vo.getServiceGroupIds().get(0));
        if (StringUtils.isEmpty(yianlianKey))
        {
            logger.error("创建应用成功但未查询到易安联应用ID, appId: {}, name: {}", service.getAppId(), service.getName());
            return error("创建应用成功但未查询到易安联应用ID");
        }

        service.setYianlianKey(yianlianKey);
        return toAjax(vpnServiceService.insertService(service));
    }

    @RequiresPermissions("yianlian:service:edit")
    @Log(title = "VPN应用管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnService service)
    {
        VpnService old = vpnServiceService.selectServiceById(service.getId());
        if (old == null)
        {
            return error("应用不存在");
        }
        // 不允许修改应用组和线路
        if (service.getServiceGroupId() != null && !service.getServiceGroupId().equals(old.getServiceGroupId()))
        {
            return error("不允许修改应用组");
        }
        if (service.getAppId() != null && !service.getAppId().equals(old.getAppId()))
        {
            return error("不允许修改线路");
        }
        service.setUpdateBy(SecurityUtils.getUsername());

        // 同步更新到易安联（使用old对象的serviceGroupId和appId，因为前端禁用了这些字段）
        if (StringUtils.isNotEmpty(old.getYianlianKey()))
        {
            service.setServiceGroupId(old.getServiceGroupId());
            service.setAppId(old.getAppId());
            YiAnLianServiceVO vo = buildYiAnLianVO(service);
            if (vo != null)
            {
                vo.setId(old.getYianlianKey());
                Boolean syncResult = yiAnLianServiceService.update(old.getAppId(), vo);
                if (syncResult == null || !syncResult)
                {
                    logger.error("同步更新应用到易安联失败, appId: {}, yianlianKey: {}", old.getAppId(), old.getYianlianKey());
                }
            }
        }

        return toAjax(vpnServiceService.updateService(service));
    }

    @RequiresPermissions("yianlian:service:remove")
    @Log(title = "VPN应用管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        // 先同步删除易安联的应用，失败则不删除本地数据库
        for (Long id : ids)
        {
            VpnService service = vpnServiceService.selectServiceById(id);
            if (service != null && StringUtils.isNotEmpty(service.getYianlianKey()))
            {
                List<String> deleteIds = Collections.singletonList(service.getYianlianKey());
                Boolean syncResult = yiAnLianServiceService.delete(service.getAppId(), deleteIds);
                if (syncResult == null || !syncResult)
                {
                    logger.error("同步删除应用到易安联失败, appId: {}, yianlianKey: {}", service.getAppId(), service.getYianlianKey());
                    return error("同步删除应用到易安联失败: " + service.getName());
                }
            }
        }

        // 易安联删除成功后，再删除本地数据库
        return toAjax(vpnServiceService.deleteServiceByIds(ids));
    }

    /**
     * 构建易安联应用VO
     */
    private YiAnLianServiceVO buildYiAnLianVO(VpnService service)
    {
        VpnServiceGroup group = serviceGroupService.selectServiceGroupById(service.getServiceGroupId());
        if (group == null || StringUtils.isEmpty(group.getYianlianKey()))
        {
            logger.error("应用组不存在或未同步到易安联, serviceGroupId: {}", service.getServiceGroupId());
            return null;
        }
        YiAnLianServiceVO vo = new YiAnLianServiceVO();
        vo.setName(service.getName());
        vo.setType(service.getType());
        vo.setUrl(service.getUrl());
        vo.setBrowserType(service.getBrowserType());
        vo.setWebPort(service.getWebPort());
        vo.setCreditLevelId(service.getCreditLevelId() != null ? service.getCreditLevelId() : "000000000002");
        vo.setServiceGroupIds(Collections.singletonList(group.getYianlianKey()));
        vo.setIcon(service.getIcon() != null ? service.getIcon() : "/diy/default-house.svg");
        vo.setIfShow(service.getIfShow() != null ? service.getIfShow() : true);
        vo.setSecondAuthEnable(service.getSecondAuthEnable() != null ? service.getSecondAuthEnable() : "1");
        vo.setIfSelfApply(service.getIfSelfApply() != null ? service.getIfSelfApply() : true);
        vo.setIfSAlarmTip(service.getIfSAlarmTip() != null ? service.getIfSAlarmTip() : false);
        vo.setIfCustomAlarmContent(service.getIfCustomAlarmContent() != null ? service.getIfCustomAlarmContent() : false);
        vo.setCustomAlarmContent(service.getCustomAlarmContent());
        vo.setCreateType(service.getCreateType() != null ? service.getCreateType() : "3");
        vo.setDescription(service.getDescription());
        vo.setReqCsServerVos(service.getReqCsServerVos());
        return vo;
    }

    /**
     * 查询易安联应用ID
     * 通过应用名称从易安联应用列表中查找对应的应用ID
     */
    private String queryYiAnLianServiceId(String appId, String serviceName, String serviceGroupId)
    {
        try
        {
            YiAnLianServiceListRequest request = new YiAnLianServiceListRequest();
            request.setAppId(appId);
            request.setServiceGroupId(serviceGroupId);
            request.setPageIndex("0");
            request.setPageSize("1000");

            List<YiAnLianServiceVO> serviceList = yiAnLianServiceService.getServiceList(request);
            if (serviceList != null)
            {
                for (YiAnLianServiceVO item : serviceList)
                {
                    if (serviceName.equals(item.getName()))
                    {
                        return item.getId();
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("查询易安联应用ID失败, appId: {}, serviceName: {}", appId, serviceName, e);
        }
        return null;
    }
}
