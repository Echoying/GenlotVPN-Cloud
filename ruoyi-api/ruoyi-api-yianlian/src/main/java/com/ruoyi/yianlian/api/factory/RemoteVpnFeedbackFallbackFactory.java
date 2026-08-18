package com.ruoyi.yianlian.api.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteVpnFeedbackService;

/**
 * VPN 问题反馈服务降级处理
 *
 * @author ruoyi
 */
@Component
public class RemoteVpnFeedbackFallbackFactory implements FallbackFactory<RemoteVpnFeedbackService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteVpnFeedbackFallbackFactory.class);

    @Override
    public RemoteVpnFeedbackService create(Throwable throwable)
    {
        log.error("VPN问题反馈服务调用失败:{}", throwable.getMessage());
        return (dto, source) -> R.fail("提交问题反馈失败:" + throwable.getMessage());
    }
}
