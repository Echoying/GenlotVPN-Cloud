package com.ruoyi.vpn.auth.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 读空闲超时断开（无 RPC 流量）
 */
@Component
@ChannelHandler.Sharable
public class VpnTcpIdleDisconnectHandler extends ChannelInboundHandlerAdapter
{
    private static final Logger log = LoggerFactory.getLogger(VpnTcpIdleDisconnectHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof IdleStateEvent)
        {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE)
            {
                String clientIp = ctx.channel().attr(VpnTcpConnectionLimitHandler.CLIENT_IP_KEY).get();
                log.warn("TCP 连接读空闲超时，断开 ip={}", clientIp);
                ctx.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
