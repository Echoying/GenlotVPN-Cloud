package com.ruoyi.vpn.auth.context;

import com.ruoyi.common.core.utils.StringUtils;

/**
 * TCP 请求线程内传递客户端设备信息，供登录审计日志写入
 */
public final class ClientAuditContext
{
    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private ClientAuditContext()
    {
    }

    public static void bind(String reportedIp, String tcpClientIp, String clientOs, String clientMac)
    {
        HOLDER.set(new Holder(reportedIp, tcpClientIp, clientOs, clientMac));
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

    private static final class Holder
    {
        private final String reportedIp;
        private final String tcpClientIp;
        private final String clientOs;
        private final String clientMac;

        private Holder(String reportedIp, String tcpClientIp, String clientOs, String clientMac)
        {
            this.reportedIp = reportedIp;
            this.tcpClientIp = tcpClientIp;
            this.clientOs = clientOs;
            this.clientMac = clientMac;
        }
    }
}
