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

    private boolean authenticated;

    private String clientIp;

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
