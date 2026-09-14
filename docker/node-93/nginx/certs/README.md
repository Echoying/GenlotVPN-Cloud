# 下载站 HTTPS 证书（独立于 vpn-auth）

在 **93** 本机生成 `download.crt` / `download.key`，放在本目录。

- **不入库**（见仓库根 `.gitignore`：`docker/node-93/nginx/certs/*.key`、`*.crt`）。
- **不要**与 vpn-auth 的 9443 证书（`docker/node-93/ruoyi/vpn/auth/certs/`）混用。

生成示例（仅在 93 上执行，勿把私钥拷进 Git）：

```bash
openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
  -keyout download.key \
  -out download.crt \
  -subj "/CN=genlotvpn-download"
```

Docker 挂载为容器内 `/etc/nginx/certs/`（只读）。
