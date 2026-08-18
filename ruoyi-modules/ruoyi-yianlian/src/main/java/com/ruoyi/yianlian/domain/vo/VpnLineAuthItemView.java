package com.ruoyi.yianlian.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class VpnLineAuthItemView
{
    private String appGroupId;
    private String appGroupName;
    private String appId;
    private String appName;
    private List<String> sources = new ArrayList<String>();

    public String getAppGroupId()
    {
        return appGroupId;
    }

    public void setAppGroupId(String appGroupId)
    {
        this.appGroupId = appGroupId;
    }

    public String getAppGroupName()
    {
        return appGroupName;
    }

    public void setAppGroupName(String appGroupName)
    {
        this.appGroupName = appGroupName;
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

    public List<String> getSources()
    {
        return sources;
    }

    public void setSources(List<String> sources)
    {
        this.sources = sources;
    }
}
