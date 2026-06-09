# Windows 开发环境安装指南

GenlotVPN 桌面客户端需要 **Qt 6**、**Protobuf**、**CMake**、**MSVC**。按下面顺序安装即可。

## 一、必装组件一览

| 组件 | 用途 | 推荐版本 |
|------|------|----------|
| Visual Studio 2022 | C++ 编译器（MSVC） | Community 即可 |
| CMake | 构建系统 | 3.21+ |
| Qt 6 | UI 框架 | 6.6+（MSVC 64-bit） |
| Protobuf | 与后端共用协议 | 3.25+ |

---

## 二、Visual Studio 2022

1. 下载：https://visualstudio.microsoft.com/zh-hans/downloads/
2. 安装时勾选：**使用 C++ 的桌面开发**
3. 确保包含 **MSVC v143** 和 **Windows 10/11 SDK**

---

## 三、CMake

**方式 A — 官方安装包（推荐）**

1. 下载：https://cmake.org/download/ （Windows x64 Installer）
2. 安装时勾选 **Add CMake to the system PATH**

**方式 B — winget**

```powershell
winget install Kitware.CMake
```

验证：

```powershell
cmake --version
```

---

## 四、Qt 6

1. 下载 **Qt Online Installer**：https://www.qt.io/download-qt-installer
2. 登录/注册 Qt 账号（开源协议选 LGPL）
3. 选择安装：
   - **Qt 6.6.x** 或更高
   - 展开后勾选：**MSVC 2019 64-bit**（或 MSVC 2022 64-bit）
   - **Qt Quick**（默认已含在 Desktop 组件里）
   - 可选：**Qt Creator**（IDE，方便调试）
4. 记下安装路径，例如：
   ```
   C:\Qt\6.6.3\msvc2019_64
   ```

设置环境变量（PowerShell，当前会话）：

```powershell
$env:CMAKE_PREFIX_PATH = "C:\Qt\6.6.3\msvc2019_64"
```

永久设置（系统环境变量）：

- 变量名：`CMAKE_PREFIX_PATH`
- 值：`C:\Qt\6.6.3\msvc2019_64`

**安装后务必验证**（两个文件都要存在）：

```powershell
Test-Path "$env:CMAKE_PREFIX_PATH\lib\cmake\Qt6\Qt6Config.cmake"
Test-Path "$env:CMAKE_PREFIX_PATH\bin\Qt6Core.dll"
```

两者均为 `True` 才算装全。改环境变量后需**重新打开**终端或 IDE。

### 常见问题：路径对了仍报 Qt6 not found

若 `D:\apps\Qt\6.11.1\msvc2022_64` 目录存在，但只有 `Qt6WebEngine*.dll`、`config_qtwebengine.summary`，说明只装了 **WebEngine 附加组件**，没有 **Desktop Qt MSVC 主套件**。

解决步骤：

1. 打开 **Qt Maintenance Tool**（开始菜单或 `D:\apps\Qt\MaintenanceTool.exe`）
2. 选择 **添加或移除组件**
3. 展开 **Qt → Qt 6.11.1**，勾选：
   - **MSVC 2022 64-bit**（主组件，含 Core / Gui / Quick / Network / QML）
   - 不要只勾选 WebEngine 或自行源码编译的 WebEngine 目录
4. 安装完成后确认：
   ```
   D:\apps\Qt\6.11.1\msvc2022_64\lib\cmake\Qt6\Qt6Config.cmake
   D:\apps\Qt\6.11.1\msvc2022_64\bin\Qt6Core.dll
   ```
5. 重新打开终端，再运行 `bin\check-vpn-client-deps.bat`

本机若已装 **MinGW 64-bit**（`mingw_64` 有 `Qt6Config.cmake`），不能替代 MSVC 套件；GenlotVPN 客户端需与 Visual Studio 配套的 **msvc2022_64**。

---

## 五、Protobuf

### 方式 A — vcpkg（推荐，与 CMake 集成好）

```powershell
# 1. 安装 vcpkg（若尚未安装）
git clone https://github.com/microsoft/vcpkg.git C:\vcpkg
C:\vcpkg\bootstrap-vcpkg.bat

# 2. 安装 protobuf
C:\vcpkg\vcpkg install protobuf:x64-windows

# 3. 构建时传入 toolchain
cd d:\cursor\genlot\GenlotVPN-Cloud\ruoyi-vpn-client
cmake -B build `
  -DCMAKE_PREFIX_PATH=C:\Qt\6.6.3\msvc2019_64 `
  -DCMAKE_TOOLCHAIN_FILE=C:\vcpkg\scripts\buildsystems\vcpkg.cmake
cmake --build build --config Release
```

### 方式 B — 预编译包 + Chocolatey

```powershell
choco install protobuf
```

安装后确保 `protoc` 在 PATH 中：

```powershell
protoc --version
```

### 方式 C — 仅构建后端 Java 时

若**暂时只跑后端 TCP 服务**，可不装 Qt/Protobuf，只执行：

```powershell
cd d:\cursor\genlot\GenlotVPN-Cloud
mvn clean package -pl ruoyi-vpn-auth -am -DskipTests
```

桌面客户端可以等环境装好后再编译，或使用 CI 产物（见下文）。

---

## 六、一键检查依赖

```powershell
cd d:\cursor\genlot\GenlotVPN-Cloud
bin\check-vpn-client-deps.bat
```

## 七、构建与打包客户端

环境就绪后：

```powershell
$env:CMAKE_PREFIX_PATH = "D:\apps\Qt\6.11.1\msvc2022_64"   # 改成你的 Qt 路径
$env:Protobuf_ROOT = "D:\anaconda3\Library"                 # 若用 Anaconda protobuf
bin\check-vpn-client-deps.bat
bin\build-vpn-client.bat
```

**Release 产物：** `ruoyi-vpn-client\build-msvc2022\Release\GenlotVPN.exe`

**打包为可分发目录（windeployqt）：**

必须使用 **Release** 产物。若 Qt Creator 当前为 Debug，左下角切换为 Release 后重新构建。

```powershell
bin\package-vpn-client.bat
# 或一步：bin\build-vpn-client.bat --package
```

若干净机器启动闪退并出现 `QML debugging is enabled`，说明误打包了 Debug 版 exe。

输出：`ruoyi-vpn-client\dist\GenlotVPN-win64\`（含 `GenlotVPN.exe`、Qt 运行时 DLL、`libprotobuf.dll`、`config.json`）

将整个 `GenlotVPN-win64` 文件夹拷贝到未安装 Qt 的 Windows 电脑即可试运行。

---

## 八、没有本地编译环境时的替代方案

| 方案 | 说明 |
|------|------|
| **只测后端** | 用 Maven 启动 `ruoyi-vpn-auth`，TCP 9443；Web 端 `ruoyi-vpn-ui` 仍走 HTTP |
| **找一台已装 Qt 的机器** | 拷贝整个仓库，按上文构建后把 `GenlotVPN.exe` 和 Qt DLL 打包分发 |
| **CI 自动构建** | 在 GitHub Actions / 公司 Jenkins 上装 Qt+Protobuf 后编译（可向团队申请） |
| **Qt 在线安装器 + 离线包** | 内网可下载 Qt 离线安装包，避免外网限制 |

---

## 九、常见问题

**Q: CMake 找不到 Qt6**

- 确认 `CMAKE_PREFIX_PATH` 指向 Qt 的 **msvc2019_64** 目录（含 `lib\cmake\Qt6`）

**Q: CMake 找不到 Protobuf**

- 使用 vcpkg 的 `-DCMAKE_TOOLCHAIN_FILE=...\vcpkg.cmake`
- 或设置 `Protobuf_ROOT` 为 vcpkg 的 `installed\x64-windows`

**Q: 运行 exe 提示缺少 DLL**

```powershell
bin\package-vpn-client.bat
```

脚本会自动执行 `windeployqt --release --compiler-runtime`，并复制 `libprotobuf.dll` 及其依赖（如 Anaconda 的 `zlib.dll`）。

若仍提示缺某个 DLL，从 `Protobuf_ROOT\bin`（或 exe 同目录）手动补拷到 `dist\GenlotVPN-win64\`。

**Q: 磁盘空间**

- Qt 完整安装约 3–5 GB；VS + vcpkg protobuf 约 2–3 GB

---

## 十、最小可行路径（约 30 分钟）

1. 安装 VS 2022（C++ 桌面开发）— 10 min  
2. 安装 CMake — 2 min  
3. Qt Online Installer 只选 **Qt 6.6 MSVC 64-bit** — 15 min  
4. `vcpkg install protobuf:x64-windows` — 5 min  
5. `bin\check-vpn-client-deps.bat` → `bin\build-vpn-client.bat`

装好后告诉我，我可以帮你在本机执行构建并联调 TCP 9443。
