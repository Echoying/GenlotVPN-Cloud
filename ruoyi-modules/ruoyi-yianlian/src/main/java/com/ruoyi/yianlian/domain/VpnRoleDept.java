package com.ruoyi.yianlian.domain;

import lombok.Data;

/**
 * VPN角色和部门关联 vpn_role_dept
 *
 * @author ruoyi
 */
@Data
public class VpnRoleDept
{
    /** 角色ID */
    private Long roleId;

    /** 部门ID */
    private Long deptId;
}
