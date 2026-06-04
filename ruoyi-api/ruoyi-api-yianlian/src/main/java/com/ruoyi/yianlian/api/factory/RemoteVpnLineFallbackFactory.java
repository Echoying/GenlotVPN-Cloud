package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteVpnLineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 线路服务降级处理
 */
@Component
public class RemoteVpnLineFallbackFactory implements FallbackFactory<RemoteVpnLineService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteVpnLineFallbackFactory.class);

    @Override
    public RemoteVpnLineService create(Throwable throwable)
    {
        log.error("线路服务调用失败:{}", throwable.getMessage());
        return source -> R.fail("获取线路列表失败:" + throwable.getMessage());
    }
}
