package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteLineProbeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 线路探测服务降级处理
 */
@Component
public class RemoteLineProbeFallbackFactory implements FallbackFactory<RemoteLineProbeService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteLineProbeFallbackFactory.class);

    @Override
    public RemoteLineProbeService create(Throwable throwable)
    {
        log.error("线路探测服务调用失败:{}", throwable.getMessage());
        return (limit, source) -> R.fail("线路探测失败:" + throwable.getMessage());
    }
}
