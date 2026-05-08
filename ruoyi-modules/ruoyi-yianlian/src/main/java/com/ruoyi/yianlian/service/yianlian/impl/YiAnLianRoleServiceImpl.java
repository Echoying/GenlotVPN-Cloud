package com.ruoyi.yianlian.service.yianlian.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianRoleVO;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListResp;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 易安联角色Service业务层处理
 *
 * @author ruoyi
 */
@Service
@Slf4j
public class YiAnLianRoleServiceImpl implements IYiAnLianRoleService
{
    @Autowired
    private OpenApiClient openApiClient;

    /**
     * 查询角色列表
     *
     * @param request 查询参数
     * @return 角色列表
     */
    @Override
    public YiAnLianRoleListResp getRoleList(YiAnLianRoleListRequest request)
    {
        return openApiClient.post(request.getAppId(), YiAnLianConstants.roleListPath, request, YiAnLianRoleListResp.class);
    }

    /**
     * 创建角色
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public Boolean create(String appId, YiAnLianRoleVO role)
    {
        if (StringUtils.isNull(role))
        {
            log.error("创建角色参数错误: role为空");
            return false;
        }

        if (StringUtils.isEmpty(role.getName()))
        {
            log.error("创建角色参数错误: name为空");
            return false;
        }

        return openApiClient.post(appId, YiAnLianConstants.roleCreatePath, role, Boolean.class);
    }

    /**
     * 修改角色
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public Boolean update(String appId, YiAnLianRoleVO role)
    {
        if (StringUtils.isNull(role))
        {
            log.error("修改角色参数错误: role为空");
            return false;
        }

        if (StringUtils.isEmpty(role.getId()))
        {
            log.error("修改角色参数错误: id为空");
            return false;
        }

        if (StringUtils.isEmpty(role.getName()))
        {
            log.error("修改角色参数错误: name为空");
            return false;
        }

        return openApiClient.post(appId, YiAnLianConstants.roleUpdatePath, role, Boolean.class);
    }

    /**
     * 删除角色
     *
     * @param ids 角色ID列表
     * @return 结果
     */
    @Override
    public Boolean delete(String appId, List<String> ids)
    {
        if (StringUtils.isNull(ids) || ids.isEmpty())
        {
            log.error("删除角色参数错误: ids为空");
            return false;
        }

        return openApiClient.post(appId, YiAnLianConstants.roleDeletePath, ids, Boolean.class);
    }
}
