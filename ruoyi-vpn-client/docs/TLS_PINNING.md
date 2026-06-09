# TLS + 证书 Pinning 联调指南

桌面客户端通过 **TLS 1.3 + TCP + Protobuf** 连接 `ruoyi-vpn-auth:9443`。内网自签证书场景下，客户端以 **SPKI SHA-256 指纹（Pin）** 为信任锚。

## 一、生成服务端证书

需本机已安装 OpenSSL。

**Git Bash / Linux:**

```bash
sh scripts/vpn-tls/gen-cert.sh
```

**Windows CMD:**

```bat
scripts\vpn-tls\gen-cert.bat
```

默认输出：

- `docker/ruoyi/vpn/auth/certs/server.crt`
- `docker/ruoyi/vpn/auth/certs/server.key`（勿提交 Git）

SAN 默认包含 `IP:10.9.2.177`，可传第二参数覆盖。

## 二、导出 Pin 指纹

```bash
sh scripts/vpn-tls/export-pin.sh docker/ruoyi/vpn/auth/certs/server.crt
```

```bat
scripts\vpn-tls\export-pin.bat docker\ruoyi\vpn\auth\certs\server.crt
```

输出示例：

```
certPinSha256: a1b2c3...
```

算法：`SHA-256(证书公钥 SPKI DER)`，与 Qt `CertificatePinner` 及 Java 启动日志一致。

## 三、启用后端 TLS

### 本地 jar 运行

在 Nacos `ruoyi-vpn-auth-dev.yml` 合并 [`sql/update/ruoyi-vpn-auth-prod-tls.yml`](../../sql/update/ruoyi-vpn-auth-prod-tls.yml)，并将路径改为本机绝对路径，例如：

```yaml
vpn:
  tcp:
    tls:
      enabled: true
      cert-path: D:/cursor/genlot/GenlotVPN-Cloud/docker/ruoyi/vpn/auth/certs/server.crt
      key-path: D:/cursor/genlot/GenlotVPN-Cloud/docker/ruoyi/vpn/auth/certs/server.key
```

发布配置后 **重启 ruoyi-vpn-auth**。

启动日志应包含：

```
VPN TCP TLS 已启用，cert-path=...
VPN TCP 证书 SPKI Pin（供客户端 certPinSha256）: <hex>
```

### Docker 部署

1. 证书放在 `docker/ruoyi/vpn/auth/certs/`
2. `docker-compose` 已挂载为容器内 `/home/ruoyi/certs`（只读）
3. Nacos 使用容器路径：

```yaml
cert-path: /home/ruoyi/certs/server.crt
key-path: /home/ruoyi/certs/server.key
```

## 四、配置客户端

### 方式 A：设置页

1. 选线页 → **设置** → **安全连接**
2. 打开 **启用 TLS**
3. 填入 **证书指纹（主）**（`export-pin` 输出）
4. **保存**

### 方式 B：config.json

复制 [`resources/config.prod.example.json`](../resources/config.prod.example.json) 为 exe 同目录 `config.json`：

```json
{
  "serverHost": "10.9.2.177",
  "serverPort": 9443,
  "useTls": true,
  "certPinSha256": "<64位hex>",
  "certPinSha256Backup": ""
}
```

开发联调可保持 `useTls: false`（明文 TCP）。

## 五、验收清单

| 步骤 | 预期 |
|------|------|
| 后端 `tls.enabled=true` | 明文 TCP 连接失败 |
| Wireshark 抓 9443 | 仅 TLS 密文 |
| 客户端正确 Pin | 选线 → 登录全流程正常 |
| 故意填错 Pin | 连接失败，`logs/genlot-vpn-error-*.log` 含 `证书 Pinning 校验失败` |
| 启用 TLS 但不填 Pin | 提示「必须填写证书指纹」 |
| 修改设置后重启客户端 | `config.json` 配置仍生效 |

## 六、证书轮换

1. 生成新证书，导出 **新 Pin**
2. 将新 Pin 填入客户端 **备用指纹**
3. 服务端切换新证书
4. 确认无误后，将新 Pin 提升为主指纹，清空备用

## 七、指纹一致性校验

本机生成证书后，OpenSSL 与 Java 启动日志中的 Pin 应一致。示例（每次生成证书后值会不同）：

```bash
sh scripts/vpn-tls/export-pin.sh docker/ruoyi/vpn/auth/certs/server.crt
```

重启 `ruoyi-vpn-auth` 后对照日志行：

```
VPN TCP 证书 SPKI Pin（供客户端 certPinSha256）: <hex>
```

两者 hex 必须完全相同，再写入客户端配置。

## 八、故障排查

| 现象 | 可能原因 |
|------|----------|
| 连接超时 | 防火墙未放行 TCP 9443；服务未启动 |
| TLS 握手失败 | 后端未启用 TLS 或证书路径错误 |
| Pinning 校验失败 | 指纹与当前 `server.crt` 不一致 |
| 后端启动失败 | `cert-path`/`key-path` 为空或文件不存在 |

日志位置：`<exe目录>/logs/genlot-vpn-error-YYYY-MM-DD.log`
