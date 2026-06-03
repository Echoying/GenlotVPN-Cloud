package com.ruoyi.vpn.auth.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 钉钉 @ 配置
 */
public class DingTalkAt
{
    @JsonProperty("atMobiles")
    private List<String> atMobiles = new ArrayList<>();

    @JsonProperty("atUserIds")
    private List<String> atUserIds = new ArrayList<>();

    @JsonProperty("isAtAll")
    private boolean isAtAll;

    public List<String> getAtMobiles()
    {
        return atMobiles;
    }

    public void setAtMobiles(List<String> atMobiles)
    {
        this.atMobiles = atMobiles;
    }

    public List<String> getAtUserIds()
    {
        return atUserIds;
    }

    public void setAtUserIds(List<String> atUserIds)
    {
        this.atUserIds = atUserIds;
    }

    public boolean isAtAll()
    {
        return isAtAll;
    }

    public void setAtAll(boolean atAll)
    {
        isAtAll = atAll;
    }
}
