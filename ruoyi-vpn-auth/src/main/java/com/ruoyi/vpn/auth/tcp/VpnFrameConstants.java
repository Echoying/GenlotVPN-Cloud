package com.ruoyi.vpn.auth.tcp;

/**
 * TCP 帧常量
 */
public final class VpnFrameConstants
{
    public static final byte[] MAGIC = new byte[] { 'G', 'V', 'P', 'N' };

    public static final byte VERSION = 0x01;

    public static final int HEADER_LENGTH = 9;

    private VpnFrameConstants()
    {
    }
}
