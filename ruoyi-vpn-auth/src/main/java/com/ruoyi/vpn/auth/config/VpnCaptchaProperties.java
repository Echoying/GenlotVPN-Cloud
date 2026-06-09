package com.ruoyi.vpn.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TCP 通道验证码配置
 */
@Component
@ConfigurationProperties(prefix = "vpn.captcha")
public class VpnCaptchaProperties
{
    private boolean enabled = true;

    /** math=彩色算式验证码（与 Web 端一致），char=彩色字符验证码 */
    private String type = "math";

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
