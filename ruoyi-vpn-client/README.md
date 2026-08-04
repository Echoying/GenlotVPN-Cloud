# GenlotVPN 桌面客户端（Qt 6 + C++）

Windows/macOS 跨平台 VPN 登录客户端。UI 风格对齐 [Genlot 官网](https://www.genlot.com/)（深蓝 + 金色点缀）。

## 功能

- 云端：**TLS + TCP + Protobuf**（`ruoyi-vpn-auth` 端口 9443）
- 本地：易安联控制器 HTTP `127.0.0.1:30303`
- 完整流程：选线 → 登录（验证码）→ 钉钉验证 → 控制器连接 → 网关/应用列表

## 依赖

| 平台 | 文档 | 一键检查 |
|------|------|----------|
| Windows | [docs/WINDOWS_SETUP.md](docs/WINDOWS_SETUP.md) | `bin\check-vpn-client-deps.bat` |
| macOS | [docs/MACOS_SETUP.md](docs/MACOS_SETUP.md) | `bash bin/check-vpn-client-deps-macos.sh` |

**Windows 组件**：Visual Studio 2022、CMake 3.21+、Qt 6.6+ MSVC 64-bit、Protobuf 3.x  

**macOS 组件**：Xcode CLT、CMake 3.21+、Qt **6.6/6.7** macOS（Monterey 勿用 6.11）、Protobuf（Homebrew）  
详细步骤与踩坑：[docs/MACOS_SETUP.md](docs/MACOS_SETUP.md)

**暂未安装 Qt/Protobuf 时**：可先只构建后端 `mvn package -pl ruoyi-vpn-auth -am -DskipTests` 并用 TCP 9443 联调；桌面客户端待环境就绪后再编译。

## 构建（Release）

### Windows（W-01）

```bat
set CMAKE_PREFIX_PATH=D:\apps\Qt\6.11.1\msvc2022_64
set Protobuf_ROOT=D:\anaconda3\Library
bin\check-vpn-client-deps.bat
bin\build-vpn-client.bat
```

产物：`ruoyi-vpn-client\build-msvc2022\Release\GenlotVPN.exe`

### macOS

```bash
export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"   # Monterey 用 6.6/6.7，勿用 6.11
export CMAKE_OSX_ARCHITECTURES="$(uname -m)"      # Intel=x86_64，Apple Silicon=arm64
export Protobuf_ROOT=/usr/local                   # Apple Silicon: /opt/homebrew
bash bin/check-vpn-client-deps-macos.sh
bash bin/build-vpn-client-macos.sh
# 打包到 dist/：
bash bin/build-vpn-client-macos.sh --package
```

清理后全量重编：

```bash
rm -rf ruoyi-vpn-client/build-macos
bash bin/build-vpn-client-macos.sh
```

产物：`ruoyi-vpn-client/build-macos/GenlotVPN.app`  
分发目录：`ruoyi-vpn-client/dist/GenlotVPN-macos-{arch}-{version}/`（经 `macdeployqt`，含 Qt 框架/插件与 libprotobuf）  
完整编译手册：[docs/MACOS_SETUP.md](docs/MACOS_SETUP.md)  
打包与运维：[docs/DEPLOY_MACOS.md](docs/DEPLOY_MACOS.md)

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
5. **构建目录** 建议与脚本一致：`ruoyi-vpn-client/build-msvc2022`（**项目 → 构建目录**），避免默认 `build/Desktop_Qt_*` 与 `detached*` 多套目录并存

### Qt Creator 13+ 编译卡死 / 长时间无输出

现象：点构建后编译输出停在 `VpnFlowController.cpp`、Protobuf 头（`D:\anaconda3\Library\include\google\protobuf\...`）等处不动，或右下角 **Indexing … with clangd** 进度为 0；重启 IDE 后又能编过。多为 **clangd / 代码模型索引** 与 **MSVC 编译** 同时读写 `build` 目录、争抢 CPU/磁盘所致，并非业务代码错误。

**推荐设置（按顺序）**：

1. **编辑 → Preferences → C++ → Clangd**：取消 **Use clangd**
2. **编辑 → Preferences → C++ → 代码模型**：
   - 取消 **启用索引**（或编译期间临时关闭）
   - **忽略文件** 增加（路径按本机调整）：
     ```
     D:/anaconda3/*
     */build/*
     */build-msvc2022/*
     ```
3. **项目 → 构建**：并行任务数改为 **1～2**
4. 关闭 Qt Creator，删除 `ruoyi-vpn-client/build` 下旧的 `Desktop_Qt_*`、`detached*` 目录，**构建 → 清除 CMake 配置** 后 **Release** 全量重编
5. **Windows Defender 排除项**（设置 → 隐私和安全性 → Windows 安全中心 → 病毒和威胁防护 → 管理设置 → 排除项 → 添加文件夹）：
   - `D:\anaconda3\Library`
   - `D:\cursor\genlot\GenlotVPN-Cloud\ruoyi-vpn-client\build-msvc2022`（及仍在使用的 `build` 目录）
   - 可选：`D:\apps\Qt\6.11.1`

  管理员 PowerShell 等价命令：
   ```PowerShell
    Add-MpPreference -ExclusionPath "D:\anaconda3\Library"
    Add-MpPreference -ExclusionPath "D:\cursor\genlot\GenlotVPN-Cloud\ruoyi-vpn-client\build-msvc2022"
    Add-MpPreference -ExclusionPath "D:\cursor\genlot\GenlotVPN-Cloud\ruoyi-vpn-client\build"
   ```

**区分「卡死」与「编译慢」**：任务管理器中若 `cl.exe` 持续占 CPU，可能只是在编译 Protobuf 相关单元，需多等几分钟；若 `cl.exe` 无 CPU 且 IDE 无响应，按上表调整。

**绕过 IDE**：日常编译可只用 `bin\build-vpn-client.bat`，Qt Creator 仅用于编辑与调试。

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
- `controllerAesEnabled`：控制本机 Agent `30303` 全包 AES（默认 `true`），见 `config.default.json`

## 部署与运维

- Windows：**[docs/DEPLOY_WINDOWS.md](docs/DEPLOY_WINDOWS.md)**
- macOS：**[docs/DEPLOY_MACOS.md](docs/DEPLOY_MACOS.md)**（配置/日志在 `~/Library/Application Support/Genlot/GenlotVPN/`）

## 联调步骤

1. 启动 Nacos、Redis、`ruoyi-vpn-auth`（含 TCP 9443）
2. 本机安装并启动易安联 Agent（30303）
3. 运行 `GenlotVPN-{version}.exe`（如 `GenlotVPN-1.0.0.exe`）
4. 选线 → 登录 → 钉钉验证码 → 自动连接控制器 → 应用列表

## 安全说明

- 云端传输：生产环境 **TLS 1.3 + 证书 Pinning**（自签证书以 SPKI 指纹为信任锚）
- 登录后 RPC 携带 **HMAC-SHA256(session_key)**
- 启用 TLS 时必须配置 `certPinSha256`（64 位 hex）
- 记住密码：Windows 使用 DPAPI；macOS 使用 Keychain（`com.genlot.GenlotVPN`）
- 控制器密码：云端经 TLS 下发表字段级 AES 密文，客户端原样写入 `loginWithAccount`；`controllerAesEnabled=true` 时再对整段 JSON 做传输层 AES 加密
- **30303 AES 密钥/IV**：与易安联 Agent SDK 一致的**协议固定参数**（非业务密钥），硬编码于客户端以便与本机 Agent 互通；主要约束本机回环上的明文 JSON 可见性，**不抵御**同机恶意软件或逆向。远程敏感数据依赖 **TLS 9443** 与 Agent 登录后会话；`controllerAesEnabled` 用于协议版本/兼容，勿当作额外安全开关

## 日志

- 日志仅写入本地文件，界面不展示
- Windows 全量 / 报错日志：`<exe目录>/logs/genlot-vpn-*.log`、`genlot-vpn-error-*.log`
- macOS：`~/Library/Application Support/Genlot/GenlotVPN/logs/`（同上文件名）
- 云端 TCP/RPC、本地控制器 HTTP 请求失败均会写入报错日志

## 国际化

- 语言文件：`i18n/genlotvpn_zh_CN.ts`、`i18n/genlotvpn_en.ts`
- 构建时 CMake `qt_add_translations` 自动 `lupdate` / `lrelease`，`.qm` 输出到 exe 同目录 `i18n/`
- **设置 → 语言** 可切换简体中文 / English，立即生效
- `config.json` 字段 `locale`：`zh_CN`（默认）或 `en`
- 新增 UI 文案请使用 QML `qsTr()` / C++ `tr()`，改完后重新构建以更新 `.ts`

手动更新翻译源（可选）：

```bat
cd ruoyi-vpn-client
lupdate . -ts i18n/genlotvpn_zh_CN.ts i18n/genlotvpn_en.ts
lrelease i18n -qm build-msvc2022/i18n
```

## 协议

见 [`../proto/README.md`](../proto/README.md)
