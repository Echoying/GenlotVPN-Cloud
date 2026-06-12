package com.ruoyi.vpn.auth.form;

/**
 * 用户登录对象
 * 
 * @author ruoyi
 */
public class VpnLoginBody
{
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 线路ID（line_app.app_id，登录前已选择）
     */
    private String appId;

    /**
     * 线路名称（line_app.app_name）
     */
    private String appName;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
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
}
