package com.ruoyi.vpn.auth.tcp;

import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.vpn.auth.config.VpnTcpProperties;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * TCP Channel 初始化
 */
@Component
public class VpnTcpChannelInitializer extends ChannelInitializer<SocketChannel>
{
    @Autowired
    private VpnTcpProperties properties;

    @Autowired
    private VpnTcpBusinessHandler businessHandler;

    @Autowired
    private VpnTcpConnectionLimitHandler connectionLimitHandler;

    @Autowired
    private VpnTcpIdleDisconnectHandler idleDisconnectHandler;

    private SslContext sslContext;

    @PostConstruct
    public void initSslContext() throws Exception
    {
        if (properties.getTls().isEnabled())
        {
            VpnTcpProperties.Tls tls = properties.getTls();
            if (StringUtils.isEmpty(tls.getCertPath()) || StringUtils.isEmpty(tls.getKeyPath()))
            {
                throw new IllegalStateException("启用 TLS 时必须配置 vpn.tcp.tls.cert-path 与 key-path");
            }
            // 同时开放 1.2/1.3：部分 Windows 10 + Qt Schannel 对「仅 TLS1.3」会报 SEC_E_UNSUPPORTED_FUNCTION
            this.sslContext = SslContextBuilder.forServer(new File(tls.getCertPath()), new File(tls.getKeyPath()))
                    .protocols("TLSv1.2", "TLSv1.3")
                    .build();
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
        pipeline.addLast("connectionLimit", connectionLimitHandler);
        int idleMinutes = properties.getSecurity().getIdleTimeoutMinutes();
        if (idleMinutes > 0)
        {
            pipeline.addLast("idleState", new IdleStateHandler(idleMinutes, 0, 0, TimeUnit.MINUTES));
            pipeline.addLast("idleDisconnect", idleDisconnectHandler);
        }
        pipeline.addLast("frameDecoder", new VpnFrameDecoder(properties));
        pipeline.addLast("frameEncoder", new VpnFrameEncoder());
        pipeline.addLast("business", businessHandler);
    }
}
