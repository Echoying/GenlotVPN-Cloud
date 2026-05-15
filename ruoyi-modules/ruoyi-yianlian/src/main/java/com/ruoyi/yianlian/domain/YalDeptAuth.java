package com.ruoyi.yianlian.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 部门授权对象 yal_dept_auth
 *
 * @author ruoyi
 */
public class YalDeptAuth extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 部门ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 应用组ID */
    @Excel(name = "应用组ID")
    private Long groupId;

    /** 应用ID */
    @Excel(name = "应用ID")
    private Long serviceId;

    /** 授权类型（1-应用组 2-应用） */
    @Excel(name = "授权类型", readConverterExp = "1=应用组,2=应用")
    private String authType;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 应用组名称 */
    private String groupName;

    /** 应用名称 */
    private String serviceName;

    /** 部门名称 */
    private String deptName;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public Long getGroupId()
    {
        return groupId;
    }

    public void setServiceId(Long serviceId)
    {
        this.serviceId = serviceId;
    }

    public Long getServiceId()
    {
        return serviceId;
    }

    public void setAuthType(String authType)
    {
        this.authType = authType;
    }

    public String getAuthType()
    {
        return authType;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("deptId", getDeptId())
            .append("groupId", getGroupId())
            .append("serviceId", getServiceId())
            .append("authType", getAuthType())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
