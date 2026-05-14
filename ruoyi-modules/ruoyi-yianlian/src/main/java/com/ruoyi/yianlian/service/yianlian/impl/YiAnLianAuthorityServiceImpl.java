package com.ruoyi.yianlian.service.yianlian.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.YiAnLianAuthorityListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianAuthorityListResp;
import com.ruoyi.yianlian.client.dto.YiAnLianGroupAuthRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserAuthRequest;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianAuthorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 易安联授权服务实现
 */
@Service
@Slf4j
public class YiAnLianAuthorityServiceImpl implements IYiAnLianAuthorityService
{
    @Autowired
    private OpenApiClient openApiClient;

    @Override
    public YiAnLianAuthorityListResp getAuthorityList(YiAnLianAuthorityListRequest request) {
        try {
            return openApiClient.post(request.getAppId(), YiAnLianConstants.authorityListPath, request, YiAnLianAuthorityListResp.class);
        } catch (Exception e) {
            log.error("查询易安联权限列表失败, appId: {}, request: {}, 错误: ", request.getAppId(), request, e);
        }
        return null;
    }

    @Override
    public Boolean grantUserAuthority(String appId, List<YiAnLianUserAuthRequest> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            log.error("授予用户权限参数错误: 请求列表为空");
            return false;
        }
        for (YiAnLianUserAuthRequest request : requestList) {
            if (StringUtils.isNull(request.getUserId())) {
                log.error("授予用户权限参数错误: userId不能为空 {}", request);
                return false;
            }
            if (StringUtils.isNull(request.getRoleId()) && StringUtils.isNull(request.getServiceId()) && StringUtils.isNull(request.getServiceGroupId())) {
                log.error("授予用户权限参数错误: roleId/serviceId/serviceGroupId至少需要一个 {}", request);
                return false;
            }
        }

        try {
            return Boolean.TRUE.equals(openApiClient.post(appId, YiAnLianConstants.authorityUserPath, requestList, Boolean.class));
        } catch (Exception e) {
            log.error("授予易安联用户权限失败, appId: {}, requestList: {}, 错误: ", appId, requestList, e);
        }
        return false;
    }

    @Override
    public Boolean grantGroupAuthority(String appId, List<YiAnLianGroupAuthRequest> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            log.error("授予组织权限参数错误: 请求列表为空");
            return false;
        }
        for (YiAnLianGroupAuthRequest request : requestList) {
            if (StringUtils.isNull(request.getGroupId())) {
                log.error("授予组织权限参数错误: groupId不能为空 {}", request);
                return false;
            }
            if (StringUtils.isNull(request.getRoleId()) && StringUtils.isNull(request.getServiceId()) && StringUtils.isNull(request.getServiceGroupId())) {
                log.error("授予组织权限参数错误: roleId/serviceId/serviceGroupId至少需要一个 {}", request);
                return false;
            }
        }

        try {
            return Boolean.TRUE.equals(openApiClient.post(appId, YiAnLianConstants.authorityGroupPath, requestList, Boolean.class));
        } catch (Exception e) {
            log.error("授予易安联组织权限失败, appId: {}, requestList: {}, 错误: ", appId, requestList, e);
        }
        return false;
    }

}
