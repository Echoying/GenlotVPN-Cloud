package com.ruoyi.vpn.auth.controller;

import javax.servlet.http.HttpServletRequest;

import com.ruoyi.vpn.auth.form.VpnLoginBody;
import com.ruoyi.vpn.auth.form.VpnUnLockBody;
import com.ruoyi.vpn.auth.service.VpnLoginService;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
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

    @PostMapping("login")
    public R<?> login(@RequestBody VpnLoginBody form)
    {
        // 用户登录
        VpnLoginUser userInfo = vpnLoginService.login(form.getUsername(), form.getPassword());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping("logout")
    public R<?> logout(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            String username = JwtUtils.getUserName(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            vpnLoginService.logout(username);
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
}
