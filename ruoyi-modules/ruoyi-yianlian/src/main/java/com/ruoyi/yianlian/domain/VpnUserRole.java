package com.ruoyi.yianlian.domain;

import lombok.Data;

/**
 * VPN用户和角色关联 vpn_user_role
 *
 * @author ruoyi
 */
@Data
public class VpnUserRole
{
    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;
}
