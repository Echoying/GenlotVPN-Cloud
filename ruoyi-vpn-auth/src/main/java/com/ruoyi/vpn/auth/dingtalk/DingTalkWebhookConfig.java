package com.ruoyi.vpn.auth.dingtalk;

/**
 * 钉钉自定义机器人 Webhook 配置
 */
public interface DingTalkWebhookConfig
{
    boolean isEnabled();

    String getWebhookUrl();

    String getAccessToken();

    String getSecret();
}
