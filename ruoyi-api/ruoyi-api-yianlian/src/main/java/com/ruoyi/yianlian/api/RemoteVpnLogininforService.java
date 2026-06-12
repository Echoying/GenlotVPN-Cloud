package com.ruoyi.yianlian.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.VpnLogininfor;
import com.ruoyi.yianlian.api.factory.RemoteVpnLogininforFallbackFactory;

/**
 * VPN 登录日志远程服务
 */
@FeignClient(contextId = "remoteVpnLogininforService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteVpnLogininforFallbackFactory.class)
public interface RemoteVpnLogininforService
{
    @PostMapping("/vpnlogininfor")
    R<Boolean> saveLogininfor(@RequestBody VpnLogininfor logininfor,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
