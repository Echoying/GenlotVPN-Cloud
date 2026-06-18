package com.ruoyi.yianlian.service.sync;

import com.ruoyi.yianlian.domain.vo.SyncProxyConfigVO;

/**
 * 同步代理配置服务
 */
public interface ISyncProxyService
{
    /**
     * 获取指定线路的同步代理配置（校验用户角色与线路）
     *
     * @param appId  线路 ID
     * @param userId VPN 用户 ID
     */
    SyncProxyConfigVO getSyncProxyConfig(String appId, Long userId);
}
