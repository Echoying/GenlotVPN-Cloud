# GenlotVPN 桌面客户端 — macOS 开发环境

面向开发者：在 Mac 上编译 `GenlotVPN.app`。  
运维/分发见 [DEPLOY_MACOS.md](DEPLOY_MACOS.md)；Windows 见 [WINDOWS_SETUP.md](WINDOWS_SETUP.md)。

## 依赖

| 组件 | 说明 |
|------|------|
| Xcode CLT | Apple Clang（版本须匹配本机 macOS；Monterey 见下文） |
| CMake **3.21+** | 推荐官方 `.dmg`；或 `brew install cmake`（Monterey 上 brew 易源码编译，很慢） |
| Ninja | `brew install ninja`（构建脚本默认 `-G Ninja`） |
| Qt **6.6 / 6.7** macOS | Online Installer / MaintenanceTool 勾选 **macOS**；**Monterey 勿用 6.8+/6.11** |
| Protobuf | `brew install protobuf`（Intel：`/usr/local`，Apple Silicon：`/opt/homebrew`） |

检查（仓库根目录）：

```bash
export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"   # 按本机实际版本调整
export Protobuf_ROOT=/usr/local                    # Apple Silicon 改为 /opt/homebrew
bash bin/check-vpn-client-deps-macos.sh
```

## 快速编译（已装好依赖）

```bash
cd /path/to/GenlotVPN-Cloud

export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"
export CMAKE_OSX_ARCHITECTURES="$(uname -m)"   # Intel=x86_64，Apple Silicon=arm64
export Protobuf_ROOT=/usr/local                # 或 /opt/homebrew

bash bin/build-vpn-client-macos.sh
# 打包到 dist/：
# bash bin/build-vpn-client-macos.sh --package
```

产物：`ruoyi-vpn-client/build-macos/GenlotVPN.app`

```bash
open ruoyi-vpn-client/build-macos/GenlotVPN.app
```

## 清理后全量重编

```bash
cd /path/to/GenlotVPN-Cloud
rm -rf ruoyi-vpn-client/build-macos
# 可选：rm -rf ruoyi-vpn-client/dist

export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"
export CMAKE_OSX_ARCHITECTURES="$(uname -m)"
export Protobuf_ROOT=/usr/local   # 或 /opt/homebrew
bash bin/build-vpn-client-macos.sh
```

改过 `CMakeLists.txt` / Qt 版本 / 架构后建议先清理再建，避免残留缓存。

## macOS Monterey 12.7.x（重要）

Monterey 是构建**主机**时的上限约束（本仓库实测：Intel Mini + 12.7.6）：

| 组件 | Monterey 上建议版本 | 说明 |
|------|---------------------|------|
| Xcode / CLT | **14.2**（最高） | App Store 最新 Xcode 装不上；CLT 即可，不必完整 IDE |
| Qt | **6.6.x 或 6.7.x** | **6.8+ / 6.11 要求 macOS 13+**，会报 `qmlimportscanner cannot be run` |
| CMake | 官方 3.28～4.x `.dmg` | 勿强依赖 brew 源码编 cmake（可卡在 `bootstrap`/`make` 数十分钟） |
| Homebrew | Intel=`/usr/local`，ARM=`/opt/homebrew` | 过旧 brew 需先修好 tap，见下文 |

```bash
uname -m      # arm64 / x86_64
sw_vers       # ProductVersion
```

### Monterey 安装步骤（推荐顺序）

1. **安装编译器（CLT）**

   ```bash
   xcode-select --install
   xcode-select -p          # 期望 /Library/Developer/CommandLineTools
   clang --version
   ```

   > `xcode-select --install` 长时间不动：从 [Apple Developer Downloads](https://developer.apple.com/download/all/) 搜 **Command Line Tools for Xcode 14.2**，下载 `.dmg` 本地安装。

2. **安装 Homebrew**（若还没有）

   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

3. **安装 Ninja + Protobuf**（CMake 建议走官方包，见下一步）

   ```bash
   brew install ninja protobuf
   # 若 brew 仍想连带编译 cmake，可：
   # brew install ninja
   # brew install protobuf --ignore-dependencies
   ```

4. **安装 CMake（推荐官方包，避免 brew 源码编译）**

   - 打开 https://cmake.org/download/
   - 选 **macOS 12 or later → `cmake-*-macos-universal.dmg`**
   - 安装到「应用程序」后：

   ```bash
   sudo ln -sf /Applications/CMake.app/Contents/bin/cmake /usr/local/bin/cmake
   sudo ln -sf /Applications/CMake.app/Contents/bin/cpack /usr/local/bin/cpack
   sudo ln -sf /Applications/CMake.app/Contents/bin/ctest /usr/local/bin/ctest
   hash -r
   cmake --version    # 必须 ≥ 3.21（系统里若残留 3.14 旧链接会踩坑）
   ```

5. **安装 Qt 6.6 / 6.7（不要装 6.11）**

   官网下载页**不选版本**，版本在安装器组件树里勾选：

   - 首次： [Qt Online Installer](https://www.qt.io/download-qt-installer)（Open Source）
   - 已装过：打开 `~/Qt/MaintenanceTool.app` → **Add or remove components**
   - 若列表没有 6.7：Settings → Repositories → 勾选 **Archive**，刷新后再选 **Qt 6.7.x → macOS**
   - 安装后路径示例：`$HOME/Qt/6.7.3/macos`

   ```bash
   ls "$HOME"/Qt/6.7.*/macos/lib/cmake/Qt6/Qt6Config.cmake
   ```

6. **检查并构建**

   ```bash
   export CMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos"
   export CMAKE_OSX_ARCHITECTURES="$(uname -m)"
   export Protobuf_ROOT=/usr/local   # Apple Silicon: /opt/homebrew
   bash bin/check-vpn-client-deps-macos.sh
   bash bin/build-vpn-client-macos.sh
   ```

### Homebrew 常见故障（Monterey）

| 现象 | 处理 |
|------|------|
| `homebrew-core is a shallow clone` / `couldn't find remote ref refs/heads/master` | 旧 tap 仍追已删除的 `master`。执行：`brew untap homebrew/core && brew tap homebrew/core && brew update` |
| `undefined method 'compatibility_version'` | brew 本体过旧，先 `brew update` 再装公式 |
| `brew install cmake` 停在 `./bootstrap` / `make` | Monterey 无 bottle，在**源码编译**（可 30～90 分钟）。建议 Ctrl+C，改用官方 CMake `.dmg` |
| 本机 `cmake` 仍是 3.14 | 旧 `/Applications/CMake.app` 或旧软链；按上面步骤覆盖并 `hash -r` |

## 手动 CMake（与脚本等价）

```bash
cd ruoyi-vpn-client
cmake -B build-macos -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_PREFIX_PATH="$HOME/Qt/6.7.3/macos" \
  -DCMAKE_OSX_ARCHITECTURES="$(uname -m)" \
  -DProtobuf_ROOT=/usr/local
cmake --build build-macos
open build-macos/GenlotVPN.app
```

## 运行前注意

1. 本机安装并启动**易安联 macOS Agent**，确认 `curl http://127.0.0.1:30303/api/v1/version/current` 可用。
2. 可写配置与日志：`~/Library/Application Support/Genlot/GenlotVPN/`（`config.json`、`logs/`）。
3. 首次运行会从 `.app/Contents/Resources/config.default.json` 生成可写 `config.json`。
4. 生产环境请在设置页或 `config.json` 中启用 TLS 并填写 `certPinSha256`（见 [TLS_PINNING.md](TLS_PINNING.md)）。

## 应用图标

`assets/images/genlot-app.icns` 随仓库提供，CMake 在 `APPLE` 分支嵌入 Bundle；`Info.plist` 需含 `CFBundleIconFile=genlot-app`。

图标与 Windows `genlot-app.ico` / `genlot-app-icon-official.png` **同源对齐**（黑底 + 标志 + GENLOT）。修改官方 PNG 后重新生成：

```bash
cd ruoyi-vpn-client
swift scripts/make_app_icon.swift   # macOS 推荐（系统 iconutil，不依赖 Pillow）
# 或：python3 scripts/make_app_icon.py   # 需 Pillow + numpy
```

会刷新 `genlot-app-1024.png` 与 `genlot-app.icns`；Swift 脚本若检测到 `build-macos/GenlotVPN.app` 会自动同步 Resources。Finder 缓存时可：

```bash
touch build-macos/GenlotVPN.app
killall Finder
```

## Qt Creator

1. Kit 选 **Desktop Qt 6.6/6.7 macOS**（勿选 6.11 on Monterey）。
2. 构建类型 **Release**。
3. CMake 参数：`-DCMAKE_OSX_ARCHITECTURES=x86_64`（或 `arm64`）；`-DProtobuf_ROOT=/usr/local` 或 `/opt/homebrew`。
4. 构建目录建议：`ruoyi-vpn-client/build-macos`。

## 架构说明

构建脚本默认 `CMAKE_OSX_ARCHITECTURES=$(uname -m)`。也可显式指定：

```bash
export CMAKE_OSX_ARCHITECTURES=x86_64   # Intel
# 或
export CMAKE_OSX_ARCHITECTURES=arm64    # Apple Silicon
bash bin/build-vpn-client-macos.sh
```

Universal 需同时具备双架构 Qt/Protobuf，另行评估。

## 常见编译错误

| 错误 | 原因 | 处理 |
|------|------|------|
| `qmlimportscanner cannot be run … requires macOS 13` | 装了 Qt 6.11+ | 改装 6.7，改 `CMAKE_PREFIX_PATH`，清 `build-macos` 重编 |
| `unknown type name 'QTimer'` | 头文件缺前向声明 | 已修于 `TrustedTimeProvider.h`；拉最新代码 |
| `Error copying directory … GenlotVPN: File exists` | QML 模块目录与可执行文件同名冲突 | 已改为拷到 `Contents/Resources/GenlotVPN`；拉最新代码后重编 |
| Finder 无自定义图标 | `Info.plist` 缺 `CFBundleIconFile` 或 icns 非法 | 拉最新代码；`swift scripts/make_app_icon.swift` 后重编 |
| 点击「退出登录」闪退 | TCP 回调栈内 `clearSession`/`abort` | 已修；拉最新代码后重编验证 |
