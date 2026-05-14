package com.ruoyi.yianlian.service.yianlian;

import com.ruoyi.yianlian.client.dto.YiAnLianAuthorityListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianAuthorityListResp;
import com.ruoyi.yianlian.client.dto.YiAnLianGroupAuthRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserAuthRequest;

import java.util.List;

/**
 * 易安联授权服务
 */
public interface IYiAnLianAuthorityService
{
    /**
     * 5.6.1权限列表
     *
     * @param request 请求参数
     * @return 权限列表响应
     */
    YiAnLianAuthorityListResp getAuthorityList(YiAnLianAuthorityListRequest request);

    /**
     * 5.6.2授予用户权限
     *
     * @param appId 应用ID
     * @param requestList 请求参数列表（支持批量）
     * @return 是否成功
     */
    Boolean grantUserAuthority(String appId, List<YiAnLianUserAuthRequest> requestList);

    /**
     * 5.6.3授予组织权限
     *
     * @param appId 应用ID
     * @param requestList 请求参数列表（支持批量）
     * @return 是否成功
     */
    Boolean grantGroupAuthority(String appId, List<YiAnLianGroupAuthRequest> requestList);
}
