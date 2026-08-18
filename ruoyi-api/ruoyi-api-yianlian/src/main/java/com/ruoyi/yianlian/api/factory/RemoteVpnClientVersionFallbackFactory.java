package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteVpnClientVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 客户端版本策略服务降级处理
 *
 * @author ruoyi
 */
@Component
public class RemoteVpnClientVersionFallbackFactory implements FallbackFactory<RemoteVpnClientVersionService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteVpnClientVersionFallbackFactory.class);

    @Override
    public RemoteVpnClientVersionService create(Throwable throwable)
    {
        log.error("客户端版本策略服务调用失败:{}", throwable.getMessage());
        return source -> R.fail("获取客户端版本策略失败:" + throwable.getMessage());
    }
}
