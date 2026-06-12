package com.ruoyi.vpn.auth.dingtalk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 钉钉自定义机器人配置
 */
@Component
@ConfigurationProperties(prefix = "dingtalk.robot")
public class DingTalkRobotProperties implements DingTalkWebhookConfig
{
    /** 是否启用 */
    private boolean enabled = false;

    /** Webhook 地址，默认官方机器人发送地址 */
    private String webhookUrl = "https://oapi.dingtalk.com/robot/send";

    /** 机器人 access_token */
    private String accessToken;

    /** 加签密钥（安全设置选择加签时必填） */
    private String secret;

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
