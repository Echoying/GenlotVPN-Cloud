package com.ruoyi.vpn.auth.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 钉钉机器人响应
 */
public class DingTalkRobotResponse
{
    @JsonProperty("errcode")
    private Integer errCode;

    @JsonProperty("errmsg")
    private String errMsg;

    public boolean isSuccess()
    {
        return errCode != null && errCode == 0;
    }

    public Integer getErrCode()
    {
        return errCode;
    }

    public void setErrCode(Integer errCode)
    {
        this.errCode = errCode;
    }

    public String getErrMsg()
    {
        return errMsg;
    }

    public void setErrMsg(String errMsg)
    {
        this.errMsg = errMsg;
    }
}
