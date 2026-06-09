package com.ruoyi.vpn.auth.tcp;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

/**
 * 单 IP TCP 并发连接计数（进程内）
 */
@Component
public class VpnTcpConnectionRegistry
{
    private final ConcurrentHashMap<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    public String resolveClientIp(ChannelHandlerContext ctx)
    {
        if (ctx == null || ctx.channel() == null || ctx.channel().remoteAddress() == null)
        {
            return "unknown";
        }
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress)
        {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            if (address.getAddress() != null)
            {
                return address.getAddress().getHostAddress();
            }
            return address.getHostString();
        }
        return ctx.channel().remoteAddress().toString();
    }

    public boolean tryAcquire(String ip, int maxConnectionsPerIp)
    {
        if (maxConnectionsPerIp <= 0 || StringUtils.isEmpty(ip))
        {
            return true;
        }
        AtomicInteger counter = connectionCounts.computeIfAbsent(ip, key -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > maxConnectionsPerIp)
        {
            counter.decrementAndGet();
            return false;
        }
        return true;
    }

    public void release(String ip)
    {
        if (StringUtils.isEmpty(ip))
        {
            return;
        }
        AtomicInteger counter = connectionCounts.get(ip);
        if (counter == null)
        {
            return;
        }
        int remaining = counter.decrementAndGet();
        if (remaining <= 0)
        {
            connectionCounts.remove(ip, counter);
        }
    }
}
