package com.ruoyi.yianlian.domain;

import java.util.List;

/**
 * 角色授权批量保存DTO
 *
 * @author ruoyi
 */
public class YalRoleAuthBatchDTO
{
    /** 角色ID */
    private Long roleId;

    /** 授权列表 */
    private List<YalRoleAuth> authList;

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public List<YalRoleAuth> getAuthList()
    {
        return authList;
    }

    public void setAuthList(List<YalRoleAuth> authList)
    {
        this.authList = authList;
    }
}
