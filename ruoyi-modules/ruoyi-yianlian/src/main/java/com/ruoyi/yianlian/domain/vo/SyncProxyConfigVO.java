package com.ruoyi.yianlian.domain.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 同步代理配置（供 GenlotVPN 客户端与内部 Feign 使用）
 */
public class SyncProxyConfigVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    private boolean enabled;

    /** 易安联真实 OpenAPI 基址（line_app.url） */
    private String upstreamUrl;

    private String listenHost;

    private int listenPort;

    private String pathPrefix;

    private List<String> allowedSourceIps = new ArrayList<>();

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getUpstreamUrl()
    {
        return upstreamUrl;
    }

    public void setUpstreamUrl(String upstreamUrl)
    {
        this.upstreamUrl = upstreamUrl;
    }

    public String getListenHost()
    {
        return listenHost;
    }

    public void setListenHost(String listenHost)
    {
        this.listenHost = listenHost;
    }

    public int getListenPort()
    {
        return listenPort;
    }

    public void setListenPort(int listenPort)
    {
        this.listenPort = listenPort;
    }

    public String getPathPrefix()
    {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix)
    {
        this.pathPrefix = pathPrefix;
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
