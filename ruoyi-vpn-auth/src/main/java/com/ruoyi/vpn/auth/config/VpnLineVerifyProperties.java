package com.ruoyi.vpn.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 选线钉钉验证码配置
 */
@Component
@ConfigurationProperties(prefix = "vpn.line-verify")
public class VpnLineVerifyProperties
{
    /** 验证码有效期（分钟） */
    private int codeTtlMinutes = 5;

    /** 发送冷却时间（秒） */
    private int sendCooldownSeconds = 60;

    /** 最大校验失败次数 */
    private int maxVerifyAttempts = 5;

    /** 线路名称最大长度 */
    private int lineNameMaxLength = 64;

    public int getCodeTtlMinutes()
    {
        return codeTtlMinutes;
    }

    public void setCodeTtlMinutes(int codeTtlMinutes)
    {
        this.codeTtlMinutes = codeTtlMinutes;
    }

    public int getSendCooldownSeconds()
    {
        return sendCooldownSeconds;
    }

    public void setSendCooldownSeconds(int sendCooldownSeconds)
    {
        this.sendCooldownSeconds = sendCooldownSeconds;
    }

    public int getMaxVerifyAttempts()
    {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts)
    {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }

    public int getLineNameMaxLength()
    {
        return lineNameMaxLength;
    }

    public void setLineNameMaxLength(int lineNameMaxLength)
    {
        this.lineNameMaxLength = lineNameMaxLength;
    }
}
