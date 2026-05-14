package com.ruoyi.yianlian.service.yianlian;

import com.ruoyi.yianlian.client.dto.YiAnLianDeptAuthRequest;

/**
 * 易安联授权服务
 */
public interface IYiAnLianAuthorityService
{
    /**
     * 5.6.3授予组织权限
     *
     * @param appId 应用ID
     * @param request 请求参数
     * @return 是否成功
     */
    Boolean grantAuthority(String appId, YiAnLianDeptAuthRequest request);
}
