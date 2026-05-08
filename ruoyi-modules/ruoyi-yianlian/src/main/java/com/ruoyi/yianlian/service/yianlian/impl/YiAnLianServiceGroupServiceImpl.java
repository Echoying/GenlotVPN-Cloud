package com.ruoyi.yianlian.service.yianlian.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceGroupVO;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceGroupListResp;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianServiceGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 易安联应用组服务实现
 */
@Service
@Slf4j
public class YiAnLianServiceGroupServiceImpl implements IYiAnLianServiceGroupService
{
    @Autowired
    private OpenApiClient openApiClient;

    @Override
    public YiAnLianServiceGroupListResp getServiceGroupList(String appId)
    {
        try {
            Map<String, Object> emptyRequest = new HashMap<>();
            return openApiClient.post(appId, YiAnLianConstants.serviceGroupListPath, emptyRequest, YiAnLianServiceGroupListResp.class);
        }
        catch (Exception e) {
            log.error("获取应用组列表失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Boolean create(String appId, YiAnLianServiceGroupVO serviceGroup)
    {
        if (StringUtils.isNull(serviceGroup.getName()) || StringUtils.isNull(serviceGroup.getParentId())
                || StringUtils.isNull(serviceGroup.getPath())) {
            log.error("创建应用组参数错误: {}", serviceGroup);
            return false;
        }

        try {
            return openApiClient.post(appId, YiAnLianConstants.serviceGroupCreatePath, serviceGroup, Boolean.class);
        }
        catch (Exception e) {
            log.error("创建应用组失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Boolean update(String appId, YiAnLianServiceGroupVO serviceGroup)
    {
        if (StringUtils.isNull(serviceGroup.getId()) || StringUtils.isNull(serviceGroup.getName())) {
            log.error("更新应用组参数错误: {}", serviceGroup);
            return false;
        }

        try {
            return openApiClient.post(appId, YiAnLianConstants.serviceGroupUpdatePath, serviceGroup, Boolean.class);
        }
        catch (Exception e) {
            log.error("更新应用组失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Boolean delete(String appId, List<String> ids)
    {
        if (StringUtils.isNull(ids) || ids.isEmpty()) {
            log.error("删除应用组参数错误: ids为空");
            return false;
        }

        try {
            return openApiClient.post(appId, YiAnLianConstants.serviceGroupDeletePath, ids, Boolean.class);
        }
        catch (Exception e) {
            log.error("删除应用组失败: {}", e.getMessage());
        }
        return false;
    }
}
