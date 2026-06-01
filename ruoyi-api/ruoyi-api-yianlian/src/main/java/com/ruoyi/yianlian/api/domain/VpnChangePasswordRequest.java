package com.ruoyi.yianlian.api.domain;

/**
 * VPN用户修改密码请求
 */
public class VpnChangePasswordRequest
{
    /** 用户名 */
    private String username;

    /** 旧密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getOldPassword()
    {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword)
    {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword(String newPassword)
    {
        this.newPassword = newPassword;
    }
}
