package com.ruoyi.vpn.auth.form;

/**
 * 选线验证码发送请求
 */
public class VpnLineVerifySendBody
{
    /** 线路 appId */
    private String appId;

    /** 线路名称（前端传入） */
    private String lineName;

    /** 登录用途 */
    private String loginPurpose;

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getLineName()
    {
        return lineName;
    }

    public void setLineName(String lineName)
    {
        this.lineName = lineName;
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
