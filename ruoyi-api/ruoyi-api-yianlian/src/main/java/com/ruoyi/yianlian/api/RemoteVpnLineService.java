package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.factory.RemoteVpnLineFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

/**
 * VPN 线路 Feign（供 ruoyi-vpn-auth 调用）
 */
@FeignClient(contextId = "remoteVpnLineService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteVpnLineFallbackFactory.class)
public interface RemoteVpnLineService
{
    /**
     * 获取 VPN 客户端可选的启用线路列表（不含敏感密钥明文）
     */
    @GetMapping("/line/public/list")
    R<List<Map<String, Object>>> listPublicLines(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
