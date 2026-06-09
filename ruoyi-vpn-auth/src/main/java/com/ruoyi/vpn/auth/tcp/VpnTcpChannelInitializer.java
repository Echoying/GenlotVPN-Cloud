package com.ruoyi.vpn.auth.tcp;

import java.io.File;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.vpn.auth.config.VpnTcpProperties;
import com.ruoyi.common.redis.service.RedisService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * TCP Channel 初始化
 */
public class VpnTcpChannelInitializer extends ChannelInitializer<SocketChannel>
{
    private final SslContext sslContext;

    private final VpnTcpProperties properties;

    private final VpnTcpBusinessHandler businessHandler;

    public VpnTcpChannelInitializer(VpnTcpProperties properties, RedisService redisService,
            TcpRpcDispatcher rpcDispatcher) throws Exception
    {
        this.properties = properties;
        this.businessHandler = new VpnTcpBusinessHandler(properties, redisService, rpcDispatcher);
        if (properties.getTls().isEnabled())
        {
            VpnTcpProperties.Tls tls = properties.getTls();
            if (StringUtils.isEmpty(tls.getCertPath()) || StringUtils.isEmpty(tls.getKeyPath()))
            {
                throw new IllegalStateException("启用 TLS 时必须配置 vpn.tcp.tls.cert-path 与 key-path");
            }
            this.sslContext = SslContextBuilder.forServer(new File(tls.getCertPath()), new File(tls.getKeyPath()))
                    .protocols("TLSv1.3")
                    .build();
        }
        else
        {
            this.sslContext = null;
        }
    }

    @Override
    protected void initChannel(SocketChannel ch)
    {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslContext != null)
        {
            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
        }
        pipeline.addLast("frameDecoder", new VpnFrameDecoder(properties));
        pipeline.addLast("frameEncoder", new VpnFrameEncoder());
        pipeline.addLast("business", businessHandler);
    }
}
