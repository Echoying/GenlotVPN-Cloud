# 线路密码过期自动更新 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `_subagent-driven-development` or an equivalent task-by-task execution flow. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 桌面端在线选线后，易安联控制器登录返回 2040 时，由云端自动轮换当前线路用户密码；成功提示用户重新登录并回到登录页。

**Architecture:** 新增 `ROTATE_LINE_PASSWORD` Protobuf RPC。vpn-auth 从已认证会话取得 `localUserId` 并校验线路授权，Feign 调 yianlian；yianlian 用独立生成器生成 8 位强密码，复用 `resetPwdWithSync` 原子更新本地线路用户和易安联。客户端为 2040 增加专用信号和最多两次的轮换状态机，成功后清理会话并回登录页。

**Tech Stack:** Java 8、Spring Boot/Feign、Protobuf 3、Qt 6/C++、Maven、CMake。

## Global Constraints

- 只改当前线路 `vpn_user`，不改 `vpn_local_user`。
- 密码固定 8 位，至少包含大写、小写、数字、`!@#$%^&*` 各一位。
- 新旧密码明文不得写日志、响应、文档或门禁。
- `RESET_PASSWORD` 不进入补偿队列。
- 离线模式不调用云端轮换。
- 成功后不重新拉凭证、不静默重登；提示「密码已经更新，需要退出重新登录」并回登录页。
- 同一轮连线最多发起两次轮换 RPC。
- 不改 TLS、证书 Pin、帧格式、管理端页面和 PHASE2 清单。
- 仓库已有未提交改动；本计划不创建 Git 提交。

---

## 文件结构

**新增：**

- `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/LinePasswordGenerator.java`：只负责安全生成满足规则的密码。
- `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/LinePasswordRotationService.java`：授权后的线路用户查找、旧密解密、生成与同步改密。
- `ruoyi-modules/ruoyi-yianlian/src/test/java/com/ruoyi/yianlian/service/vpn/LinePasswordGeneratorTest.java`：生成规则单元测试。

**修改：**

- `proto/vpn/envelope.proto`、`proto/vpn/line.proto`：RPC 类型与消息。
- `ruoyi-api/ruoyi-api-yianlian/.../RemoteVpnLocalUserService.java`：新增 Inner Feign 契约。
- `ruoyi-api/ruoyi-api-yianlian/.../RemoteVpnLocalUserFallbackFactory.java`：新增降级实现。
- `ruoyi-modules/ruoyi-yianlian/.../VpnLocalUserController.java`：新增 Inner 入口。
- `ruoyi-vpn-auth/.../VpnLoginService.java`：封装授权校验和 Feign 调用。
- `ruoyi-vpn-auth/.../TcpRpcDispatcher.java`：解析并处理新 RPC。
- `ruoyi-vpn-client/src/core/VpnCloudService.{h,cpp}`：发送轮换 RPC并发专用成功/失败信号。
- `ruoyi-vpn-client/src/core/ControllerService.{h,cpp}`：把控制器 2040 从通用错误中分流。
- `ruoyi-vpn-client/src/core/VpnFlowController.{h,cpp}`：两次轮换状态与成功回登录清理。
- `pipeline/work/2026-09-14-线路密码过期自动更新/质量门禁.md`：记录 gate/deploy 结果。

---

### Task 1: Protobuf 轮换契约

**Files:**
- Modify: `proto/vpn/envelope.proto`
- Modify: `proto/vpn/line.proto`

**Interfaces:**
- Produces: `MessageType.ROTATE_LINE_PASSWORD = 21`
- Produces: `RotateLinePasswordRequest { string app_id = 1; }`
- Produces: `RotateLinePasswordResponse {}`

- [ ] **Step 1: 增加消息定义**

在 `GET_USER_CREDENTIALS` 邻近增加请求/响应；在枚举末尾使用未占用编号 21，不改已有编号。

- [ ] **Step 2: 验证 Java 协议生成**

Run:

```powershell
mvn clean package -pl ruoyi-vpn-protocol -am -DskipTests
```

Expected: `BUILD SUCCESS`，生成 `RotateLinePasswordRequest`、`RotateLinePasswordResponse`，`MessageType` 含编号 21。

- [ ] **Step 3: 验证 Qt 协议可生成**

由最终 `python -u pipeline/bin/gate.py --run 2026-09-14-线路密码过期自动更新` 统一执行；本任务不提交 `target/` 或客户端 build 生成物。

---

### Task 2: yianlian 线路密码轮换

**Files:**
- Create: `.../service/vpn/LinePasswordGenerator.java`
- Create: `.../service/vpn/LinePasswordRotationService.java`
- Create: `.../src/test/.../LinePasswordGeneratorTest.java`
- Modify: `.../api/RemoteVpnLocalUserService.java`
- Modify: `.../api/factory/RemoteVpnLocalUserFallbackFactory.java`
- Modify: `.../controller/VpnLocalUserController.java`

**Interfaces:**
- Produces: `String LinePasswordGenerator.generate()`
- Produces: `void LinePasswordRotationService.rotate(Long localUserId, String appId)`
- Produces: `R<Boolean> RemoteVpnLocalUserService.rotateLinePassword(Long localUserId, String appId, String source)`
- Consumes: `VpnUserMapper.selectUserByLocalUserIdAndAppId`
- Consumes: `IVpnUserService.resetPwdWithSync(VpnUser user, String plainPassword)`

- [ ] **Step 1: 先写生成器失败测试**

测试循环生成 1000 次，每次断言长度为 8，并分别匹配 `[A-Z]`、`[a-z]`、`[0-9]`、`[!@#$%^&*]`，且所有字符只来自允许字符集。

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
mvn test -pl ruoyi-modules/ruoyi-yianlian -am -Dtest=LinePasswordGeneratorTest
```

Expected: FAIL，原因是 `LinePasswordGenerator` 尚不存在。

- [ ] **Step 3: 实现安全生成器**

使用 `SecureRandom`：四类字符各选一个，其余四位从合并字符集中选取，再用 Fisher-Yates 洗牌。禁止使用 `Random`、时间戳或 UUID 截断。

- [ ] **Step 4: 运行生成器测试**

重复 Step 2 命令。Expected: PASS。

- [ ] **Step 5: 实现轮换服务**

`rotate(localUserId, appId)` 顺序：

1. 参数非空，否则抛 `ServiceException`。
2. `IVpnLocalUserService.isAuthorizedForLine` 校验授权。
3. 按 `localUserId + appId` 读取有效线路用户；不存在、停用、删除均按现有授权语义返回「您无权访问所选线路，请联系管理员」。
4. AES 解密 `encryptedPwd`；为空或异常使用 spec 固定文案。
5. 最多生成 8 次，排除与旧密码相同；仍碰撞则固定失败文案。
6. 构造仅含必要字段的 `VpnUser`，调用 `resetPwdWithSync`。
7. 方法与被调方法保持事务边界，异常向上抛出，禁止日志打印密码。

- [ ] **Step 6: 暴露 Inner 接口**

在 `VpnLocalUserController` 增加 `@InnerAuth @PutMapping("/rotate-line-password")`，参数采用 `@RequestParam Long localUserId`、`@RequestParam String appId`；Feign 与 fallback 签名完全一致，成功返回 `R.ok(true)`。

- [ ] **Step 7: 编译并运行模块测试**

Run:

```powershell
mvn clean test -pl ruoyi-modules/ruoyi-yianlian -am -DskipTests=false
```

Expected: `BUILD SUCCESS`。

---

### Task 3: vpn-auth 鉴权 RPC

**Files:**
- Modify: `ruoyi-vpn-auth/src/main/java/com/ruoyi/vpn/auth/service/VpnLoginService.java`
- Modify: `ruoyi-vpn-auth/src/main/java/com/ruoyi/vpn/auth/tcp/TcpRpcDispatcher.java`

**Interfaces:**
- Produces: `void VpnLoginService.rotateLinePassword(Long localUserId, String appId)`
- Consumes: `RemoteVpnLocalUserService.rotateLinePassword(...)`
- Consumes: `RotateLinePasswordRequest` / `RotateLinePasswordResponse`

- [ ] **Step 1: 增加服务方法**

校验 `localUserId`、`appId`，先调用现有 `assertLocalUserAuthorizedForLine`，再调 Feign Inner。Feign 失败时用返回消息抛 `ServiceException`。

- [ ] **Step 2: 增加 Dispatcher 分支**

在 `switch` 增加 `ROTATE_LINE_PASSWORD`；handler 必须先 `requireAuth(session, envelope)`，再 `resolveUserId`，解析 `app_id` 后调用服务并返回空成功响应。禁止调用 `vpnLineVerifyService.consumePassed`。

- [ ] **Step 3: 验证 Java 全链路编译**

Run:

```powershell
mvn clean package -pl ruoyi-vpn-auth -am -DskipTests
```

Expected: `BUILD SUCCESS`。

---

### Task 4: Qt 客户端 2040 状态机

**Files:**
- Modify: `ruoyi-vpn-client/src/core/ControllerService.h`
- Modify: `ruoyi-vpn-client/src/core/ControllerService.cpp`
- Modify: `ruoyi-vpn-client/src/core/VpnCloudService.h`
- Modify: `ruoyi-vpn-client/src/core/VpnCloudService.cpp`
- Modify: `ruoyi-vpn-client/src/core/VpnFlowController.h`
- Modify: `ruoyi-vpn-client/src/core/VpnFlowController.cpp`

**Interfaces:**
- Produces: `ControllerService::loginPasswordExpired(const QString &message)`
- Produces: `VpnCloudService::rotateLinePassword(const QString &appId)`
- Produces: `linePasswordRotationSucceeded()` / `linePasswordRotationFailed(const QString &message)`

- [ ] **Step 1: 让控制器请求支持专用业务错误**

为 `postJson` 增加可选错误回调（或新增仅供登录使用的等价私有方法）。`loginWithAccount` 遇 `code=="2040"` 发 `loginPasswordExpired`，其它非 200 保持 `operationFailed`，成功行为不变。

- [ ] **Step 2: 增加云端轮换调用**

序列化 `RotateLinePasswordRequest.app_id`，发送消息 21。业务失败和传输最终失败均发 `linePasswordRotationFailed`，不要落到通用 `requestFailed`，避免被全局失败处理提前终止重试。

- [ ] **Step 3: 增加 Flow 状态**

新增：

```cpp
int m_linePasswordRotationAttempts = 0;
QString m_linePasswordExpiredMessage;
bool m_linePasswordRotationPending = false;
```

开始新的控制器连线链时重置；退出、会话失效、成功连线时也重置。

- [ ] **Step 4: 实现最多两次轮换**

收到 2040：

- 离线：提示原文并结束。
- 在线：保存原文，从 `m_verifyLine` 或 `m_pendingLine` 取 `appId`。
- attempts 小于 2 时置 pending、递增并发送 RPC。
- 失败且 attempts 小于 2：直接重发；否则按原控制器失败路径记录/上报并 toast 过期原文。

- [ ] **Step 5: 实现成功回登录**

轮换成功后清 pending；停止轮询；必要时控制器 logout；清云端 session；调用 `finishLogout(false)` 保留用户名；toast **密码已经更新，需要退出重新登录**；刷新验证码。不得调用 `fetchUserCredentials` 或 `loginWithAccount`。

- [ ] **Step 6: 编译并打包客户端**

由最终 gate 调用 Windows 客户端构建。Expected: CMake/Protobuf/C++ 编译成功并生成安装/分发产物；不运行 QTest，不声称客户端已验收。

---

### Task 5: 门禁、部署与交接

**Files:**
- Create/Update: `pipeline/work/2026-09-14-线路密码过期自动更新/质量门禁.md`

- [ ] **Step 1: 检查敏感信息与改动范围**

确认 diff 不含密码、token、`session_key`、生成文件和无关重构。

- [ ] **Step 2: 执行总门禁**

Run:

```powershell
python -u pipeline/bin/gate.py --run 2026-09-14-线路密码过期自动更新
```

Expected: Java 全量、管理端前端、客户端编译打包均成功；结果写入当次 `质量门禁.md`。

- [ ] **Step 3: 部署管理服务**

Run:

```powershell
python -u pipeline/bin/deploy.py --run 2026-09-14-线路密码过期自动更新
```

Expected: yianlian 等 92 侧服务部署成功并落门禁记录。

- [ ] **Step 4: 部署 vpn-auth**

Run:

```powershell
python -u pipeline/bin/deploy.py vpn --run 2026-09-14-线路密码过期自动更新
```

Expected: 93 vpn-auth 部署成功并落门禁记录。

- [ ] **Step 5: 交给需求方人工自验**

不跑页面 accept，不跑 `client-ref`。明确交接 spec 7.4 的五步人工验证；最终状态只能写「构建/部署完成，客户端待需求方自验」，不能写「客户端已验收」。


---

## 回修 R1：轮换超时被判失败（2026-09-14 人工自验发现）

**现象**：桌面端遇 2040 后没有自动退出回登录页。

**根因**（92/93 容器日志）：13:40:08 第 1 次轮换，易安联 13:40:22 返回 200 实际成功（耗时约 14s），但 vpn-auth→yianlian 的 Feign 默认 `readTimeout: 10000` 在 13:40:18 先超时，被降级工厂判成失败；客户端按 spec 重试第 2 次，此时易安联侧已是新密码，第 2 次拿旧密码去改，易安联返回 `400 原密码不正确`，本地事务回滚。两次都"失败"，客户端走失败分支，故不退出。

**修复**（方案 A，已获批）：

1. 轮换拆到独立 Feign 客户端 `RemoteVpnLinePasswordService`（contextId `remoteVpnLinePasswordService`），Nacos `application-dev.yml` 配 `readTimeout: 60000`；`RemoteVpnLocalUserService` 及其降级工厂移除该方法，登录查询仍用 10s。
2. yianlian 新增 `LinePasswordRotationGuard`：按 `(localUserId, appId)` 的 Redis 互斥锁（TTL 120s，等锁最长 45s）+ 成功标记（TTL 120s）。`LinePasswordRotationService.rotate` 先判标记再抢锁，锁内二次判标记，成功后记标记；等锁超时报"线路密码正在更新，请稍后重试"。
3. 客户端流程不变（仍最多 2 次），仅成功提示文案改为"密码已经更新，需要退出重新登录"。
4. 配置落盘：`sql/update/feign_line_password.yml`、`sql/ry-config.sql`、`docker/node-91/mysql/db/ry-config.sql`；线上 Nacos 已发布并回读校验。
