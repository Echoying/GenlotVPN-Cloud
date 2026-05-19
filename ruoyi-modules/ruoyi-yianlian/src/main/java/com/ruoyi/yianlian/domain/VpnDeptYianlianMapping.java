package com.ruoyi.yianlian.domain;

import java.util.Date;

/**
 * VPN部门与易安联部门映射对象 vpn_dept_yianlian_mapping
 *
 * @author ruoyi
 */
public class VpnDeptYianlianMapping
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 本地部门ID */
    private Long deptId;

    /** 线路appId */
    private String appId;

    /** 易安联部门ID */
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

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
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
        return "VpnDeptYianlianMapping{" +
                "id=" + id +
                ", deptId=" + deptId +
                ", appId='" + appId + '\'' +
                ", yianlianId='" + yianlianId + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
