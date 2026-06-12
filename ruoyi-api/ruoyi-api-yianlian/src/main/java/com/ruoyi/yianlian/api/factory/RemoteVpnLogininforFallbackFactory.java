package com.ruoyi.yianlian.api.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteVpnLogininforService;
import com.ruoyi.yianlian.api.domain.VpnLogininfor;

/**
 * VPN 登录日志服务降级处理
 */
@Component
public class RemoteVpnLogininforFallbackFactory implements FallbackFactory<RemoteVpnLogininforService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteVpnLogininforFallbackFactory.class);

    @Override
    public RemoteVpnLogininforService create(Throwable throwable)
    {
        log.error("VPN登录日志服务调用失败:{}", throwable.getMessage());
        return new RemoteVpnLogininforService()
        {
            @Override
            public R<Boolean> saveLogininfor(VpnLogininfor logininfor, String source)
            {
                return R.fail("保存VPN登录日志失败:" + throwable.getMessage());
            }
        };
    }
}
