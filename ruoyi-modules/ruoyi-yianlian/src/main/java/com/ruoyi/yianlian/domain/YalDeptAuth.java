package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
    private Long deptId;

    /** 线路ID */
    private String lineId;

    /** 应用组ID列表，逗号分隔 */
    private String appGroupIds;

    /** 应用ID列表，逗号分隔 */
    private String appIds;

    /** 部门名称（关联查询） */
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

    public void setLineId(String lineId)
    {
        this.lineId = lineId;
    }

    public String getLineId()
    {
        return lineId;
    }

    public void setAppGroupIds(String appGroupIds)
    {
        this.appGroupIds = appGroupIds;
    }

    public String getAppGroupIds()
    {
        return appGroupIds;
    }

    public void setAppIds(String appIds)
    {
        this.appIds = appIds;
    }

    public String getAppIds()
    {
        return appIds;
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
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("deptId", getDeptId())
            .append("lineId", getLineId())
            .append("appGroupIds", getAppGroupIds())
            .append("appIds", getAppIds())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
