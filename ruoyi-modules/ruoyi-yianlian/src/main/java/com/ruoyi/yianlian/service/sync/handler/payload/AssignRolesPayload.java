package com.ruoyi.yianlian.service.sync.handler.payload;

import java.io.Serializable;

/**
 * 分配角色命令载荷
 */
public class AssignRolesPayload implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long[] roleIds;

    public AssignRolesPayload()
    {
    }

    public AssignRolesPayload(Long userId, Long[] roleIds)
    {
        this.userId = userId;
        this.roleIds = roleIds;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long[] getRoleIds()
    {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds)
    {
        this.roleIds = roleIds;
    }
}
