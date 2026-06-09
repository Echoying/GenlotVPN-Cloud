package com.ruoyi.vpn.auth.tcp;

import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.ByteString;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.vpn.auth.config.VpnTcpProperties;
import com.ruoyi.vpn.protocol.Envelope;
import com.ruoyi.vpn.protocol.MessageType;
import com.ruoyi.vpn.protocol.RpcResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

/**
 * TCP 业务处理器：安全校验 + RPC 分发
 */
@ChannelHandler.Sharable
public class VpnTcpBusinessHandler extends SimpleChannelInboundHandler<byte[]>
{
    private static final Logger log = LoggerFactory.getLogger(VpnTcpBusinessHandler.class);

    public static final AttributeKey<TcpSessionContext> SESSION_KEY =
            AttributeKey.valueOf("vpnTcpSession");

    private static final String NONCE_REDIS_PREFIX = "vpn_tcp_nonce:";

    private final VpnTcpProperties properties;

    private final RedisService redisService;

    private final TcpRpcDispatcher rpcDispatcher;

    public VpnTcpBusinessHandler(VpnTcpProperties properties, RedisService redisService,
            TcpRpcDispatcher rpcDispatcher)
    {
        this.properties = properties;
        this.redisService = redisService;
        this.rpcDispatcher = rpcDispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] frame) throws Exception
    {
        Envelope request = Envelope.parseFrom(frame);
        TcpSessionContext session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null)
        {
            session = new TcpSessionContext();
            ctx.channel().attr(SESSION_KEY).set(session);
        }

        RpcResponse response;
        try
        {
            validateSecurity(request, session);
            markNonceUsed(request);
            TcpRpcDispatcher.RpcResult result = rpcDispatcher.dispatch(request, session);
            response = result.getResponse();
        }
        catch (SecurityException e)
        {
            log.warn("TCP 安全校验失败: {}", e.getMessage());
            response = RpcResponse.newBuilder().setCode(500).setMsg(e.getMessage()).build();
        }
        catch (Exception e)
        {
            log.warn("TCP 业务异常: {}", e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage() : "服务异常";
            response = RpcResponse.newBuilder().setCode(500).setMsg(msg).build();
        }

        Envelope reply = buildReply(request, response, session);
        ctx.writeAndFlush(reply.toByteArray());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        TcpSessionContext session = ctx.channel().attr(SESSION_KEY).get();
        if (session != null)
        {
            session.clearSecrets();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        log.error("TCP 连接异常", cause);
        ctx.close();
    }

    private void validateSecurity(Envelope envelope, TcpSessionContext session)
    {
        long now = System.currentTimeMillis();
        long skewMs = properties.getSecurity().getTimestampSkewSec() * 1000L;
        if (Math.abs(now - envelope.getTimestampMs()) > skewMs)
        {
            throw new SecurityException("请求时间戳无效");
        }
        if (envelope.getNonce() == null || envelope.getNonce().isEmpty())
        {
            throw new SecurityException("nonce 不能为空");
        }
        String nonceKey = NONCE_REDIS_PREFIX + Base64.getEncoder().encodeToString(envelope.getNonce().toByteArray());
        if (redisService.hasKey(nonceKey))
        {
            throw new SecurityException("重复请求");
        }

        MessageType type = envelope.getType();
        if (requiresMac(type) && session.getSessionKey() != null)
        {
            if (!TcpHmacUtils.verify(session.getSessionKey(), envelope))
            {
                throw new SecurityException("消息完整性校验失败");
            }
        }
    }

    private void markNonceUsed(Envelope envelope)
    {
        String nonceKey = NONCE_REDIS_PREFIX + Base64.getEncoder().encodeToString(envelope.getNonce().toByteArray());
        redisService.setCacheObject(nonceKey, "1", (long) properties.getSecurity().getNonceTtlSec(), TimeUnit.SECONDS);
    }

    private boolean requiresMac(MessageType type)
    {
        return type != MessageType.GET_CAPTCHA
                && type != MessageType.LIST_PUBLIC_LINES
                && type != MessageType.LOGIN
                && type != MessageType.CHANGE_PASSWORD
                && type != MessageType.MESSAGE_TYPE_UNSPECIFIED;
    }

    private Envelope buildReply(Envelope request, RpcResponse response, TcpSessionContext session)
    {
        Envelope.Builder builder = Envelope.newBuilder()
                .setRequestId(StringUtils.isNotEmpty(request.getRequestId()) ? request.getRequestId() : "")
                .setTimestampMs(System.currentTimeMillis())
                .setNonce(ByteString.copyFromUtf8(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16)))
                .setType(request.getType())
                .setPayload(response.toByteString());
        if (session.getSessionKey() != null)
        {
            Envelope temp = builder.build();
            builder.setMac(ByteString.copyFrom(TcpHmacUtils.sign(session.getSessionKey(), temp)));
        }
        return builder.build();
    }
}
