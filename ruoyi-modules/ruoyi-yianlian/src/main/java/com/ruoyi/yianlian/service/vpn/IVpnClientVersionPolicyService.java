package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.VpnClientVersionPolicy;

/**
 * VPN 客户端版本策略 服务层
 *
 * @author ruoyi
 */
public interface IVpnClientVersionPolicyService
{
    /**
     * 获取当前策略（单行 id=1，带短缓存）
     *
     * @return 策略
     */
    VpnClientVersionPolicy getPolicy();

    /**
     * 更新策略（校验后落库并清缓存）
     *
     * @param policy 策略
     * @return 影响行数
     */
    int updatePolicy(VpnClientVersionPolicy policy);
}
