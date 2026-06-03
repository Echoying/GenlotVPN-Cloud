package com.ruoyi.vpn.auth.controller;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.JwtUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.vpn.auth.form.VpnLineVerifyConfirmBody;
import com.ruoyi.vpn.auth.form.VpnLineVerifySendBody;
import com.ruoyi.vpn.auth.service.VpnLineVerifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选线钉钉验证码
 */
@RestController
public class VpnLineVerifyController
{
    @Autowired
    private VpnLineVerifyService vpnLineVerifyService;

    /**
     * 发送选线验证码到钉钉群
     */
    @PostMapping("line-verify/send")
    public R<?> send(HttpServletRequest request, @RequestBody VpnLineVerifySendBody body)
    {
        String token = SecurityUtils.getToken(request);
        Long userId = Long.parseLong(JwtUtils.getUserId(token));
        String username = JwtUtils.getUserName(token);
        Map<String, String> result = vpnLineVerifyService.sendCode(
            userId, username, body.getAppId(), body.getLineName());
        return R.ok(result);
    }

    /**
     * 校验选线验证码
     */
    @PostMapping("line-verify/confirm")
    public R<?> confirm(HttpServletRequest request, @RequestBody VpnLineVerifyConfirmBody body)
    {
        String token = SecurityUtils.getToken(request);
        Long userId = Long.parseLong(JwtUtils.getUserId(token));
        vpnLineVerifyService.confirmCode(userId, body.getAppId(), body.getCode());
        return R.ok();
    }
}
