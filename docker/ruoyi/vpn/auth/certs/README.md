# VPN TCP TLS 证书目录

将 `server.crt` 与 `server.key` 放在此目录（勿提交私钥到 Git）。

生成方式：

```bash
sh ../../../scripts/vpn-tls/gen-cert.sh
```

或 Windows：

```bat
..\..\..\scripts\vpn-tls\gen-cert.bat
```

Docker 挂载为容器内 `/home/ruoyi/certs/`（只读）。
