package com.ruoyi.yianlian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 易安联 OpenAPI 同步代理配置（Nacos: yianlian.sync-proxy）
 */
@Configuration
@ConfigurationProperties(prefix = "yianlian.sync-proxy")
public class SyncProxyProperties
{
    /** 是否启用代理模式（false 时 OpenApiClient 仍直连 line_app.url） */
    private boolean enabled = false;

    /** 默认 VPN 管理机内网 IP */
    private String defaultHost = "127.0.0.1";

    /** 默认代理监听端口 */
    private int defaultPort = 18001;

    /** 92 访问代理的协议，默认 http */
    private String defaultScheme = "http";

    /** 入站白名单（GenlotVPN 代理校验用） */
    private List<String> allowedSourceIps = new ArrayList<>();

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getDefaultHost()
    {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost)
    {
        this.defaultHost = defaultHost;
    }

    public int getDefaultPort()
    {
        return defaultPort;
    }

    public void setDefaultPort(int defaultPort)
    {
        this.defaultPort = defaultPort;
    }

    public String getDefaultScheme()
    {
        return defaultScheme;
    }

    public void setDefaultScheme(String defaultScheme)
    {
        this.defaultScheme = defaultScheme;
    }

    public List<String> getAllowedSourceIps()
    {
        return allowedSourceIps;
    }

    public void setAllowedSourceIps(List<String> allowedSourceIps)
    {
        this.allowedSourceIps = allowedSourceIps != null ? allowedSourceIps : new ArrayList<>();
    }
}
