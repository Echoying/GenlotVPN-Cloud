package com.ruoyi.vpn.auth.tcp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.ruoyi.vpn.protocol.Envelope;

/**
 * HMAC 工具
 */
public final class TcpHmacUtils
{
    private static final String HMAC_ALGO = "HmacSHA256";

    private TcpHmacUtils()
    {
    }

    public static byte[] sign(byte[] sessionKey, Envelope envelope)
    {
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(sessionKey, HMAC_ALGO));
            mac.update(int32(envelope.getTypeValue()));
            mac.update(int64(envelope.getTimestampMs()));
            if (envelope.getNonce() != null && !envelope.getNonce().isEmpty())
            {
                mac.update(envelope.getNonce().toByteArray());
            }
            if (envelope.getPayload() != null && !envelope.getPayload().isEmpty())
            {
                mac.update(envelope.getPayload().toByteArray());
            }
            return mac.doFinal();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    public static boolean verify(byte[] sessionKey, Envelope envelope)
    {
        if (envelope.getMac() == null || envelope.getMac().isEmpty())
        {
            return false;
        }
        byte[] expected = sign(sessionKey, envelope);
        return MessageDigestSafeEquals(expected, envelope.getMac().toByteArray());
    }

    private static byte[] int32(int value)
    {
        return new byte[] {
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }

    private static byte[] int64(long value)
    {
        return new byte[] {
            (byte) (value >>> 56),
            (byte) (value >>> 48),
            (byte) (value >>> 40),
            (byte) (value >>> 32),
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }

    private static boolean MessageDigestSafeEquals(byte[] a, byte[] b)
    {
        if (a == null || b == null || a.length != b.length)
        {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++)
        {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
