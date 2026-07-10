package com.ruoyi.yianlian.service.sync;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.config.SyncProxyProperties;
import com.ruoyi.yianlian.constant.SyncProxyConstants;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.domain.LineApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 解析 OpenAPI 调用基址：代理模式走 Nacos 默认 + 线路覆盖，否则直连 line_app.url
 */
@Component
public class SyncProxyEndpointResolver
{
    @Autowired
    private SyncProxyProperties syncProxyProperties;

    /**
     * 供 OpenApiClient 使用的 HTTP 基址（与 line_app.url 相同拼接方式）
     */
    public String resolveApiBaseUrl(LineApp lineApp)
    {
        if (lineApp == null)
        {
            return null;
        }
        if (!syncProxyProperties.isEnabled() || !SyncProxyConstants.isLineProxyEnabled(lineApp))
        {
            return normalizeBase(lineApp.getUrl());
        }
        String host = StringUtils.isNotEmpty(lineApp.getProxyHost())
            ? lineApp.getProxyHost().trim()
            : syncProxyProperties.getDefaultHost();
        int port = lineApp.getProxyPort() != null
            ? lineApp.getProxyPort()
            : syncProxyProperties.getDefaultPort();
        String scheme = StringUtils.isNotEmpty(syncProxyProperties.getDefaultScheme())
            ? syncProxyProperties.getDefaultScheme().trim()
            : "http";
        return scheme + "://" + host + ":" + port;
    }

    /**
     * 代理管理 API 基址（login/logout/probe），host 优先取线路 proxy_host，端口为管理端口
     */
    public String resolveAdminBaseUrl(LineApp lineApp)
    {
        String host = lineApp != null && StringUtils.isNotEmpty(lineApp.getProxyHost())
            ? lineApp.getProxyHost().trim()
            : syncProxyProperties.getDefaultHost();
        String scheme = StringUtils.isNotEmpty(syncProxyProperties.getDefaultScheme())
            ? syncProxyProperties.getDefaultScheme().trim()
            : "http";
        return scheme + "://" + host + ":" + syncProxyProperties.getProxyAdminPort();
    }

    /**
     * 代理会话的唯一标识（host:adminPort），用于 Redis 分布式锁与活跃标记
     */
    public String resolveAdminKey(LineApp lineApp)
    {
        String host = lineApp != null && StringUtils.isNotEmpty(lineApp.getProxyHost())
            ? lineApp.getProxyHost().trim()
            : syncProxyProperties.getDefaultHost();
        return host + ":" + syncProxyProperties.getProxyAdminPort();
    }

    public String resolveListenHost(LineApp lineApp)
    {
        if (lineApp != null && StringUtils.isNotEmpty(lineApp.getProxyHost()))
        {
            return lineApp.getProxyHost().trim();
        }
        return syncProxyProperties.getDefaultHost();
    }

    public int resolveListenPort(LineApp lineApp)
    {
        if (lineApp != null && lineApp.getProxyPort() != null)
        {
            return lineApp.getProxyPort();
        }
        return syncProxyProperties.getDefaultPort();
    }

    public String resolvePathPrefix()
    {
        return YiAnLianConstants.tokenPath.replace("/token", "");
    }

    private String normalizeBase(String url)
    {
        if (StringUtils.isEmpty(url))
        {
            return url;
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
