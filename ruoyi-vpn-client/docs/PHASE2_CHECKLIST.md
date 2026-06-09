# GenlotVPN 桌面客户端 — 第二期任务清单

> 首期目标：Windows + TCP/Protobuf 五步法 MVP（选线 → 登录 → 钉钉验证 → 控制器 → 应用列表）  
> 二期目标：生产级安全、正式交付、跨平台与体验完善  
> 更新日期：2026-06-09

---

## 使用说明

- 状态：`[ ]` 未开始 · `[~]` 进行中 · `[x]` 已完成 · `[-]` 不做/延期
- 优先级：**P0** 阻塞外网/上线 · **P1** 正式交付必备 · **P2** 体验与跨平台 · **P3** 锦上添花
- 负责人栏留空，实施时自行填写

---

## P0 — 生产安全（外网上线前置）

| 状态 | ID | 任务 | 范围 | 验收标准 |
|------|-----|------|------|----------|
| [x] | S-01 | 启用 TLS 1.3 | 后端 `ruoyi-vpn-auth` Nacos：`vpn.tcp.tls.enabled=true`，配置 `cert-path` / `key-path` | Wireshark 抓包 9443 仅为 TLS 密文；禁用明文 TCP |
| [x] | S-02 | 客户端强制 TLS（生产） | `config.json` / 设置页：`useTls=true`；开发环境可保留 false | 生产包默认 `useTls: true`，连不上明文端口 |
| [x] | S-03 | 证书 Pinning 配置 | 后端导出 SPKI SHA-256；客户端 `certPinSha256` | Pin 错误证书时客户端拒绝连接并写 error 日志 |
| [x] | S-04 | 设置页：TLS + Pin | `SettingsPage.qml` 新增「安全连接」或扩展现有「服务器」 | 可编辑 `useTls`、`certPinSha256` 并写入 `config.json` |
| [x] | S-05 | 连接/IP 限流 | 后端 TCP：`VpnTcpConnectionLimitHandler` + `VpnTcpRateLimitService` | 单 IP 连接数、Login 频率超限被拒绝；有日志 |
| [x] | S-06 | 空闲连接断开 | 后端 TCP：`IdleStateHandler` + `VpnTcpIdleDisconnectHandler` | 长时间挂机后客户端重连或提示会话失效 |
| [x] | S-07 | 生产 Nacos 模板 | `sql/update/ruoyi-vpn-auth-dev.yml` 补充 TLS 生产示例 | 运维可按文档一键切换 dev/prod |
| [x] | S-08 | Docker 9443 + TLS | `docker-compose` 证书挂载、端口映射 | 容器部署桌面端可 TLS 连 9443 |
| [x] | S-09 | 外网全链路联调 | 客户端 + `ruoyi-vpn-auth` + Redis + Agent | 完整 5 步在 TLS 环境下跑通并记录联调报告 |

---

## P1 — Windows 正式交付

| 状态 | ID | 任务 | 范围 | 验收标准 |
|------|-----|------|------|----------|
| [x] | W-01 | Release 构建固化 | `bin/build-vpn-client.bat`、CMake Release 说明 | 新同事按 README 可产出 Release exe |
| [x] | W-02 | windeployqt 依赖打包 | `bin/package-vpn-client.bat` 拷贝 Qt/Protobuf DLL | 干净 Windows 机器双击 exe 可启动 |
| [ ] | W-03 | NSIS 安装包 | 安装向导、卸载、开始菜单快捷方式 | 生成 `.exe` 安装包，安装后可用 |
| [x] | W-04 | 默认 config.json | 安装目录释放默认配置或首次运行生成 | 首次启动能连默认服务器 |
| [x] | W-05 | DPAPI 记住密码 | `SecureStorage`：Windows `CryptProtectData` | 记住密码不以明文存 QSettings；换用户读不出 |
| [ ] | W-06 | Token / 会话安全存储 | 评估是否将 session 相关敏感项迁入 Credential Manager | 明文密码、session_key 不落盘 |
| [x] | W-07 | 单实例锁 | 启动时检测已有进程 | 重复打开提示或聚焦已有窗口 |
| [x] | W-08 | 系统托盘 | 最小化到托盘、右键退出/打开 | 关闭窗口可最小化到托盘而非直接退出 |
| [ ] | W-09 | 应用图标与版本号 | exe 属性、关于页或标题显示版本 | 版本与 git tag / 构建号一致 |
| [x] | W-10 | 安装与运维文档 | `docs/DEPLOY_WINDOWS.md` | 含防火墙 9443、Agent 30303、日志路径 |

---

## P1 — 客户端功能补齐（对齐 Web）

| 状态 | ID | 任务 | 范围 | 验收标准 |
|------|-----|------|------|----------|
| [x] | C-01 | 网关列表轮询 | 参考 `ruoyi-vpn-ui` `pollGatewayList` | 连接控制器后网关 IP 自动刷新至就绪 |
| [x] | C-02 | 网关切换体验 | `AppListPage` 切换网关反馈 | 切换中有 loading，失败有 toast + error 日志 |
| [ ] | C-03 | 改密流程回归 | `LoginPage` 改密弹窗 | 改密成功/失败与 Web 行为一致 |
| [x] | C-04 | 会话过期统一处理 | `VpnFlowController` 各 RPC 失败分支 | 过期回登录页，状态清理干净 |
| [x] | C-05 | 证书 Pin 备用指纹 | `CertificatePinner` 支持主 Pin + 备用 Pin | 证书轮换窗口期新旧 Pin 均可连 |
| [ ] | C-06 | 自动重连（可选） | TCP 断线后有限次重试 | 网络闪断可恢复，不无限循环 |

---

## P2 — mTLS（企业设备认证）

| 状态 | ID | 任务 | 范围 | 验收标准 |
|------|-----|------|------|----------|
| [ ] | M-01 | 私有 CA 与证书规范 | 运维文档：CA、server、client 证书签发流程 | 有标准 PEM 格式与有效期策略 |
| [ ] | M-02 | Netty 开启客户端认证 | `vpn.tcp.tls.client-auth: need` | 无客户端证书无法建立 TCP |
| [ ] | M-03 | Qt 加载客户端证书 | `TcpClient` + `QSslSocket` 本地 cert/key | 握手双向成功 |
| [ ] | M-04 | 设备证书下发方案 | MDM / 安装包内置 / 首次激活 | 明确企业内如何发放 `client.crt` |
| [ ] | M-05 | mTLS 与账密双因素 | 服务端校验证书 CN/OU + 用户登录 | 吊销设备证书后该设备无法登录 |
| [ ] | M-06 | mTLS 联调与回滚 | Nacos 可切换 `none` / `want` / `need` | 出问题可快速退回单向 TLS |

---

## P2 — macOS 跨平台

| 状态 | ID | 任务 | 范围 | 验收标准 |
|------|-----|------|------|----------|
| [ ] | Mac-00 | **前置确认** | 向易安联确认 macOS Agent（30303） | 书面结论：有/无 Mac Agent |
| [ ] | Mac-01 | CMake macOS 构建 | Qt 6 + Clang，arm64/x86_64 | `cmake --build` 产出 `.app` |
| [ ] | Mac-02 | Keychain 安全存储 | `ISecureStorage` macOS 实现 | 记住密码进 Keychain，与 Windows API 隔离 |
| [ ] | Mac-03 | 平台抽象层收尾 | 托盘/单实例/路径差异 | Core 层无 `#ifdef _WIN32` 散落 |
| [ ] | Mac-04 | 代码签名 | Apple Developer 证书签名 `.app` | Gatekeeper 可打开（开发证书至少本机可用） |
| [ ] | Mac-05 | 公证（Notarization） | `notarytool` 提交 | 外部分发 Mac 包不被拦截 |
| [ ] | Mac-06 | `.dmg` 打包 | 拖拽安装镜像 | 用户可标准方式安装 |
| [ ] | Mac-07 | macOS 联调文档 | `docs/DEPLOY_MACOS.md` | 含 Agent、防火墙、日志路径 |

> **说明**：若无 Mac Agent，Mac-01～07 仍可做「云端登录客户端」，但步骤 4～5（控制器/隧道）需标注为不支持或仅 Windows。

---

## P3 — 工程与运维增强

| 状态 | ID | 任务 | 范围 | 验收标准 |
|------|-----|------|------|----------|
| [ ] | E-01 | Protobuf CI 校验 | 根 `pom.xml` 或 GitHub Action：`buf lint` / 生成 Java+C++ | `.proto` 变更破坏兼容时 CI 失败 |
| [ ] | E-02 | `vpn-tcp-cli` 调试工具 | 小型 Java 或 C++ CLI | 无需 GUI 可测 10 个 RPC |
| [ ] | E-03 | 自动更新 | 检查版本 URL + 下载安装包（Windows 优先） | 有新版本时提示升级 |
| [ ] | E-04 | 崩溃/错误上报（可选） | 本地 error 日志聚合或上报接口 | 运维可收集现场 error 文件 |
| [ ] | E-05 | Qt LGPL 合规 | 动态链接说明、`LICENSES` 目录 | 法务审核通过 |

---

## 明确不在二期范围

| 项 | 原因 |
|----|------|
| Web 端改 TCP/Protobuf | 计划约定 Web 继续 HTTP |
| 本地 30303 改 Protobuf | 易安联厂商 API，不可改 |
| 引入 gRPC | 用户要求纯 TCP 帧，非 HTTP/2 |
| 替换易安联 Agent | 厂商依赖，非本仓库范围 |

---

## 建议实施顺序（里程碑）

### 里程碑 1：外网可上线（约 2～3 周）
```
S-01 → S-02 → S-03 → S-04 → S-07 → S-08 → S-09
并行：S-05、S-06
```

### 里程碑 2：Windows 正式包（约 1～2 周）
```
W-01 → W-02 → W-03 → W-04 → W-05 → W-10
并行：W-07、W-08、C-01
```

### 里程碑 3：企业安全增强（约 2 周，可与 Mac 并行）
```
M-01 → M-02 → M-03 → M-05 → M-06
并行：C-05
```

### 里程碑 4：macOS（约 2～3 周，依赖 Mac-00 结论）
```
Mac-00 → Mac-01 → Mac-02 → Mac-03 → Mac-04 → Mac-05 → Mac-06 → Mac-07
```

### 里程碑 5：锦上添花
```
E-01、E-02、E-03、W-09、C-06
```

---

## 任务统计

| 优先级 | 任务数 | 已完成 |
|--------|--------|--------|
| P0 生产安全 | 9 | **9/9** |
| P1 Windows 交付 | 10 | **8/10** |
| P1 功能补齐 | 6 |
| P2 mTLS | 6 |
| P2 macOS | 8 |
| P3 工程增强 | 5 |
| **合计** | **44** |

---

## 相关文档

- 客户端 README：`../README.md`
- Windows 环境：`WINDOWS_SETUP.md`
- Windows 部署运维：`DEPLOY_WINDOWS.md`
- TCP Nacos 示例：`../../sql/update/vpn_tcp_nacos_example.yml`
- 协议说明：`../../proto/README.md`
- 实施计划（Cursor Plan）：`VPN Qt 跨平台客户端`
