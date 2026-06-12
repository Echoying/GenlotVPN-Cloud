package com.ruoyi.vpn.auth.dingtalk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录成功钉钉通知机器人配置（独立于线路验证码机器人）
 */
@Component
@ConfigurationProperties(prefix = "dingtalk.login-notify")
public class DingTalkLoginNotifyProperties implements DingTalkWebhookConfig
{
    /** 是否启用独立登录通知机器人（未配置 token 时可回退 dingtalk.robot） */
    private boolean enabled = false;

    /** 仅 login-notify 未启用时，是否回退 dingtalk.robot；启用独立通知时应保持 false */
    private boolean fallbackRobot = false;

    private String webhookUrl = "https://oapi.dingtalk.com/robot/send";

    private String accessToken;

    private String secret;

    public boolean isFallbackRobot()
    {
        return fallbackRobot;
    }

    public void setFallbackRobot(boolean fallbackRobot)
    {
        this.fallbackRobot = fallbackRobot;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public String getWebhookUrl()
    {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl)
    {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    @Override
    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }
}
