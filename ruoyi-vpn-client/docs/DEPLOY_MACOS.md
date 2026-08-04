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

## 2. 打包交付物

构建机（仓库根目录）：

```bash
export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"   # Monterey 用 6.6/6.7
export CMAKE_OSX_ARCHITECTURES="$(uname -m)"
export Protobuf_ROOT=/usr/local                   # Apple Silicon: /opt/homebrew

# 构建并打包（推荐）
bash bin/build-vpn-client-macos.sh --package

# 或仅对已有 build-macos/GenlotVPN.app 打包
bash bin/package-vpn-client-macos.sh
```

产物示例：

- 临时：`ruoyi-vpn-client/dist/GenlotVPN-macos-x86_64-1.0.1/GenlotVPN.app`（gitignore，不提交）
- **进仓库**：`apps/macos/GenlotVPN-macos-x86_64-1.0.1.zip`（打包成功后自动生成）
- 本机副本：`apps/macos/.../GenlotVPN.app`（gitignore，方便本机双击）

```bash
# 打包成功后提交分发 zip
git add apps/README.md apps/macos/*.zip
git commit -m "release: macOS 客户端 1.0.1 (x86_64)"
git push
```

详见仓库根目录 [`apps/README.md`](../../apps/README.md)。

将 zip 解压后发给同事即可（内测）。外发需签名与公证（第 5 节）。

脚本会执行：

1. `macdeployqt`：嵌入 Qt 框架、platforms / tls / imageformats / qml 等插件，以及 `Resources/qml/QtQuick*`  
2. 拷贝 Homebrew `libprotobuf*.dylib` 到 `Contents/Frameworks`，并改写 `@executable_path`  
3. 确保业务 `Resources/GenlotVPN/`、`config.default.json`、`genlot-app.icns`、i18n  
4. **自检**：核心 Qt 框架、cocoa/tls 插件、无本机绝对路径残留；失败则非 0 退出  
5. 同步到仓库 `apps/macos/`（zip 进 Git；未压缩 `.app` 仅本机）

### 包内应有（抽查）

| 路径 | 用途 |
|------|------|
| `Contents/MacOS/GenlotVPN`（→ `GenlotVPN-x.y.z`） | 主程序 |
| `Contents/Frameworks/Qt*.framework` | Qt 运行时 |
| `Contents/Frameworks/libprotobuf*.dylib` | Protobuf |
| `Contents/PlugIns/platforms/libqcocoa.dylib` | Cocoa 平台插件 |
| `Contents/PlugIns/tls/*` | TLS 后端（含 SecureTransport / OpenSSL 插件） |
| `Contents/Resources/qml/QtQuick/` | Qt QML 模块 |
| `Contents/Resources/GenlotVPN/qmldir` | 业务 QML |
| `Contents/Resources/genlot-app.icns` | 应用图标 |
| `Contents/Resources/config.default.json` | 首次运行配置模板 |

**不要**只拷贝未打包的 `build-macos/GenlotVPN.app`（未跑 macdeployqt 时仍依赖本机 Qt `@rpath`，换机无法运行）。

将整个 `dist/GenlotVPN-macos-…/` 目录或其中 `.app` 拷到目标机即可（内测）。外发需签名与公证（第 5 节）。

### 干净机器冒烟（建议）

在未安装 Qt 的 Mac 上：

```bash
# 不应再依赖 /usr/local/opt/qt 或 ~/Qt
otool -L GenlotVPN.app/Contents/MacOS/GenlotVPN-*.??* | grep -E '/usr/local|/Users/|/opt/homebrew' || echo OK
open GenlotVPN.app
```

## 3. 部署前检查

| 项 | 要求 |
|----|------|
| 系统 | macOS 12+（构建默认 deployment target 12.0） |
| 易安联 Agent | 已安装并运行，`127.0.0.1:30303` 可访问 |
| 网络 | 可访问云端 **9443**（TLS） |
| 账号 | 平台已开通 VPN 用户与线路 |
| 钉钉验证码 | 默认走 Nacos `dingtalk.robot`；业务用户可在管理端 **VPN 角色** 单独配置验证码群（未配置则回退默认群） |

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
| 闪退 / dyld 找不到 Qt | 用了未 `macdeployqt` 的 build 目录；请重新 `--package` |
| 控制器 30303 失败 | 启动易安联 Mac Agent |
| 网关超时 | 检查 Agent 与线路可达性 |
| 改配置不生效 | 确认改的是 Application Support 下的 `config.json`，不是 `.app` 内只读副本 |
| 打包日志出现 libpq / libiodbc ERROR | 来自无用的 Qt SQL 插件；脚本会删除 `sqldrivers`，可忽略 |

## 7. 安全说明（与 Windows 对齐）

- 云端：TLS + 证书 Pinning + 会话 HMAC
- 本机 30303：AES 为厂商协议常量，约束 loopback 明文可见性，不防同机恶意软件
- 记住密码：Keychain，非明文
