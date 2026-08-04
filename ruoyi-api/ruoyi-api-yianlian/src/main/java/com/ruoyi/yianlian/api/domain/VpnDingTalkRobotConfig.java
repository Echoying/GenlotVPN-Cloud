package com.ruoyi.yianlian.api.domain;

import java.io.Serializable;

/**
 * VPN 选线验证码钉钉机器人配置（由角色解析，供 vpn-auth 发码）
 */
public class VpnDingTalkRobotConfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 是否启用 */
    private boolean enabled;

    /** Webhook 地址 */
    private String webhookUrl;

    /** access_token */
    private String accessToken;

    /** 加签 secret（可空） */
    private String secret;

    /** 命中的角色 ID（日志用） */
    private Long roleId;

    /** 命中的角色名称（日志用） */
    private String roleName;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getWebhookUrl()
    {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl)
    {
        this.webhookUrl = webhookUrl;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }
}
