package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户授权对象 yal_user_auth
 *
 * @author ruoyi
 */
public class YalUserAuth extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 线路appId */
    @Excel(name = "线路appId")
    private String lineId;

    /** 应用组ID列表，逗号分隔 */
    @Excel(name = "应用组ID列表")
    private String appGroupIds;

    /** 应用ID列表，逗号分隔 */
    @Excel(name = "应用ID列表")
    private String appIds;

    /** 用户名称（用于列表展示） */
    private String userName;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
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

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
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
