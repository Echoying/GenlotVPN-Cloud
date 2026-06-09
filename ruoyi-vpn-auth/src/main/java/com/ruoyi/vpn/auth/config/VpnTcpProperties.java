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
    }
}
