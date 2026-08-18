package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.VpnClientVersionPolicy;

/**
 * VPN 客户端版本策略 数据层
 *
 * @author ruoyi
 */
public interface VpnClientVersionPolicyMapper
{
    /**
     * 按主键查询
     *
     * @param id 主键
     * @return 策略
     */
    VpnClientVersionPolicy selectById(Long id);

    /**
     * 更新策略
     *
     * @param policy 策略
     * @return 影响行数
     */
    int update(VpnClientVersionPolicy policy);
}
