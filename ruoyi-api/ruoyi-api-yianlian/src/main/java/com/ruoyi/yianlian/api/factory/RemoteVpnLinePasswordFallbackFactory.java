package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteVpnLinePasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 线路密码轮换服务降级处理
 */
@Component
public class RemoteVpnLinePasswordFallbackFactory implements FallbackFactory<RemoteVpnLinePasswordService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteVpnLinePasswordFallbackFactory.class);

    @Override
    public RemoteVpnLinePasswordService create(Throwable throwable)
    {
        log.error("线路密码轮换服务调用失败:{}", throwable.getMessage());
        return (localUserId, appId, source) -> R.fail("轮换线路密码失败:" + throwable.getMessage());
    }
}
