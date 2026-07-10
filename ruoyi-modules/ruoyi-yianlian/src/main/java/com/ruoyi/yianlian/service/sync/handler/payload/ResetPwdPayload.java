package com.ruoyi.yianlian.service.sync.handler.payload;

import com.ruoyi.yianlian.domain.VpnUser;

import java.io.Serializable;

/**
 * 重置密码命令载荷（非可延迟操作，明文不入库）
 */
public class ResetPwdPayload implements Serializable
{
    private static final long serialVersionUID = 1L;

    private VpnUser user;

    private String plainPassword;

    public ResetPwdPayload()
    {
    }

    public ResetPwdPayload(VpnUser user, String plainPassword)
    {
        this.user = user;
        this.plainPassword = plainPassword;
    }

    public VpnUser getUser()
    {
        return user;
    }

    public void setUser(VpnUser user)
    {
        this.user = user;
    }

    public String getPlainPassword()
    {
        return plainPassword;
    }

    public void setPlainPassword(String plainPassword)
    {
        this.plainPassword = plainPassword;
    }
}
