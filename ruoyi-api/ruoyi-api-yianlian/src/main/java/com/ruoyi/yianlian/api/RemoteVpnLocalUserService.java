package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.VpnChangePasswordRequest;
import com.ruoyi.yianlian.api.domain.VpnDingTalkRobotConfig;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.factory.RemoteVpnLocalUserFallbackFactory;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * VPN本地用户服务
 */
@FeignClient(contextId = "remoteVpnLocalUserService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteVpnLocalUserFallbackFactory.class)
public interface RemoteVpnLocalUserService
{
    @GetMapping("/vpn/local/user/info/{username}")
    R<VpnLoginUser> getUserInfo(@PathVariable("username") String username,
                                @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PutMapping("/vpn/local/user/recordlogin")
    R<Boolean> recordUserLogin(@RequestBody VpnUserInfo vpnUser,
                               @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/vpn/local/user/authorized-lines/{localUserId}")
    R<List<Map<String, Object>>> getAuthorizedLines(@PathVariable("localUserId") Long localUserId,
                                                    @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/vpn/local/user/authorized-check")
    R<Boolean> isAuthorizedForLine(@RequestParam("localUserId") Long localUserId,
                                   @RequestParam("appId") String appId,
                                   @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/vpn/local/user/line-credentials")
    R<Map<String, String>> getLineUserCredentials(@RequestParam("localUserId") Long localUserId,
                                                  @RequestParam("appId") String appId,
                                                  @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 按本地用户 + 选线解析钉钉验证码机器人；无角色配置时 data 为 null
     */
    @GetMapping("/vpn/local/user/dingtalk-robot")
    R<VpnDingTalkRobotConfig> resolveDingTalkRobot(@RequestParam("localUserId") Long localUserId,
                                                   @RequestParam("appId") String appId,
                                                   @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PutMapping("/vpn/local/user/changePassword")
    R<Boolean> changePassword(@RequestBody VpnChangePasswordRequest request,
                              @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
