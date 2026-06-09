package com.ruoyi.vpn.auth.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.vpn.auth.config.VpnTcpProperties;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

/**
 * 单 IP 并发连接数限制
 */
@Component
@ChannelHandler.Sharable
public class VpnTcpConnectionLimitHandler extends ChannelInboundHandlerAdapter
{
    private static final Logger log = LoggerFactory.getLogger(VpnTcpConnectionLimitHandler.class);

    public static final AttributeKey<String> CLIENT_IP_KEY = AttributeKey.valueOf("vpnTcpClientIp");

    @Autowired
    private VpnTcpProperties properties;

    @Autowired
    private VpnTcpConnectionRegistry connectionRegistry;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        String clientIp = connectionRegistry.resolveClientIp(ctx);
        ctx.channel().attr(CLIENT_IP_KEY).set(clientIp);

        int maxConnections = properties.getLimits().getMaxConnectionsPerIp();
        if (!connectionRegistry.tryAcquire(clientIp, maxConnections))
        {
            log.warn("TCP 连接数超限，拒绝连接 ip={} max={}", clientIp, maxConnections);
            ctx.close();
            return;
        }
        log.debug("TCP 连接建立 ip={}", clientIp);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        String clientIp = ctx.channel().attr(CLIENT_IP_KEY).get();
        connectionRegistry.release(clientIp);
        log.debug("TCP 连接关闭 ip={}", clientIp);
        super.channelInactive(ctx);
    }
}
