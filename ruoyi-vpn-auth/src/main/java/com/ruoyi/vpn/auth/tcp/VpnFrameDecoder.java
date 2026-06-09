package com.ruoyi.vpn.auth.tcp;

import java.util.List;
import com.ruoyi.vpn.auth.config.VpnTcpProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * GVPN 帧解码器
 */
public class VpnFrameDecoder extends ByteToMessageDecoder
{
    private final int maxFrameBytes;

    public VpnFrameDecoder(VpnTcpProperties properties)
    {
        this.maxFrameBytes = properties.getSecurity().getMaxFrameBytes();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    {
        if (in.readableBytes() < VpnFrameConstants.HEADER_LENGTH)
        {
            return;
        }
        in.markReaderIndex();
        byte[] magic = new byte[4];
        in.readBytes(magic);
        for (int i = 0; i < VpnFrameConstants.MAGIC.length; i++)
        {
            if (magic[i] != VpnFrameConstants.MAGIC[i])
            {
                in.resetReaderIndex();
                in.skipBytes(1);
                return;
            }
        }
        byte version = in.readByte();
        if (version != VpnFrameConstants.VERSION)
        {
            throw new IllegalStateException("不支持的协议版本: " + version);
        }
        int length = in.readInt();
        if (length <= 0 || length > maxFrameBytes)
        {
            throw new IllegalStateException("非法帧长度: " + length);
        }
        if (in.readableBytes() < length)
        {
            in.resetReaderIndex();
            return;
        }
        byte[] body = new byte[length];
        in.readBytes(body);
        out.add(body);
    }
}
