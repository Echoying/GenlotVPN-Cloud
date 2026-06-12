package com.ruoyi.vpn.auth.tcp;

import java.util.Arrays;

/**
 * TCP 连接会话上下文
 */
public class TcpSessionContext
{
    private byte[] sessionKey;

    private String accessToken;

    private Long userId;

    private String username;

    private String loginPurpose;

    private String appId;

    private String appName;

    private boolean authenticated;

    private String clientIp;

    private String clientReportedIp;

    private String clientOs;

    private String clientMac;

    private long lastActivityMs;

    public byte[] getSessionKey()
    {
        return sessionKey;
    }

    public void setSessionKey(byte[] sessionKey)
    {
        this.sessionKey = sessionKey;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getLoginPurpose()
    {
        return loginPurpose;
    }

    public void setLoginPurpose(String loginPurpose)
    {
        this.loginPurpose = loginPurpose;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public boolean isAuthenticated()
    {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated)
    {
        this.authenticated = authenticated;
    }

    public String getClientIp()
    {
        return clientIp;
    }

    public void setClientIp(String clientIp)
    {
        this.clientIp = clientIp;
    }

    public String getClientReportedIp()
    {
        return clientReportedIp;
    }

    public void setClientReportedIp(String clientReportedIp)
    {
        this.clientReportedIp = clientReportedIp;
    }

    public String getClientOs()
    {
        return clientOs;
    }

    public void setClientOs(String clientOs)
    {
        this.clientOs = clientOs;
    }

    public String getClientMac()
    {
        return clientMac;
    }

    public void setClientMac(String clientMac)
    {
        this.clientMac = clientMac;
    }

    public long getLastActivityMs()
    {
        return lastActivityMs;
    }

    public void setLastActivityMs(long lastActivityMs)
    {
        this.lastActivityMs = lastActivityMs;
    }

    public void clearSecrets()
    {
        if (sessionKey != null)
        {
            Arrays.fill(sessionKey, (byte) 0);
            sessionKey = null;
        }
        accessToken = null;
    }
}
