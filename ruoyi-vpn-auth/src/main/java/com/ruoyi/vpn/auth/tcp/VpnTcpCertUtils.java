package com.ruoyi.vpn.auth.tcp;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VPN TCP 证书工具（导出 SPKI Pin 指纹，与 Qt CertificatePinner 对齐）
 */
public final class VpnTcpCertUtils
{
    private static final Logger log = LoggerFactory.getLogger(VpnTcpCertUtils.class);

    private VpnTcpCertUtils()
    {
    }

    /**
     * 从 PEM 证书文件计算 SPKI SHA-256 十六进制指纹
     */
    public static String spkiSha256Hex(String certPath)
    {
        try (FileInputStream in = new FileInputStream(certPath))
        {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory.generateCertificate(in);
            byte[] spkiDer = cert.getPublicKey().getEncoded();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(spkiDer);
            return bytesToHex(hash);
        }
        catch (Exception e)
        {
            log.warn("无法读取证书 Pin 指纹: {}", certPath, e);
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
