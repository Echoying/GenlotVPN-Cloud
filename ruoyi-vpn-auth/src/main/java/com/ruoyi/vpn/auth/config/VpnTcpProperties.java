package com.ruoyi.vpn.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VPN TCP 服务配置
 */
@Component
@ConfigurationProperties(prefix = "vpn.tcp")
public class VpnTcpProperties
{
    /** 是否启用 TCP 服务 */
    private boolean enabled = true;

    /** 监听端口 */
    private int port = 9443;

    private final Tls tls = new Tls();

    private final Security security = new Security();

    private final Limits limits = new Limits();

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public Tls getTls()
    {
        return tls;
    }

    public Security getSecurity()
    {
        return security;
    }

    public Limits getLimits()
    {
        return limits;
    }

    public static class Tls
    {
        /** 是否启用 TLS（生产环境必须为 true） */
        private boolean enabled = false;

        private String certPath = "";

        private String keyPath = "";

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getCertPath()
        {
            return certPath;
        }

        public void setCertPath(String certPath)
        {
            this.certPath = certPath;
        }

        public String getKeyPath()
        {
            return keyPath;
        }

        public void setKeyPath(String keyPath)
        {
            this.keyPath = keyPath;
        }
    }

    public static class Security
    {
        /** 时间戳允许偏差（秒） */
        private int timestampSkewSec = 60;

        /** nonce Redis TTL（秒） */
        private int nonceTtlSec = 300;

        /** 单帧最大字节 */
        private int maxFrameBytes = 1048576;

        public int getTimestampSkewSec()
        {
            return timestampSkewSec;
        }

        public void setTimestampSkewSec(int timestampSkewSec)
        {
            this.timestampSkewSec = timestampSkewSec;
        }

        public int getNonceTtlSec()
        {
            return nonceTtlSec;
        }

        public void setNonceTtlSec(int nonceTtlSec)
        {
            this.nonceTtlSec = nonceTtlSec;
        }

        public int getMaxFrameBytes()
        {
            return maxFrameBytes;
        }

        public void setMaxFrameBytes(int maxFrameBytes)
        {
            this.maxFrameBytes = maxFrameBytes;
        }

        /** 读空闲断开（分钟），0 表示不启用 */
        private int idleTimeoutMinutes = 30;

        public int getIdleTimeoutMinutes()
        {
            return idleTimeoutMinutes;
        }

        public void setIdleTimeoutMinutes(int idleTimeoutMinutes)
        {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
        }
    }

    public static class Limits
    {
        /** 单 IP 最大并发 TCP 连接数，0 表示不限制 */
        private int maxConnectionsPerIp = 5;

        /** 单 IP 登录尝试次数上限（窗口内） */
        private int loginMaxAttemptsPerIp = 10;

        /** 登录频率统计窗口（秒） */
        private int loginRateWindowSeconds = 60;

        public int getMaxConnectionsPerIp()
        {
            return maxConnectionsPerIp;
        }

        public void setMaxConnectionsPerIp(int maxConnectionsPerIp)
        {
            this.maxConnectionsPerIp = maxConnectionsPerIp;
        }

        public int getLoginMaxAttemptsPerIp()
        {
            return loginMaxAttemptsPerIp;
        }

        public void setLoginMaxAttemptsPerIp(int loginMaxAttemptsPerIp)
        {
            this.loginMaxAttemptsPerIp = loginMaxAttemptsPerIp;
        }

        public int getLoginRateWindowSeconds()
        {
            return loginRateWindowSeconds;
        }

        public void setLoginRateWindowSeconds(int loginRateWindowSeconds)
        {
            this.loginRateWindowSeconds = loginRateWindowSeconds;
        }
    }
}
