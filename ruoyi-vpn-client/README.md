# GenlotVPN 桌面客户端（Qt 6 + C++）

Windows/macOS 跨平台 VPN 登录客户端。UI 风格对齐 [Genlot 官网](https://www.genlot.com/)（深蓝 + 金色点缀）。

## 功能

- 云端：**TLS + TCP + Protobuf**（`ruoyi-vpn-auth` 端口 9443）
- 本地：易安联控制器 HTTP `127.0.0.1:30303`
- 完整流程：选线 → 登录（验证码）→ 钉钉验证 → 控制器连接 → 网关/应用列表

## 依赖

本机需安装开发环境，**详见 [docs/WINDOWS_SETUP.md](docs/WINDOWS_SETUP.md)**。

| 组件 | 说明 |
|------|------|
| Visual Studio 2022 | C++ 桌面开发 |
| CMake 3.21+ | 构建 |
| Qt 6.6+ MSVC 64-bit | UI |
| Protobuf 3.x | 协议（推荐 vcpkg） |

检查依赖：`bin\check-vpn-client-deps.bat`

**暂未安装 Qt/Protobuf 时**：可先只构建后端 `mvn package -pl ruoyi-vpn-auth -am -DskipTests`，Web 端 `ruoyi-vpn-ui` 仍可 HTTP 登录；桌面客户端待环境就绪后再编译。

## 构建（Release）

**推荐：一键脚本（W-01）**

```bat
set CMAKE_PREFIX_PATH=D:\apps\Qt\6.11.1\msvc2022_64
set Protobuf_ROOT=D:\anaconda3\Library
bin\check-vpn-client-deps.bat
bin\build-vpn-client.bat
```

产物：`ruoyi-vpn-client\build-msvc2022\Release\GenlotVPN.exe`

**打包依赖（W-02，windeployqt + libprotobuf.dll）**

必须先 **Release 构建**（不要用 Debug，否则干净机器会闪退并提示 QML debugging）。

```bat
bin\package-vpn-client.bat
```

或构建并打包一步完成：

```bat
bin\build-vpn-client.bat --package
```

可分发目录：`ruoyi-vpn-client\dist\GenlotVPN-win64-{version}\`（示例 `GenlotVPN-win64-1.0.0\`，内含 `GenlotVPN-{version}.exe`、Qt DLL、`config.json`，可在无开发环境的 Windows 机器运行）

版本号：修改 `CMakeLists.txt` 中 `project(GenlotVPN VERSION x.y.z)` 后重新打包，目录与 exe 名称自动带上该版本；构建时自动附加 Git 短提交。界面见窗口标题与 **设置 → 关于**，exe 属性见「详细信息」。

**手动 CMake（与脚本等价）**

```bat
cd ruoyi-vpn-client
cmake -B build-msvc2022 -G "Visual Studio 17 2022" -A x64 ^
  -DCMAKE_PREFIX_PATH=D:/apps/Qt/6.11.1/msvc2022_64 ^
  -DProtobuf_ROOT=D:/anaconda3/Library
cmake --build build-msvc2022 --config Release
```

**重要（Qt Creator）**：

1. **Kit** 必须选 **Desktop Qt 6.11.1 MSVC2022 64bit**（不要用 MinGW / ARM64 / WebAssembly）
2. **构建类型** 选 **Release**（Anaconda 的 `libprotobuf.dll` 为 Release 版，Debug 会 `0xC0000005` 崩溃）
3. CMake 初始配置建议包含：`-DProtobuf_ROOT=D:/anaconda3/Library`（路径按本机 Anaconda 调整）
4. 若提示 “Project did not parse successfully”，多半是选错了 MinGW Kit；切换到 MSVC 后点 **构建 → 重新运行 CMake**

运行前将 `build/config.json` 放在 exe 同目录，或通过 `vpnStorage` 保存服务器地址。

## 配置 config.json

**开发（明文 TCP）：**

```json
{
  "serverHost": "10.9.2.177",
  "serverPort": 9443,
  "useTls": false,
  "certPinSha256": ""
}
```

**生产（TLS + Pinning）：** 见 [`resources/config.prod.example.json`](resources/config.prod.example.json)，完整步骤见 **[docs/TLS_PINNING.md](docs/TLS_PINNING.md)**。

- 证书生成：`scripts/vpn-tls/gen-cert.bat`
- Pin 导出：`scripts/vpn-tls/export-pin.bat`
- 设置页：**安全连接** → 启用 TLS → 填写证书指纹

## 部署与运维（Windows）

生产/内网分发、防火墙、Agent、日志排障：**[docs/DEPLOY_WINDOWS.md](docs/DEPLOY_WINDOWS.md)**

## 联调步骤

1. 启动 Nacos、Redis、`ruoyi-vpn-auth`（含 TCP 9443）
2. 本机安装并启动易安联 Agent（30303）
3. 运行 `GenlotVPN-{version}.exe`（如 `GenlotVPN-1.0.0.exe`）
4. 选线 → 登录 → 钉钉验证码 → 自动连接控制器 → 应用列表

## 安全说明

- 云端传输：生产环境 **TLS 1.3 + 证书 Pinning**（自签证书以 SPKI 指纹为信任锚）
- 登录后 RPC 携带 **HMAC-SHA256(session_key)**
- 启用 TLS 时必须配置 `certPinSha256`（64 位 hex）
- 记住密码使用 Windows DPAPI 加密存储（macOS 计划 Keychain）
- 控制器密码使用服务端 AES 密文，**客户端不解密**，直接传给 Agent

## 日志

- 日志仅写入本地文件，界面不展示
- 全量日志：`<exe目录>/logs/genlot-vpn-YYYY-MM-DD.log`
- **报错专用**：`<exe目录>/logs/genlot-vpn-error-YYYY-MM-DD.log`（ERROR 级别双写并立即刷盘）
- 云端 TCP/RPC、本地控制器 HTTP 请求失败均会写入报错日志

## 协议

见 [`../proto/README.md`](../proto/README.md)
