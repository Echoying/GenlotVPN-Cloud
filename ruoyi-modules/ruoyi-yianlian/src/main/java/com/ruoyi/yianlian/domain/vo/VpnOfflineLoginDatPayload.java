package com.ruoyi.yianlian.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 离线登录 .dat 文件 JSON 内容
 */
public class VpnOfflineLoginDatPayload
{
    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("app_name")
    private String appName;

    private String host;

    @JsonProperty("srv_port")
    private Integer srvPort;

    @JsonProperty("spa_port")
    private Integer spaPort;

    @JsonProperty("spa_key")
    private String spaKey;

    @JsonProperty("user_name")
    private String userName;

    private String password;

    @JsonProperty("expire_at")
    private String expireAt;

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

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public Integer getSrvPort()
    {
        return srvPort;
    }

    public void setSrvPort(Integer srvPort)
    {
        this.srvPort = srvPort;
    }

    public Integer getSpaPort()
    {
        return spaPort;
    }

    public void setSpaPort(Integer spaPort)
    {
        this.spaPort = spaPort;
    }

    public String getSpaKey()
    {
        return spaKey;
    }

    public void setSpaKey(String spaKey)
    {
        this.spaKey = spaKey;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getExpireAt()
    {
        return expireAt;
    }

    public void setExpireAt(String expireAt)
    {
        this.expireAt = expireAt;
    }
}
