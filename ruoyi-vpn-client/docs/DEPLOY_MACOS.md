# GenlotVPN 桌面客户端 — macOS 部署与运维手册

面向运维与最终用户。开发编译见 [MACOS_SETUP.md](MACOS_SETUP.md)。

## 1. 架构与端口

```
用户 Mac
  GenlotVPN.app
    ├─ 云端 TCP/TLS + Protobuf ──► ruoyi-vpn-auth :9443
    └─ 本地 HTTP ────────────────► 易安联 Agent :30303 (127.0.0.1)
```

| 方向 | 地址 | 说明 |
|------|------|------|
| 出站 | `<服务器>:9443` | 选线、登录、钉钉、凭证下发 |
| 本机 | `127.0.0.1:30303` | 控制器 / 网关 / 应用列表（易安联 Mac Agent） |
| 隧道 | 由 Agent 建立 | 非 GenlotVPN.app 直连 VPN 设备 |

## 2. 交付物

构建机执行：

```bash
export CMAKE_PREFIX_PATH="$HOME/Qt/6.11.1/macos"
bash bin/build-vpn-client-macos.sh --package
```

产物示例：`ruoyi-vpn-client/dist/GenlotVPN-macos-arm64-1.0.1/GenlotVPN.app`

将整个目录或 `.app` 拷贝到目标机即可（内测）。外发需代码签名与公证（见第 5 节）。

## 3. 部署前检查

| 项 | 要求 |
|----|------|
| 系统 | macOS 12+（构建默认 deployment target 12.0），优先 Apple Silicon |
| 易安联 Agent | 已安装并运行，`127.0.0.1:30303` 可访问 |
| 网络 | 可访问云端 **9443**（TLS） |
| 账号 | 平台已开通 VPN 用户与线路 |

验证 Agent：

```bash
curl -sS http://127.0.0.1:30303/api/v1/version/current
```

验证云端端口：

```bash
nc -vz <服务器IP> 9443
```

## 4. 配置与日志路径

| 用途 | 路径 |
|------|------|
| 可写配置 | `~/Library/Application Support/Genlot/GenlotVPN/config.json` |
| 默认模板 | `GenlotVPN.app/Contents/Resources/config.default.json` |
| 全量日志 | `~/Library/Application Support/Genlot/GenlotVPN/logs/genlot-vpn-YYYY-MM-DD.log` |
| 错误日志 | `~/Library/Application Support/Genlot/GenlotVPN/logs/genlot-vpn-error-YYYY-MM-DD.log` |
| 记住密码 | macOS Keychain（服务名 `com.genlot.GenlotVPN`） |

生产 `config.json` 要点：`useTls=true`、`certPinSha256`、按需 `controllerAesEnabled`（默认 true）。

## 5. 代码签名、公证与 DMG（外发）

内测可直接运行本地构建的 `.app`（可能需右键打开绕过 Gatekeeper）。外发建议：

1. **签名**（Developer ID Application）：
   ```bash
   codesign --deep --force --options runtime \
     --sign "Developer ID Application: YOUR NAME (TEAMID)" \
     dist/GenlotVPN-macos-arm64-x.y.z/GenlotVPN.app
   ```
2. **公证**：用 `notarytool` 提交 zip/dmg，通过后 `xcrun stapler staple GenlotVPN.app`。
3. **DMG**（示例）：
   ```bash
   hdiutil create -volname "Genlot VPN" -srcfolder dist/GenlotVPN-macos-arm64-x.y.z \
     -ov -format UDZO dist/GenlotVPN-macos-arm64-x.y.z.dmg
   ```

具体证书名、Apple ID、App 专用密码由团队保管，勿写入仓库。

## 6. 常见问题

| 现象 | 处理 |
|------|------|
| 无法打开，因为来自身份不明的开发者 | 完成签名公证；或系统设置 → 隐私与安全性 → 仍要打开 |
| 控制器 30303 失败 | 启动易安联 Mac Agent |
| 网关超时 | 检查 Agent 与线路可达性 |
| 改配置不生效 | 确认改的是 Application Support 下的 `config.json`，不是 `.app` 内只读副本 |

## 7. 安全说明（与 Windows 对齐）

- 云端：TLS + 证书 Pinning + 会话 HMAC
- 本机 30303：AES 为厂商协议常量，约束 loopback 明文可见性，不防同机恶意软件
- 记住密码：Keychain，非明文
