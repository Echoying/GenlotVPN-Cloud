package com.ruoyi.yianlian.service.yianlian.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceVO;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.YiAnLianDeleteRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceListRequest;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianServiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 易安联应用服务实现
 */
@Service
@Slf4j
public class YiAnLianServiceServiceImpl implements IYiAnLianServiceService
{
    @Autowired
    private OpenApiClient openApiClient;

    @Override
    public List<YiAnLianServiceVO> getServiceList(YiAnLianServiceListRequest request)
    {
        if (StringUtils.isNull(request.getAppId()))
        {
            log.error("查询应用列表参数错误: appId为空");
            return null;
        }
        if (StringUtils.isNull(request.getServiceGroupId()))
        {
            log.error("查询应用列表参数错误: serviceGroupId为空");
            return null;
        }
        try
        {
            return openApiClient.postForList(request.getAppId(), YiAnLianConstants.serviceListPath, request, YiAnLianServiceVO.class);
        }
        catch (Exception e)
        {
            log.error("获取应用列表失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Boolean create(String appId, YiAnLianServiceVO service)
    {
        if (StringUtils.isNull(service.getName()) || StringUtils.isNull(service.getType())
                || StringUtils.isNull(service.getUrl()) || StringUtils.isNull(service.getBrowserType())
                || StringUtils.isNull(service.getWebPort()) || StringUtils.isNull(service.getCreditLevelId())
                || StringUtils.isNull(service.getServiceGroupIds()) || service.getServiceGroupIds().isEmpty()
                || StringUtils.isNull(service.getIcon()) || StringUtils.isNull(service.getIfShow())
                || StringUtils.isNull(service.getSecondAuthEnable()) || StringUtils.isNull(service.getIfSelfApply())
                || StringUtils.isNull(service.getIfSAlarmTip()) || StringUtils.isNull(service.getIfCustomAlarmContent())
                || StringUtils.isNull(service.getCreateType()))
        {
            log.error("创建应用参数错误: {}", service);
            return false;
        }
        if (service.getIfCustomAlarmContent() && StringUtils.isNull(service.getCustomAlarmContent()))
        {
            log.error("创建应用参数错误: 自定义告警内容为空");
            return false;
        }
        try
        {
            return openApiClient.post(appId, YiAnLianConstants.serviceCreatePath, service, Boolean.class);
        }
        catch (Exception e)
        {
            log.error("创建应用失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Boolean update(String appId, YiAnLianServiceVO service)
    {
        if (StringUtils.isNull(service.getId()) || StringUtils.isNull(service.getName())
                || StringUtils.isNull(service.getType()) || StringUtils.isNull(service.getUrl())
                || StringUtils.isNull(service.getBrowserType()) || StringUtils.isNull(service.getWebPort())
                || StringUtils.isNull(service.getCreditLevelId())
                || StringUtils.isNull(service.getServiceGroupIds()) || service.getServiceGroupIds().isEmpty()
                || StringUtils.isNull(service.getIcon()) || StringUtils.isNull(service.getIfShow())
                || StringUtils.isNull(service.getSecondAuthEnable()) || StringUtils.isNull(service.getIfSelfApply())
                || StringUtils.isNull(service.getIfSAlarmTip()) || StringUtils.isNull(service.getIfCustomAlarmContent())
                || StringUtils.isNull(service.getCreateType()))
        {
            log.error("更新应用参数错误: {}", service);
            return false;
        }
        if (service.getIfCustomAlarmContent() && StringUtils.isNull(service.getCustomAlarmContent()))
        {
            log.error("更新应用参数错误: 自定义告警内容为空");
            return false;
        }
        try
        {
            return openApiClient.post(appId, YiAnLianConstants.serviceUpdatePath, service, Boolean.class);
        }
        catch (Exception e)
        {
            log.error("更新应用失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Boolean delete(String appId, List<String> ids)
    {
        if (StringUtils.isNull(ids) || ids.isEmpty())
        {
            log.error("删除应用参数错误: ids为空");
            return false;
        }
        try
        {
            YiAnLianDeleteRequest request = new YiAnLianDeleteRequest(ids);
            return openApiClient.post(appId, YiAnLianConstants.serviceDeletePath, request, Boolean.class);
        }
        catch (Exception e)
        {
            log.error("删除应用失败: {}", e.getMessage());
        }
        return false;
    }
}
