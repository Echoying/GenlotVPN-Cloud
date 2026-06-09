package com.ruoyi.vpn.auth.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * GVPN 帧编码器
 */
public class VpnFrameEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out)
    {
        out.writeBytes(VpnFrameConstants.MAGIC);
        out.writeByte(VpnFrameConstants.VERSION);
        out.writeInt(msg.length);
        out.writeBytes(msg);
    }
}
