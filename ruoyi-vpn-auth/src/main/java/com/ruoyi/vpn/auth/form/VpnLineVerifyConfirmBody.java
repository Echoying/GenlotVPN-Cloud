package com.ruoyi.vpn.auth.form;

/**
 * 选线验证码确认请求
 */
public class VpnLineVerifyConfirmBody
{
    /** 线路 appId */
    private String appId;

    /** 验证码 */
    private String code;

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }
}
