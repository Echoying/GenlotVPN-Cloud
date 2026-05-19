package com.ruoyi.yianlian.domain;

import java.util.Date;

/**
 * VPN用户与易安联用户映射对象 vpn_user_yianlian_mapping
 *
 * @author ruoyi
 */
public class VpnUserYianlianMapping
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 本地用户ID */
    private Long userId;

    /** 线路appId */
    private String appId;

    /** 易安联用户ID */
    private String yianlianId;

    /** 创建时间 */
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getYianlianId()
    {
        return yianlianId;
    }

    public void setYianlianId(String yianlianId)
    {
        this.yianlianId = yianlianId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString()
    {
        return "VpnUserYianlianMapping{" +
                "id=" + id +
                ", userId=" + userId +
                ", appId='" + appId + '\'' +
                ", yianlianId='" + yianlianId + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
