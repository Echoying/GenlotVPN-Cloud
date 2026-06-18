package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteSyncProxyService;
import com.ruoyi.yianlian.api.domain.SyncProxyConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 同步代理服务降级
 */
@Component
public class RemoteSyncProxyFallbackFactory implements FallbackFactory<RemoteSyncProxyService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteSyncProxyFallbackFactory.class);

    @Override
    public RemoteSyncProxyService create(Throwable throwable)
    {
        log.error("同步代理服务调用失败:{}", throwable.getMessage());
        return (appId, userId, source) -> R.fail("获取同步代理配置失败:" + throwable.getMessage());
    }
}
