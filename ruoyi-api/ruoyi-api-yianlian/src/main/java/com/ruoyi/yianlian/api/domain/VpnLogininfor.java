package com.ruoyi.yianlian.api.domain;

import java.io.Serializable;
import org.apache.ibatis.type.Alias;

/**
 * VPN 登录日志（Feign 传输对象）
 */
@Alias("ApiVpnLogininfor")
public class VpnLogininfor implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String userName;

    private String status;

    private String ipaddr;

    private String clientOs;

    private String clientMac;

    private String msg;

    private String loginPurpose;

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public String getLoginPurpose()
    {
        return loginPurpose;
    }

    public void setLoginPurpose(String loginPurpose)
    {
        this.loginPurpose = loginPurpose;
    }
}
