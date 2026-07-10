package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceListRequest;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceVO;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.vpn.IVpnServiceGroupService;
import com.ruoyi.yianlian.service.vpn.IVpnServiceService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 应用同步处理器：远程优先（远程失败则本地不写/不改），成功后落库 yianlianKey。
 */
@Component
public class ServiceSyncHandler implements SyncHandler
{
    private static final Logger log = LoggerFactory.getLogger(ServiceSyncHandler.class);

    @Autowired
    private IVpnServiceService vpnServiceService;

    @Autowired
    private IVpnServiceGroupService serviceGroupService;

    @Autowired
    private IYiAnLianServiceService yiAnLianServiceService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_SERVICE;
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        switch (command.getOperation())
        {
            case SyncConstants.OP_CREATE:
                doCreate(command.parsePayload(VpnService.class));
                break;
            case SyncConstants.OP_UPDATE:
                doUpdate(command.parsePayload(VpnService.class));
                break;
            case SyncConstants.OP_DELETE:
                doDelete(Long.valueOf(command.getBizId()));
                break;
            default:
                throw new YiAnLianException("应用同步不支持的操作：" + command.getOperation());
        }
    }

    private void doCreate(VpnService service)
    {
        YiAnLianServiceVO vo = buildYiAnLianVO(service);
        if (vo == null)
        {
            throw new YiAnLianException("应用组不存在或未同步到易安联");
        }
        Boolean syncResult = yiAnLianServiceService.create(service.getAppId(), vo);
        if (syncResult == null || !syncResult)
        {
            throw new YiAnLianException("同步创建应用到易安联失败");
        }
        String yianlianKey = queryYiAnLianServiceId(service.getAppId(), service.getName(), vo.getServiceGroupIds().get(0));
        if (StringUtils.isEmpty(yianlianKey))
        {
            throw new YiAnLianException("创建应用成功但未查询到易安联应用ID");
        }
        service.setYianlianKey(yianlianKey);
        vpnServiceService.insertService(service);
    }

    private void doUpdate(VpnService service)
    {
        VpnService old = vpnServiceService.selectServiceById(service.getId());
        if (old == null)
        {
            throw new YiAnLianException("应用不存在");
        }
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
                    throw new YiAnLianException("同步更新应用到易安联失败");
                }
            }
        }
        vpnServiceService.updateService(service);
    }

    private void doDelete(Long id)
    {
        VpnService service = vpnServiceService.selectServiceById(id);
        if (service == null)
        {
            return;
        }
        if (StringUtils.isNotEmpty(service.getYianlianKey()))
        {
            Boolean syncResult = yiAnLianServiceService.delete(service.getAppId(),
                Collections.singletonList(service.getYianlianKey()));
            if (syncResult == null || !syncResult)
            {
                throw new YiAnLianException("同步删除应用到易安联失败: " + service.getName());
            }
        }
        vpnServiceService.deleteServiceByIds(new Long[]{id});
    }

    private YiAnLianServiceVO buildYiAnLianVO(VpnService service)
    {
        VpnServiceGroup group = serviceGroupService.selectServiceGroupById(service.getServiceGroupId());
        if (group == null || StringUtils.isEmpty(group.getYianlianKey()))
        {
            log.error("应用组不存在或未同步到易安联, serviceGroupId: {}", service.getServiceGroupId());
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
            log.error("查询易安联应用ID失败, appId: {}, serviceName: {}", appId, serviceName, e);
        }
        return null;
    }
}
