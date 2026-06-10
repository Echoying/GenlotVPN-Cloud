# GenlotVPN 桌面客户端 — Windows 部署与运维手册

面向**运维人员**与**最终用户**：如何在 Windows 上部署、配置、使用与排障 GenlotVPN 客户端。  
开发/编译说明见 [WINDOWS_SETUP.md](WINDOWS_SETUP.md)，TLS 证书与 Pin 详见 [TLS_PINNING.md](TLS_PINNING.md)。

---

## 1. 架构与端口

```
用户 PC
  GenlotVPN.exe
    ├─ 云端 TCP/TLS + Protobuf ──► ruoyi-vpn-auth :9443
    └─ 本地 HTTP ────────────────► 易安联 Agent :30303 (127.0.0.1)
```

| 方向 | 地址 | 协议 | 说明 |
|------|------|------|------|
| 出站 | `<服务器IP>:9443` | TLS 1.2+ / TCP | 登录、选线、钉钉验证、凭证下发 |
| 本机 | `127.0.0.1:30303` | HTTP | 控制器探测、登录、网关、应用列表 |
| 出站（可选） | 钉钉 / 线路 VPN 设备 | 依线路 | 由易安联 Agent 建立隧道，非本客户端直连 |

**服务端依赖**（由平台运维保障）：Nacos、Redis、`ruoyi-vpn-auth`（含 TCP 9443）、`ruoyi-modules-yianlian`（线路数据）。

---

## 2. 客户端交付物

### 2.1 获取安装包

当前为**绿色压缩目录**（尚未提供 NSIS 安装向导，见二期 W-03）：

1. 在构建机执行 `bin\package-vpn-client.bat`（需先 Release 构建）
2. 产物目录：`ruoyi-vpn-client\dist\GenlotVPN-win64\`
3. 将整个 `GenlotVPN-win64` 文件夹拷贝到目标机器（U 盘、共享盘、内网分发均可）

### 2.2 目录结构（分发包）

```
GenlotVPN-win64/
  GenlotVPN.exe          # 主程序
  config.default.json    # 首次运行模板（勿删）
  config.json            # 首次启动后自动生成，可手工编辑
  GenlotVPN/             # QML 模块（qmldir + 页面，勿删）
  Qt6*.dll               # Qt 运行时
  libprotobuf.dll        # 协议库
  zlib.dll               # Protobuf 依赖
  logs/                  # 运行后自动创建
```

> **注意**：必须保持 `GenlotVPN.exe` 与 `GenlotVPN\` 文件夹、`Qt6*.dll`、`libprotobuf.dll` 在同一目录，否则启动会失败或提示「界面加载失败」。

---

## 3. 部署前检查清单

| 项 | 要求 |
|----|------|
| 操作系统 | Windows 10 / 11 64 位 |
| 易安联 Agent | 已安装并运行，本机 `127.0.0.1:30303` 可访问 |
| 网络 | 能访问 VPN 云端服务器 **9443**（TLS） |
| 防火墙 | 允许 `GenlotVPN.exe` **出站** 连服务器 9443；本机 30303 一般无需入站规则 |
| 云端服务 | `ruoyi-vpn-auth` 已启用 TCP 9443；生产环境已配置 TLS |
| 账号 | 已在平台开通 VPN 用户与线路权限 |

### 3.1 防火墙（Windows Defender）

**出站规则（推荐）**

1. `wf.msc` → 出站规则 → 新建规则
2. 程序 → 浏览选择 `GenlotVPN.exe`
3. 允许连接 → 域/专用/公用按需勾选
4. 名称示例：`Genlot VPN 云端 9443`

或使用 PowerShell（管理员，路径按实际修改）：

```powershell
New-NetFirewallRule -DisplayName "Genlot VPN Outbound" `
  -Direction Outbound -Program "D:\Apps\GenlotVPN\GenlotVPN.exe" `
  -Action Allow -Profile Domain,Private
```

**说明**：客户端**不监听** 9443，只需出站。30303 为本地回环，通常不被防火墙拦截。

### 3.2 验证易安联 Agent

PowerShell：

```powershell
Invoke-WebRequest -Uri "http://127.0.0.1:30303/api/v1/version/current" -UseBasicParsing
```

返回 JSON 且 `code` 为 `200` 表示 Agent 正常。若连接失败，请先启动/重装易安联客户端（Agent）。

### 3.3 验证云端 9443

```powershell
Test-NetConnection -ComputerName <服务器IP> -Port 9443
```

`TcpTestSucceeded : True` 表示端口可达（TLS 握手在客户端日志中进一步确认）。

---

## 4. 配置 config.json

配置文件与 `GenlotVPN.exe` **同目录**。首次启动时，若不存在或无效，会从 `config.default.json` 自动生成。

### 4.1 生产环境（推荐）

```json
{
  "serverHost": "vpn.example.com",
  "serverPort": 9443,
  "useTls": true,
  "certPinSha256": "64位十六进制指纹",
  "certPinSha256Backup": ""
}
```

| 字段 | 说明 |
|------|------|
| `serverHost` | `ruoyi-vpn-auth` 对外 IP 或域名 |
| `serverPort` | 固定 **9443**（TCP，非 HTTP 8081） |
| `useTls` | 生产必须为 `true` |
| `certPinSha256` | 服务端证书 SPKI SHA-256，64 位 hex；启用 TLS 时必填 |
| `certPinSha256Backup` | 证书轮换时的备用指纹，可选 |

指纹获取：见 [TLS_PINNING.md](TLS_PINNING.md) 第二节，或查阅 `ruoyi-vpn-auth` 启动日志中的 `VPN TCP 证书 SPKI Pin`。

### 4.2 内网开发调试（明文 TCP）

```json
{
  "serverHost": "10.9.2.177",
  "serverPort": 9443,
  "useTls": false,
  "certPinSha256": ""
}
```

仅用于内网联调；**勿用于生产分发包**。

### 4.3 应用内修改

登录前可在 **设置 → 服务器 / 安全连接** 中修改地址、TLS 与指纹，保存后写入 `config.json` 并重新加载线路。

---

## 5. 使用流程

1. 双击 `GenlotVPN.exe`（仅允许**一个实例**；重复打开会提示并置前已有窗口）
2. **选择线路** → **登录**（图形验证码 + 账号密码）
3. **钉钉验证码**（6 位数字，发至 VPN 群）
4. 自动连接易安联控制器 → **应用列表 / 网关**
5. 关闭窗口或最小化 → **缩到系统托盘**；托盘右键「打开主窗口」/「退出」

记住密码：勾选后凭据经 **Windows DPAPI** 加密存于本机，换 Windows 用户无法读取。

---

## 6. 日志与排障

### 6.1 日志路径

| 文件 | 路径 | 内容 |
|------|------|------|
| 全量日志 | `<exe目录>\logs\genlot-vpn-YYYY-MM-DD.log` | INFO/WARN/ERROR |
| 报错日志 | `<exe目录>\logs\genlot-vpn-error-YYYY-MM-DD.log` | 仅 ERROR，便于运维收集 |

界面不展示日志，排障以文件为准。

### 6.2 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 双击闪退 / 界面加载失败 | 缺少 `GenlotVPN\` 或 Qt DLL | 重新完整拷贝 `GenlotVPN-win64` 目录 |
| 提示 QML debugging / 0xC0000005 | 使用了 Debug 构建包 | 换 **Release** 打包目录 |
| 证书 Pinning 校验失败 | 指纹与服务器证书不一致 | 核对 `certPinSha256` 或重新导出 Pin |
| TLS 握手失败 | 服务端未开 TLS 或仅 TLS1.3 | 确认 Nacos `vpn.tcp.tls`；客户端需 TLS 1.2+ |
| 无法连接 9443 | 防火墙/网络/服务未启 | `Test-NetConnection`；查 `ruoyi-vpn-auth` 与 9443 映射 |
| 控制器 30303 失败 | Agent 未启动 | 启动易安联 Agent；检查本机 30303 |
| 登录已过期 | 会话超时或空闲断开 | 重新登录；见服务端 TCP 空闲断开配置 |
| 网络闪断 / 连接已断开 | TCP 短暂中断 | 客户端自动重试最多 3 次（间隔 1.5s）；日志见 `[云端] 传输失败…重试` |
| 网关连接超时 | 隧道未就绪 | 确认 Agent、线路可达；查看网关轮询日志 |
| 已在运行中 | 单实例锁 | 托盘或任务管理器结束旧进程后再开 |

### 6.3 关键日志关键字

```
[云端]              # TCP/TLS/RPC
[控制器]            # 127.0.0.1:30303 HTTP
[TLS]               # 本机 TLS 能力
[单实例]            # 重复启动
[托盘]              # 系统托盘
会话已失效          # 需重新登录
切换网关失败        # 网关切换
```

---

## 7. 升级与分发

1. 通知用户退出托盘「退出」或结束进程
2. 备份旧目录中的 `config.json`（及用户自定义配置）
3. 用新包覆盖 `GenlotVPN-win64`（或仅替换 `GenlotVPN.exe`、DLL、`GenlotVPN\`）
4. 还原/合并 `config.json`
5. 抽检：启动 → 选线 → 登录 → 应用列表

版本号：

- 窗口标题：`Genlot VPN <主版本>`（如 `Genlot VPN 1.0.0`）
- 设置 → **关于**：完整标签 `1.0.0 (<构建标识>)`，构建标识为 Git 短提交或构建日期
- exe 属性 → **详细信息**：文件版本 / 产品版本（Windows `VERSIONINFO`）
- 主版本号在 `CMakeLists.txt` 的 `project(GenlotVPN VERSION x.y.z)` 修改；构建标识在编译时由 Git 自动生成

---

## 8. 安全要点（运维）

- 生产包 **`useTls: true`** 且配置正确 **Pin**，防止中间人篡改
- **不要**在工单/邮件中明文传播 `config.json` 中的生产 Pin（可内网 KB 维护）
- 记住密码为 DPAPI 保护，**不换机复制 profile** 到其他用户无效
- 云端 `session_key` 仅存内存，不落盘；会话过期需重新登录
- 控制器密码为服务端 AES 密文，客户端不解密，直接交 Agent

---

## 9. 相关文档

| 文档 | 用途 |
|------|------|
| [../README.md](../README.md) | 客户端总览 |
| [WINDOWS_SETUP.md](WINDOWS_SETUP.md) | 开发环境、编译打包 |
| [TLS_PINNING.md](TLS_PINNING.md) | 证书生成、Pin 导出、Nacos TLS |
| [PHASE2_CHECKLIST.md](PHASE2_CHECKLIST.md) | 二期任务进度 |
| [../../proto/README.md](../../proto/README.md) | TCP/Protobuf 协议 |
| [../../sql/update/vpn_tcp_nacos_example.yml](../../sql/update/vpn_tcp_nacos_example.yml) | TCP Nacos 配置示例 |

---

## 10. 运维联系信息（模板）

| 角色 | 负责范围 | 联系方式 |
|------|----------|----------|
| 平台运维 | Nacos、9443、Redis、`ruoyi-vpn-auth` | （填写） |
| 线路/VPN 设备 | 易安联线路、Agent 版本 | （填写） |
| 账号权限 | VPN 用户、线路授权 | （填写） |

*文档版本：与二期 W-10 同步，适用于 `GenlotVPN-win64` 绿色包部署。*
