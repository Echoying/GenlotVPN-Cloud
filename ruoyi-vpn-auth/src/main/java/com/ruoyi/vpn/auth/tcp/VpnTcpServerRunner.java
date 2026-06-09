package com.ruoyi.vpn.auth.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.vpn.auth.config.VpnTcpProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * VPN TCP 服务启动器
 */
@Component
public class VpnTcpServerRunner implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(VpnTcpServerRunner.class);

    @Autowired
    private VpnTcpProperties properties;

    @Autowired
    private VpnTcpChannelInitializer channelInitializer;

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        if (!properties.isEnabled())
        {
            log.info("VPN TCP 服务未启用");
            return;
        }

        logTlsStartupInfo();
        logLimitsStartupInfo();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(channelInitializer);

            ChannelFuture future = bootstrap.bind(properties.getPort());
            future.addListener(f -> {
                if (f.isSuccess())
                {
                    log.info("VPN TCP 服务已启动，端口 {}，TLS={}", properties.getPort(), properties.getTls().isEnabled());
                }
                else
                {
                    log.error("VPN TCP 服务启动失败", f.cause());
                }
            });
        }
        catch (Exception e)
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    private void logTlsStartupInfo()
    {
        VpnTcpProperties.Tls tls = properties.getTls();
        if (tls.isEnabled())
        {
            log.info("VPN TCP TLS 已启用，cert-path={}，key-path={}", tls.getCertPath(), tls.getKeyPath());
            if (StringUtils.isNotEmpty(tls.getCertPath()))
            {
                String pin = VpnTcpCertUtils.spkiSha256Hex(tls.getCertPath());
                if (StringUtils.isNotEmpty(pin))
                {
                    log.info("VPN TCP 证书 SPKI Pin（供客户端 certPinSha256）: {}", pin);
                }
            }
        }
        else
        {
            log.warn("VPN TCP TLS 未启用（vpn.tcp.tls.enabled=false），仅适用于开发联调；生产环境必须启用 TLS");
        }
    }

    private void logLimitsStartupInfo()
    {
        VpnTcpProperties.Limits limits = properties.getLimits();
        int idleMinutes = properties.getSecurity().getIdleTimeoutMinutes();
        log.info("VPN TCP 限流: maxConnectionsPerIp={}, loginMaxAttemptsPerIp={}, loginWindowSec={}",
                limits.getMaxConnectionsPerIp(), limits.getLoginMaxAttemptsPerIp(), limits.getLoginRateWindowSeconds());
        if (idleMinutes > 0)
        {
            log.info("VPN TCP 读空闲断开: {} 分钟无 RPC 流量将断开连接", idleMinutes);
        }
        else
        {
            log.warn("VPN TCP 读空闲断开未启用（idle-timeout-minutes=0）");
        }
    }
}
