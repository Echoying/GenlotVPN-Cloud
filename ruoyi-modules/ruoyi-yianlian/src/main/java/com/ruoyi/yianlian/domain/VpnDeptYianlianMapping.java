package com.ruoyi.yianlian.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * VPN部门与易安联部门映射对象 vpn_dept_yianlian_mapping
 *
 * @author ruoyi
 */
public class VpnDeptYianlianMapping extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 应用ID */
    @Excel(name = "应用ID")
    private String appId;

    /** VPN部门ID */
    @Excel(name = "VPN部门ID")
    private Long vpnDeptId;

    /** 易安联部门ID */
    @Excel(name = "易安联部门ID")
    private String yianlianDeptId;

    /** 易安联部门名称 */
    @Excel(name = "易安联部门名称")
    private String yianlianDeptName;

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

    public void setVpnDeptId(Long vpnDeptId)
    {
        this.vpnDeptId = vpnDeptId;
    }

    public Long getVpnDeptId()
    {
        return vpnDeptId;
    }

    public void setYianlianDeptId(String yianlianDeptId)
    {
        this.yianlianDeptId = yianlianDeptId;
    }

    public String getYianlianDeptId()
    {
        return yianlianDeptId;
    }

    public void setYianlianDeptName(String yianlianDeptName)
    {
        this.yianlianDeptName = yianlianDeptName;
    }

    public String getYianlianDeptName()
    {
        return yianlianDeptName;
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
            .append("vpnDeptId", getVpnDeptId())
            .append("yianlianDeptId", getYianlianDeptId())
            .append("yianlianDeptName", getYianlianDeptName())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
