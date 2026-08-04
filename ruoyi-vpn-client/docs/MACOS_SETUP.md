# GenlotVPN 桌面客户端 — macOS 开发环境

面向开发者：在 Mac 上编译 `GenlotVPN.app`。  
运维/分发见 [DEPLOY_MACOS.md](DEPLOY_MACOS.md)；Windows 见 [WINDOWS_SETUP.md](WINDOWS_SETUP.md)。

## 依赖

| 组件 | 说明 |
|------|------|
| Xcode / CLT | Apple Clang（版本须匹配本机 macOS，见下文 Monterey 专节） |
| CMake 3.21+ | `brew install cmake`（建议带 Ninja：`brew install ninja`） |
| Qt 6.6+ macOS | Online Installer 选 **macOS** 套件；Monterey 建议 **6.6 / 6.7**，勿强上最新 6.11 |
| Protobuf 3.x | `brew install protobuf` |

检查：

```bash
export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"   # 按本机实际版本调整
bash bin/check-vpn-client-deps-macos.sh
```

## macOS Monterey 12.7.x（重要）

Monterey 是构建**主机**时的上限约束：

| 组件 | Monterey 上建议版本 | 说明 |
|------|---------------------|------|
| Xcode | **14.2**（最高） | App Store 最新 Xcode 装不上；从 [Apple Developer Downloads](https://developer.apple.com/download/all/) 搜 `Xcode 14.2` |
| 命令行工具 | 与 Xcode 14.2 配套，或 `xcode-select --install` | `clang --version` 能出号即可 |
| Qt | **6.6.x 或 6.7.x** | Qt 6.8+ / 6.11 官方构建环境要求更新的 Xcode/SDK，在 12.7 上易失败 |
| Homebrew | 仍可用；Intel 前缀 `/usr/local`，Apple Silicon 为 `/opt/homebrew` | 若 brew 提示系统过旧，可继续用已装 bottle 或从源码装 cmake/protobuf |

先确认芯片与路径：

```bash
uname -m                    # arm64 = Apple Silicon；x86_64 = Intel
sw_vers                     # 应显示 12.7.x
```

### Monterey 安装步骤（推荐顺序）

1. **安装编译器**。本项目用 CMake + Clang 构建，**不需要完整 Xcode IDE**，装 Command Line Tools（CLT）即可：

   ```bash
   xcode-select --install
   ```

   验证：

   ```bash
   xcode-select -p          # 期望 /Library/Developer/CommandLineTools
   clang --version
   ```

   若确实需要完整 Xcode（`.xip` 解压后拖到 `/Applications`）：

   ```bash
   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
   sudo xcodebuild -license accept
   xcodebuild -version      # 期望 Xcode 14.2
   ```

   > **`xcode-select --install` 预估几十小时下不动**：这是 Apple 更新服务器（`swcdn.apple.com`）限速导致，多数情况不会自行变快。改用直链下载：
   > 1. 打开 [Apple Developer Downloads](https://developer.apple.com/download/all/)，用**免费 Apple ID** 登录（无需付费会员）
   > 2. 搜索 `Command Line Tools`，下载 **Command Line Tools for Xcode 14.2**（约 700 MB，远小于完整 Xcode 的 ~7 GB）
   > 3. 双击 `.dmg` 安装，再执行上面的 `xcode-select -p` 验证
   >
   > 也可在其他机器下好后用 U 盘拷过来；或换手机热点 / 有线网络重试（公司代理常对 Apple CDN 限速）。
   > 想继续用系统安装器可试：`softwareupdate --list` 后 `sudo softwareupdate -i "Command Line Tools for Xcode-14.2" --verbose`。
2. **安装 Homebrew**（若还没有）：
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   # Apple Silicon 按提示把 brew 加入 PATH；Intel 一般已是 /usr/local/bin/brew
   ```
3. **安装构建工具与 protobuf**：
   ```bash
   brew install cmake ninja protobuf
   protoc --version
   ```
4. **安装 Qt 6.6 或 6.7（macOS 套件）**  
   - 用 [Qt Online Installer](https://www.qt.io/download-qt-installer)  
   - 勾选：`Qt 6.6.x` 或 `Qt 6.7.x` → **macOS**（含 Qt Quick / Network 等桌面组件）  
   - 安装后目录类似：`$HOME/Qt/6.7.3/macos`
5. **拉代码并检查依赖**（在仓库根目录）：
   ```bash
   export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"   # 改成你的版本
   # Intel Mac 若 protobuf 在 /usr/local：
   # export Protobuf_ROOT=/usr/local
   # Apple Silicon：
   # export Protobuf_ROOT=/opt/homebrew
   bash bin/check-vpn-client-deps-macos.sh
   ```
6. **构建**：
   ```bash
   # Apple Silicon
   export CMAKE_OSX_ARCHITECTURES=arm64
   # Intel Monterey 请改成：
   # export CMAKE_OSX_ARCHITECTURES=x86_64

   bash bin/build-vpn-client-macos.sh
   # 或连带打包：
   bash bin/build-vpn-client-macos.sh --package
   ```

若 `macdeployqt` / 链接阶段报 SDK 或 Clang 过旧，优先确认是 **Xcode 14.2 + Qt 6.6/6.7**，不要混用 Homebrew 最新 `qt`（可能拉到需更新 SDK 的版本）。

## 构建（Release）

在仓库根目录：

```bash
export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"
# 可选：export Protobuf_ROOT=/opt/homebrew   # 或 /usr/local
bash bin/build-vpn-client-macos.sh
```

产物：`ruoyi-vpn-client/build-macos/GenlotVPN.app`

构建并打包到 `dist/`：

```bash
bash bin/build-vpn-client-macos.sh --package
```

手动 CMake：

```bash
cd ruoyi-vpn-client
cmake -B build-macos -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos" \
  -DCMAKE_OSX_ARCHITECTURES="$(uname -m)"
cmake --build build-macos
open build-macos/GenlotVPN.app
```

## 运行前注意

1. 本机安装并启动**易安联 macOS Agent**，确认 `curl http://127.0.0.1:30303/api/v1/version/current` 可用。
2. 可写配置与日志目录：`~/Library/Application Support/Genlot/GenlotVPN/`（`config.json`、`logs/`）。
3. 首次运行会从 `.app/Contents/Resources/config.default.json` 生成可写 `config.json`。
4. 生产环境请在设置页或 `config.json` 中启用 TLS 并填写 `certPinSha256`（见 [TLS_PINNING.md](TLS_PINNING.md)）。

## 应用图标

`assets/images/genlot-app.icns` 已随仓库提供，CMake 在 `APPLE` 分支自动嵌入 Bundle。

图标由 `genlot-logo.svg` 的圆形标志重新排版而来（深蓝圆角底 + 白色双弧 + 渐变光球，遵循 Big Sur 的 1024 画布 / 824 本体规范）。修改配色或构图后重新生成：

```bash
cd ruoyi-vpn-client
python3 scripts/make_app_icon.py   # 需 Pillow + numpy
```

会同时刷新 `genlot-app.svg`（方形源）、`genlot-app-1024.png` 与 `genlot-app.icns`，无需 macOS 的 `iconutil`。

## Qt Creator

1. Kit 选 **Desktop Qt 6.6/6.7 macOS**（Clang / Xcode 14.2）。
2. 构建类型 **Release**。
3. CMake 初始参数可加：`-DCMAKE_OSX_ARCHITECTURES=arm64`（Intel 用 `x86_64`）；必要时 `-DProtobuf_ROOT=/opt/homebrew` 或 `/usr/local`。
4. 构建目录建议：`ruoyi-vpn-client/build-macos`。

## 架构说明

脚本可用环境变量指定架构；默认脚本里写的是 `arm64`，**Intel Monterey 务必设 `CMAKE_OSX_ARCHITECTURES=x86_64`**。

```bash
export CMAKE_OSX_ARCHITECTURES=x86_64   # Intel
# 或
export CMAKE_OSX_ARCHITECTURES=arm64    # Apple Silicon
bash bin/build-vpn-client-macos.sh
```

Universal 需同时具备双架构 Qt/Protobuf，另行评估。
