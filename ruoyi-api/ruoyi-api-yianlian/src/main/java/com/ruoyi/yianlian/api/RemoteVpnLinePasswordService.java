package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.factory.RemoteVpnLinePasswordFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 线路密码轮换 Feign（供 ruoyi-vpn-auth 调用）
 *
 * 单独成客户端是为了给改密链路配更长的读超时（易安联改密可达十几秒），
 * 不牵连登录查询等短调用的 10s 默认超时。
 */
@FeignClient(contextId = "remoteVpnLinePasswordService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteVpnLinePasswordFallbackFactory.class)
public interface RemoteVpnLinePasswordService
{
    @PutMapping("/vpn/local/user/rotate-line-password")
    R<Boolean> rotateLinePassword(@RequestParam("localUserId") Long localUserId,
                                  @RequestParam("appId") String appId,
                                  @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
