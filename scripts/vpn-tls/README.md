# VPN TCP TLS 证书工具

为 `ruoyi-vpn-auth` TCP 9443 生成自签证书，并导出与 Qt 客户端 `CertificatePinner` 一致的 SPKI SHA-256 指纹。

## 生成证书

**Linux / Git Bash:**

```bash
sh scripts/vpn-tls/gen-cert.sh
# 自定义输出目录与 SAN IP:
sh scripts/vpn-tls/gen-cert.sh /path/to/certs 10.9.2.177
```

**Windows:**

```bat
scripts\vpn-tls\gen-cert.bat
```

默认输出到 `docker/ruoyi/vpn/auth/certs/server.crt` 与 `server.key`。

## 导出 Pin 指纹

```bash
sh scripts/vpn-tls/export-pin.sh docker/ruoyi/vpn/auth/certs/server.crt
```

```bat
scripts\vpn-tls\export-pin.bat docker\ruoyi\vpn\auth\certs\server.crt
```

将输出的 hex 填入客户端 `config.json` 的 `certPinSha256` 字段。

## 算法说明

指纹 = `SHA-256(SPKI DER)`，与 Qt `QSslKey::toDer()` 后哈希一致。

详细联调步骤见 `ruoyi-vpn-client/docs/TLS_PINNING.md`。
