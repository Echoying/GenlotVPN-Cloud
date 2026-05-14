package com.ruoyi.yianlian.service.yianlian.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptAuthRequest;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianAuthorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    public Boolean grantAuthority(String appId, YiAnLianDeptAuthRequest request) {
        if (StringUtils.isNull(request.getGroupId())) {
            log.error("授予组织权限参数错误: groupId不能为空 {}", request);
            return false;
        }
        if (StringUtils.isNull(request.getRoleId()) && StringUtils.isNull(request.getServiceId()) && StringUtils.isNull(request.getServiceGroupId())) {
            log.error("授予组织权限参数错误: roleId/serviceId/serviceGroupId至少需要一个 {}", request);
            return false;
        }

        try {
            List<YiAnLianDeptAuthRequest> requestList = new ArrayList<>();
            requestList.add(request);
            Boolean result = openApiClient.post(appId, YiAnLianConstants.authorityGroupPath, requestList, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("授予易安联组织权限失败, appId: {}, request: {}, 错误: ", appId, request, e);
        }
        return false;
    }
}
