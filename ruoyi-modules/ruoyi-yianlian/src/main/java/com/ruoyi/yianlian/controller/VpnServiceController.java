package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.client.dto.vo.ReqCsServerVO;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncDeferredException;
import com.ruoyi.yianlian.service.sync.orchestrator.YiAnLianSyncOrchestrator;
import com.ruoyi.yianlian.service.vpn.IVpnServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    private YiAnLianSyncOrchestrator syncOrchestrator;

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
        String validateMsg = validateByType(service);
        if (validateMsg != null)
        {
            return error(validateMsg);
        }
        service.setCreateBy(SecurityUtils.getUsername());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_SERVICE, SyncConstants.OP_CREATE,
            service.getAppId(), null, service));
        return success();
    }

    @RequiresPermissions("yianlian:service:edit")
    @Log(title = "VPN应用管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnService service)
    {
        String validateMsg = validateByType(service);
        if (validateMsg != null)
        {
            return error(validateMsg);
        }
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
        // 前端禁用了应用组与线路字段，同步/落库统一以 old 为准
        service.setServiceGroupId(old.getServiceGroupId());
        service.setAppId(old.getAppId());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_SERVICE, SyncConstants.OP_UPDATE,
            old.getAppId(), service.getId(), service));
        return success();
    }

    @RequiresPermissions("yianlian:service:remove")
    @Log(title = "VPN应用管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        boolean anyDeferred = false;
        for (Long id : ids)
        {
            VpnService service = vpnServiceService.selectServiceById(id);
            if (service == null)
            {
                continue;
            }
            try
            {
                syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_SERVICE, SyncConstants.OP_DELETE,
                    service.getAppId(), id, null));
            }
            catch (SyncDeferredException e)
            {
                anyDeferred = true;
            }
        }
        if (anyDeferred)
        {
            throw new SyncDeferredException(null, "部分应用代理暂不可用，已加入补偿队列，稍后自动重试");
        }
        return success();
    }

    /**
     * 按应用类型校验并规整字段。
     * Web应用：应用地址、端口必填，且不携带CS服务器配置；
     * 隧道应用：至少配置一组CS服务器且每组字段完整，地址、端口置空。
     *
     * @return 校验失败时返回错误信息，校验通过返回null
     */
    private String validateByType(VpnService service)
    {
        String type = service.getType();
        if ("web".equals(type))
        {
            if (StringUtils.isEmpty(service.getUrl()))
            {
                return "应用地址不能为空";
            }
            if (StringUtils.isEmpty(service.getWebPort()))
            {
                return "端口不能为空";
            }
            if (!service.getWebPort().matches("^\\d{1,6}$"))
            {
                return "端口必须是1-6位数字";
            }
            // Web应用不需要CS服务器配置
            service.setReqCsServerVos(null);
        }
        else if ("cs".equals(type))
        {
            List<ReqCsServerVO> servers = service.getReqCsServerVos();
            if (servers == null || servers.isEmpty())
            {
                return "隧道应用至少需要配置一组CS服务器";
            }
            for (int i = 0; i < servers.size(); i++)
            {
                ReqCsServerVO server = servers.get(i);
                String prefix = "第" + (i + 1) + "组CS服务器";
                if (StringUtils.isEmpty(server.getProtocolType()))
                {
                    return prefix + "协议类型不能为空";
                }
                if (StringUtils.isEmpty(server.getProtocol()))
                {
                    return prefix + "协议不能为空";
                }
                if (StringUtils.isEmpty(server.getIp()))
                {
                    return prefix + "IP地址不能为空";
                }
                if (StringUtils.isEmpty(server.getPort()))
                {
                    return prefix + "端口不能为空";
                }
            }
            // 隧道应用不需要地址、端口，置空串以便清除数据库中的旧值
            service.setUrl("");
            service.setWebPort("");
        }
        return null;
    }
}
