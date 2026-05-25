package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.VpnUser;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 易安联服务
 *
 * @author ruoyi
 */
@FeignClient(contextId = "remoteYiAnLianService", value = ServiceNameConstants.YIANLIAN_SERVICE, fallbackFactory = RemoteVpnUserService.class)
public interface RemoteVpnUserService {
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/vpn/user/info/{username}")
    public R<VpnLoginUser> getUserInfo(@PathVariable("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 记录用户登录IP地址和登录时间
     *
     * @param vpnUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PutMapping("/vpn/user/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody VpnUser vpnUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

}
