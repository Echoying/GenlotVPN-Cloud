package com.ruoyi.yianlian.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * VPN用户与易安联用户映射对象 vpn_user_yianlian_mapping
 *
 * @author ruoyi
 */
public class VpnUserYianlianMapping extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 应用ID */
    @Excel(name = "应用ID")
    private String appId;

    /** VPN用户ID */
    @Excel(name = "VPN用户ID")
    private Long vpnUserId;

    /** 易安联用户ID */
    @Excel(name = "易安联用户ID")
    private String yianlianUserId;

    /** 易安联用户名称 */
    @Excel(name = "易安联用户名称")
    private String yianlianUserName;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setVpnUserId(Long vpnUserId)
    {
        this.vpnUserId = vpnUserId;
    }

    public Long getVpnUserId()
    {
        return vpnUserId;
    }

    public void setYianlianUserId(String yianlianUserId)
    {
        this.yianlianUserId = yianlianUserId;
    }

    public String getYianlianUserId()
    {
        return yianlianUserId;
    }

    public void setYianlianUserName(String yianlianUserName)
    {
        this.yianlianUserName = yianlianUserName;
    }

    public String getYianlianUserName()
    {
        return yianlianUserName;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("appId", getAppId())
            .append("vpnUserId", getVpnUserId())
            .append("yianlianUserId", getYianlianUserId())
            .append("yianlianUserName", getYianlianUserName())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
