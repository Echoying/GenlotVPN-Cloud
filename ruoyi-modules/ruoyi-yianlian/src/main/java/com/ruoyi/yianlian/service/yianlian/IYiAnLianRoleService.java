package com.ruoyi.yianlian.service.yianlian;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianRoleVO;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListResp;

import java.util.List;

/**
 * 易安联角色Service接口
 *
 * @author ruoyi
 */
public interface IYiAnLianRoleService
{
    /**
     * 查询角色列表
     *
     * @param request 查询参数
     * @return 角色列表
     */
    YiAnLianRoleListResp getRoleList(YiAnLianRoleListRequest request);

    /**
     * 创建角色
     *
     * @param role 角色信息
     * @return 结果
     */
    Boolean create(String appId, YiAnLianRoleVO role);

    /**
     * 修改角色
     *
     * @param role 角色信息
     * @return 结果
     */
    Boolean update(String appId, YiAnLianRoleVO role);

    /**
     * 删除角色
     *
     * @param ids 角色ID列表
     * @return 结果
     */
    Boolean delete(String appId, List<String> ids);
}
