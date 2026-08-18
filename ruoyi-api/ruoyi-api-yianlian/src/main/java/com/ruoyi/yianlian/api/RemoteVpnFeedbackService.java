package com.ruoyi.yianlian.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.VpnFeedbackCreateDTO;
import com.ruoyi.yianlian.api.factory.RemoteVpnFeedbackFallbackFactory;

/**
 * VPN 问题反馈远程服务（供 ruoyi-vpn-auth 调用）
 *
 * @author ruoyi
 */
@FeignClient(contextId = "remoteVpnFeedbackService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteVpnFeedbackFallbackFactory.class)
public interface RemoteVpnFeedbackService
{
    /**
     * 内部落库问题反馈
     *
     * @param dto 创建体
     * @param source 请求来源
     * @return 新反馈 id
     */
    @PostMapping("/vpn/feedback/inner")
    R<Long> create(@RequestBody VpnFeedbackCreateDTO dto,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
