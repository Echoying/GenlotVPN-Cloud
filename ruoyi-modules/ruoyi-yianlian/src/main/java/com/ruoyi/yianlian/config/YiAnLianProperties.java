package com.ruoyi.yianlian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 易安联接口配置
 */
@Configuration
@ConfigurationProperties(prefix = "yianlian")
public class YiAnLianProperties
{
    /**
     * 易安联接口基础地址，例如：https://open.example.com
     */
    private String baseUrl;

    /**
     * 默认AppId
     */
    private String appId;

    /**
     * 默认AppSecret
     */
    private String appSecret;

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppSecret()
    {
        return appSecret;
    }

    public void setAppSecret(String appSecret)
    {
        this.appSecret = appSecret;
    }

}
