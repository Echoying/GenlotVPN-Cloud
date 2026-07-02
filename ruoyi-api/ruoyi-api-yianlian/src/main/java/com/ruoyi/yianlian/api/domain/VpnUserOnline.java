package com.ruoyi.yianlian.api.domain;

import java.io.Serializable;

/**
 * VPN 当前在线会话（Redis 缓存对象）
 */
public class VpnUserOnline implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 会话编号（与 login_tokens 的 user_key 一致） */
    private String tokenId;

    private Long userId;

    private String userName;

    private String nickName;

    private String appId;

    private String appName;

    private String ipaddr;

    private String clientOs;

    private String clientMac;

    private String loginPurpose;

    /** 连接成功时间（毫秒时间戳） */
    private Long loginTime;

    public String getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
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

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
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

    public String getLoginPurpose()
    {
        return loginPurpose;
    }

    public void setLoginPurpose(String loginPurpose)
    {
        this.loginPurpose = loginPurpose;
    }

    public Long getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(Long loginTime)
    {
        this.loginTime = loginTime;
    }
}
