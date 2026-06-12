package com.ruoyi.vpn.auth.service;

/**
 * 登录成功钉钉通知上下文
 */
public class LoginNotifyContext
{
    private String username;

    private String appName;

    private String loginPurpose;

    private String ipaddr;

    private String accessTime;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public String getLoginPurpose()
    {
        return loginPurpose;
    }

    public void setLoginPurpose(String loginPurpose)
    {
        this.loginPurpose = loginPurpose;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public String getAccessTime()
    {
        return accessTime;
    }

    public void setAccessTime(String accessTime)
    {
        this.accessTime = accessTime;
    }
}
