package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.VpnChangePasswordRequest;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.factory.RemoteVpnUserFallbackFactory;
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
 * 易安联服务
 *
 * @author ruoyi
 */
@FeignClient(contextId = "remoteVpnUserService", value = ServiceNameConstants.YIANLIAN_SERVICE, fallbackFactory = RemoteVpnUserFallbackFactory.class)
public interface RemoteVpnUserService {
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/vpn/user/info/{username}")
    public R<VpnLoginUser> getUserInfo(@PathVariable("username") String username,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 记录用户登录IP地址和登录时间
     *
     * @param vpnUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PutMapping("/vpn/user/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody VpnUserInfo vpnUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 获取用户授权线路列表
     *
     * @param userId 用户ID
     * @param source 请求来源
     * @return 授权线路列表
     */
    @GetMapping("/vpn/user/authorized-lines/{userId}")
    public R<List<Map<String, Object>>> getAuthorizedLines(@PathVariable("userId") Long userId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * VPN用户自助修改密码
     *
     * @param request 修改密码请求
     * @param source 请求来源
     * @return 结果
     */
    @PutMapping("/vpn/user/changePassword")
    public R<Boolean> changePassword(@RequestBody VpnChangePasswordRequest request, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

}
