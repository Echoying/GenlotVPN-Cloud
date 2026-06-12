package com.ruoyi.vpn.auth.context;

import com.ruoyi.common.core.utils.StringUtils;

/**
 * TCP/HTTP 请求线程内传递客户端设备与线路信息，供登录审计日志写入
 */
public final class ClientAuditContext
{
    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private ClientAuditContext()
    {
    }

    public static void bind(String reportedIp, String tcpClientIp, String clientOs, String clientMac)
    {
        Holder holder = HOLDER.get();
        if (holder == null)
        {
            HOLDER.set(new Holder(reportedIp, tcpClientIp, clientOs, clientMac, null, null));
            return;
        }
        holder.reportedIp = reportedIp;
        holder.tcpClientIp = tcpClientIp;
        holder.clientOs = clientOs;
        holder.clientMac = clientMac;
    }

    public static void bindApp(String appId, String appName)
    {
        Holder holder = HOLDER.get();
        if (holder == null)
        {
            HOLDER.set(new Holder(null, null, null, null, appId, appName));
            return;
        }
        holder.appId = appId;
        holder.appName = appName;
    }

    public static void clear()
    {
        HOLDER.remove();
    }

    public static String resolveIpaddr()
    {
        Holder holder = HOLDER.get();
        if (holder == null)
        {
            return null;
        }
        if (StringUtils.isNotEmpty(holder.reportedIp))
        {
            return holder.reportedIp;
        }
        return holder.tcpClientIp;
    }

    public static String getClientOs()
    {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.clientOs;
    }

    public static String getClientMac()
    {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.clientMac;
    }

    public static String getAppId()
    {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.appId;
    }

    public static String getAppName()
    {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.appName;
    }

    private static final class Holder
    {
        private String reportedIp;
        private String tcpClientIp;
        private String clientOs;
        private String clientMac;
        private String appId;
        private String appName;

        private Holder(String reportedIp, String tcpClientIp, String clientOs, String clientMac,
                String appId, String appName)
        {
            this.reportedIp = reportedIp;
            this.tcpClientIp = tcpClientIp;
            this.clientOs = clientOs;
            this.clientMac = clientMac;
            this.appId = appId;
            this.appName = appName;
        }
    }
}
