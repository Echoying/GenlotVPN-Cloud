package com.ruoyi.vpn.auth.controller;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.vpn.auth.form.VpnLoginBody;
import com.ruoyi.vpn.auth.form.VpnChangePasswordBody;
import com.ruoyi.vpn.auth.form.VpnUnLockBody;
import com.ruoyi.vpn.auth.service.VpnLoginService;
import com.ruoyi.vpn.auth.utils.AesUtils;
import com.ruoyi.yianlian.api.RemoteVpnUserService;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.vpn.auth.service.VpnLineVerifyService;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.JwtUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.auth.AuthUtil;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.model.LoginUser;

/**
 * token 控制
 * 
 * @author ruoyi
 */
@RestController
public class TokenController
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private VpnLoginService vpnLoginService;

    @Autowired
    private RemoteVpnUserService remoteVpnUserService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private AesUtils aesUtils;

    @Autowired
    private VpnLineVerifyService vpnLineVerifyService;

    @PostMapping("login")
    public R<?> login(@RequestBody VpnLoginBody form)
    {
        // 用户登录
        VpnLoginUser userInfo = vpnLoginService.login(form.getUsername(), form.getPassword(), form.getAppId());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping("logout")
    public R<?> logout(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            Long userId = Long.parseLong(JwtUtils.getUserId(token));
            String username = JwtUtils.getUserName(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            vpnLoginService.logout(username);
            vpnLineVerifyService.clearSendCooldown(userId);
        }
        return R.ok();
    }

    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request)
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
            return R.ok();
        }
        return R.ok();
    }

    /**
     * 解锁屏幕
     */
    @PostMapping("/unlockscreen")
    public R<?> unlockScreen(@RequestBody VpnUnLockBody unLockBody)
    {
        vpnLoginService.unlock(unLockBody.getPassword());
        return R.ok();
    }

    /**
     * 登录前获取可选线路列表（无需 token）
     */
    @GetMapping("lines")
    public R<?> listPublicLines()
    {
        return R.ok(vpnLoginService.listPublicLines());
    }

    /**
     * 获取当前用户的授权线路列表
     */
    @GetMapping("authorized-lines")
    public R<?> getAuthorizedLines(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        Long userId = Long.parseLong(JwtUtils.getUserId(token));
        return remoteVpnUserService.getAuthorizedLines(userId, SecurityConstants.INNER);
    }

    /**
     * 获取当前用户的控制器登录凭证（用户名 + AES加密密码）
     */
    @GetMapping("user-credentials")
    public R<?> getUserCredentials(HttpServletRequest request, @RequestParam String appId)
    {
        String token = SecurityUtils.getToken(request);
        String username = JwtUtils.getUserName(token);
        Long userId = Long.parseLong(JwtUtils.getUserId(token));

        vpnLineVerifyService.consumePassed(userId, appId);

        // 从Redis获取缓存的明文密码
        String plainPassword = redisService.getCacheObject("vpn_plain_pwd:" + userId);
        if (StringUtils.isEmpty(plainPassword))
        {
            return R.fail("凭证已过期，请重新登录");
        }

        // AES加密密码
        String encryptedPassword = aesUtils.encrypt(plainPassword);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", encryptedPassword);
        return R.ok(credentials);
    }

    /**
     * VPN用户修改密码（登录前）
     */
    @PutMapping("change-password")
    public R<?> changePassword(@RequestBody VpnChangePasswordBody form)
    {
        vpnLoginService.changePassword(form.getUsername(), form.getOldPassword(), form.getNewPassword(), form.getAppId());
        return R.ok();
    }
}
