package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.SyncProxyConfigDTO;
import com.ruoyi.yianlian.api.factory.RemoteSyncProxyFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 同步代理 Feign（供 ruoyi-vpn-auth 调用）
 */
@FeignClient(contextId = "remoteSyncProxyService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteSyncProxyFallbackFactory.class)
public interface RemoteSyncProxyService
{
    @GetMapping("/sync-proxy/config/{appId}")
    R<SyncProxyConfigDTO> getSyncProxyConfig(@PathVariable("appId") String appId,
                                             @RequestParam("userId") Long userId,
                                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
