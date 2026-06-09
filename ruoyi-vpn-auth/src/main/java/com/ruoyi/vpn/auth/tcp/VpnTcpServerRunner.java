package com.ruoyi.vpn.auth.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.ruoyi.common.redis.service.RedisService;
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
    private RedisService redisService;

    @Autowired
    private TcpRpcDispatcher rpcDispatcher;

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        if (!properties.isEnabled())
        {
            log.info("VPN TCP 服务未启用");
            return;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new VpnTcpChannelInitializer(properties, redisService, rpcDispatcher));

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
}
