package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.VpnClientVersionPolicyDTO;
import com.ruoyi.yianlian.api.factory.RemoteVpnClientVersionFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * VPN 客户端版本策略 Feign（供 ruoyi-vpn-auth 调用）
 *
 * @author ruoyi
 */
@FeignClient(contextId = "remoteVpnClientVersionService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteVpnClientVersionFallbackFactory.class)
public interface RemoteVpnClientVersionService
{
    /**
     * 获取客户端版本策略
     *
     * @param source 请求来源
     * @return 策略
     */
    @GetMapping("/vpn/clientVersion/inner")
    R<VpnClientVersionPolicyDTO> getPolicy(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
