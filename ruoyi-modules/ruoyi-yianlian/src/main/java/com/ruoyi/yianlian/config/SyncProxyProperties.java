package com.ruoyi.yianlian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ruoyi.yianlian.constant.SyncProxyConstants;

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

    /** GenlotVPN-Proxy 管理 API 端口（login/logout/probe） */
    private int proxyAdminPort = 18080;

    /** 92 访问代理的协议，默认 http */
    private String defaultScheme = "http";

    /** 入站白名单（GenlotVPN 代理校验用） */
    private List<String> allowedSourceIps = new ArrayList<>();

    /** 线路探测调用代理管理 API 读超时（毫秒），略大于代理端 30303 detect 超时 */
    private int probeReadTimeoutMs = 35000;

    /** 实时 API 触发的代理 login 最大尝试次数（失败即入队补偿） */
    private int loginMaxAttemptsApi = 1;

    /** 定时补偿 Job 触发的代理 login 最大尝试次数 */
    private int loginMaxAttemptsJob = 3;

    /** 代理 login 两次尝试之间的间隔（毫秒） */
    private long loginRetryIntervalMs = 10000L;

    /** 单次代理 login/logout HTTP 读超时（毫秒），需覆盖 30303 建隧道全过程 */
    private int loginReadTimeoutMs = 150000;

    /** 代理会话分布式锁 / 活跃标记 TTL（秒） */
    private long sessionLockSeconds = 360L;

    /** 抢占代理会话锁的最长等待时间（毫秒） */
    private long sessionLockWaitMs = 30000L;

    /** 代理 login 成功后、发起 OpenAPI 业务请求前的等待时间（毫秒），用于隧道/路由就绪 */
    private long postLoginDelayMs = 60000L;

    /** 服务端代理同步 login 使用的线路 VPN 用户名（各线路须存在同名用户且已设密码） */
    private String loginUsername = SyncProxyConstants.LOGIN_USERNAME;

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

    public int getProxyAdminPort()
    {
        return proxyAdminPort;
    }

    public void setProxyAdminPort(int proxyAdminPort)
    {
        this.proxyAdminPort = proxyAdminPort;
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

    public int getProbeReadTimeoutMs()
    {
        return probeReadTimeoutMs;
    }

    public void setProbeReadTimeoutMs(int probeReadTimeoutMs)
    {
        this.probeReadTimeoutMs = probeReadTimeoutMs;
    }

    public int getLoginMaxAttemptsApi()
    {
        return loginMaxAttemptsApi;
    }

    public void setLoginMaxAttemptsApi(int loginMaxAttemptsApi)
    {
        this.loginMaxAttemptsApi = loginMaxAttemptsApi;
    }

    public int getLoginMaxAttemptsJob()
    {
        return loginMaxAttemptsJob;
    }

    public void setLoginMaxAttemptsJob(int loginMaxAttemptsJob)
    {
        this.loginMaxAttemptsJob = loginMaxAttemptsJob;
    }

    public long getLoginRetryIntervalMs()
    {
        return loginRetryIntervalMs;
    }

    public void setLoginRetryIntervalMs(long loginRetryIntervalMs)
    {
        this.loginRetryIntervalMs = loginRetryIntervalMs;
    }

    public int getLoginReadTimeoutMs()
    {
        return loginReadTimeoutMs;
    }

    public void setLoginReadTimeoutMs(int loginReadTimeoutMs)
    {
        this.loginReadTimeoutMs = loginReadTimeoutMs;
    }

    public long getSessionLockSeconds()
    {
        return sessionLockSeconds;
    }

    public void setSessionLockSeconds(long sessionLockSeconds)
    {
        this.sessionLockSeconds = sessionLockSeconds;
    }

    public long getSessionLockWaitMs()
    {
        return sessionLockWaitMs;
    }

    public void setSessionLockWaitMs(long sessionLockWaitMs)
    {
        this.sessionLockWaitMs = sessionLockWaitMs;
    }

    public long getPostLoginDelayMs()
    {
        return postLoginDelayMs;
    }

    public void setPostLoginDelayMs(long postLoginDelayMs)
    {
        this.postLoginDelayMs = postLoginDelayMs;
    }

    public String getLoginUsername()
    {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername)
    {
        this.loginUsername = loginUsername;
    }
}
