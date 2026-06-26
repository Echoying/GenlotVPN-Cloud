package com.ruoyi.yianlian.utils;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.vo.VpnOfflineLoginDatPayload;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 离线登录 .dat 完整性签名（HMAC-SHA256）
 */
public final class OfflineLoginIntegrity
{
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private OfflineLoginIntegrity()
    {
    }

    public static String buildCanonical(VpnOfflineLoginDatPayload payload)
    {
        StringBuilder sb = new StringBuilder(256);
        appendLine(sb, "app_id", payload.getAppId());
        appendLine(sb, "app_name", payload.getAppName());
        appendLine(sb, "expire_at", payload.getExpireAt());
        appendLine(sb, "host", payload.getHost());
        appendLine(sb, "local_password_hash", payload.getLocalPasswordHash());
        appendLine(sb, "local_user_name", payload.getLocalUserName());
        appendLine(sb, "password", payload.getPassword());
        appendLine(sb, "spa_key", payload.getSpaKey());
        appendLine(sb, "spa_port", payload.getSpaPort());
        appendLine(sb, "srv_port", payload.getSrvPort());
        appendLine(sb, "user_name", payload.getUserName());
        return sb.toString();
    }

    public static String sign(String canonical, String hmacKey)
    {
        if (StringUtils.isEmpty(hmacKey))
        {
            throw new IllegalArgumentException("HMAC 密钥未配置");
        }
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("离线登录 HMAC 签名失败: " + e.getMessage(), e);
        }
    }

    public static boolean verify(String canonical, String hmacKey, String payloadHmac)
    {
        if (StringUtils.isEmpty(payloadHmac))
        {
            return false;
        }
        String expected = sign(canonical, hmacKey);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            payloadHmac.getBytes(StandardCharsets.UTF_8));
    }

    private static void appendLine(StringBuilder sb, String key, Object value)
    {
        sb.append(key).append('=');
        if (value == null)
        {
            sb.append("");
        }
        else
        {
            sb.append(value);
        }
        sb.append('\n');
    }
}
