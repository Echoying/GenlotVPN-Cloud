# SPEC：线路密码过期自动更新

| 项 | 内容 |
|----|------|
| 日期 | 2026-09-14 |
| 状态 | **spec 已批准**（2026-09-14「可以」）；实施计划见 [plan.md](./plan.md) |
| 安全级 | M1/M2=S1（人审服务端；客户端需求方自验；不改 TLS）。级别由人定，AI 不得自行降级 |
| 模块 | M1 / M2 |
| 熟悉度 | 熟悉（对照 `GET_USER_CREDENTIALS`、`resetPwdWithSync`、控制器 `loginWithAccount`） |

人批准本文件后才写业务代码。实现后按第 7 节跑 `gate` → `deploy`（含 vpn）；**无新管理端页，不跑页面 `accept.py`**。客户端由需求方 **人工自验**，不跑 `client-ref`，AI 不得声称客户端已验收。

对照：[模块拆分清单.md](./模块拆分清单.md)。方案 A：新 RPC `ROTATE_LINE_PASSWORD`。

---

## 1. 背景与目标

易安联控制器 `POST /api/v1/user/loginWithAccount` 在线路密码过期时返回 `code=2040`，`messages` 为「当前密码已过期，请修改密码后重新登录！」。现状：客户端把所有非 `200` 当失败 toast，用户无法自助恢复。

目标：所有桌面用户（不限 `sync_proxy`）在**在线**连线遇到 `2040` 时，由云端为**当前线路用户**生成符合固定策略的新密码，写入本地线路用户并同步易安联；然后提示用户重新登录并回到登录页。下次选线用 `GET_USER_CREDENTIALS` 取新密文再登控制器。

---

## 2. 范围

**做：**

- proto：`MessageType.ROTATE_LINE_PASSWORD = 21`；请求仅 `app_id`。
- vpn-auth：须已登录；校验该本地用户对 `app_id` 有授权；Feign Inner 调 yianlian；**不**调用 `consumePassed`。
- yianlian：按 `localUserId + appId` 找 `vpn_user`，生成 8 位强密码，走现有 `resetPwdWithSync`（易安联改密用库内可解密的旧线路密码）。
- 桌面端：仅 `loginWithAccount` 响应 `code==2040` 触发；成功 toast 固定文案并回登录页；同一轮连线轮换 RPC 最多 2 次。

**不做：**

- 不改 `vpn_local_user` 登录密码；不改登录页「修改密码」语义。
- 不静默用新凭证再 `loginWithAccount`。
- 不把 2040 的 `passwordLength` / `stongLevel` 传给云端。
- 不入同步补偿队列（`RESET_PASSWORD` 仍不可延迟）。
- 无新管理端菜单/页面；无定时 20 天轮换。
- 不改 TLS / 证书 Pin / 帧格式。
- 离线模式不调本 RPC。

---

## 3. 行为

### 3.1 云端轮换（M1）

时机：桌面端已完成云端登录与选线验证，控制器 `loginWithAccount` 返回 2040 之后。

`TcpRpcDispatcher` 处理 `ROTATE_LINE_PASSWORD`：

| 条件 | 结果 |
|------|------|
| 未登录 / 会话失效 | 失败，现有未登录文案 |
| `app_id` 空 | 失败 |
| 本地用户对该线路无授权，或线路用户不存在 / 停用 / 删除 | **您无权访问所选线路，请联系管理员** |
| 旧线路密码无法解密 | **无法获取线路用户密码，请在线路用户管理中重置密码** |
| 易安联同步改密失败 | `resetPwdWithSync` 事务回滚，返回失败原因；本地线路密码保持旧值 |
| 成功 | `RpcResponse.code=200`；**不回传新明文/密文**（下次走 `GET_USER_CREDENTIALS`） |

新密码规则（服务端固定，不读 2040 字段）：

- 长度 **8**
- 至少各 1 个：大写 `A-Z`、小写 `a-z`、数字 `0-9`、特殊字符 `!@#$%^&*`
- 不得与旧线路明文密码相同；碰撞则重生成，最多 8 次，仍相同则失败 **生成线路密码失败，请重试**
- 明文不得写入日志、审计正文、对话、质量门禁

不调用 `VpnLineVerifyService.consumePassed`（选线通过标记在首次拉凭证时已消费）。

### 3.2 桌面端（M2）

`ControllerService`：`loginWithAccount` 在 `code != 200` 时，若 `code == "2040"` 发独立信号（例如 `loginPasswordExpired`），把 `messages` 带出；其它错误仍走 `operationFailed`。

`VpnFlowController`（在线、连线链路上）：

1. 收到过期信号：若本轮轮换次数 `< 2`，调 `ROTATE_LINE_PASSWORD(当前 appId)`，次数 +1。
2. RPC 成功：toast **密码已经更新，需要退出重新登录**（非 error 样式）；执行与会话失效类似的清理（控制器 logout、清云端会话、`finishLogout`），`navigateTo("login")`，刷新验证码。账号输入框可保留用户名，不强制清空。
3. RPC 失败且次数仍 `< 2`：再调一次轮换。
4. 两次都失败或次数用尽仍失败：toast 控制器原文（优先 `messages`，空则「当前密码已过期，请修改密码后重新登录！」），**不**回登录页，结束本轮连线（与现有控制器失败一致：error 日志 + 可上报 connect 失败）。
5. 离线模式 2040：不调云端，按第 4 条提示原文。

成功路径 **禁止**：再次 `GET_USER_CREDENTIALS`、再次 `loginWithAccount`。

日志可写「线路密码已过期，已请求云端更新」；**禁止**写新/旧密码。

### 3.3 客户端验收（M2）

**不**改 `PHASE2_CHECKLIST.md`，**不**跑 `accept.py client-ref`。造 2040、看 toast、回登录页、再用原本地密码连上，全部由需求方在本机人工完成。AI 只保证能编包、部署 vpn-auth/yianlian。

---

## 4. 数据与接口

无新表。Nacos `application-dev.yml` 增加一项 Feign 超时（见 4.2）。

### 4.1 proto

`envelope.proto` 增加：

```text
ROTATE_LINE_PASSWORD = 21;
```

`line.proto`（或同文件邻近凭证消息）：

```text
message RotateLinePasswordRequest {
  string app_id = 1;
}
message RotateLinePasswordResponse {}
```

### 4.2 Feign / yianlian

内部接口（建议挂本地用户 Inner，与 `line-credentials` 并列）：

- `PUT /vpn/local/user/rotate-line-password`
- 参数：`localUserId`、`appId`（query 或 body 二选一，实现与现有 Inner 风格一致）
- 返回：`R.ok(true)` / `R.fail(msg)`
- 实现：授权校验 → 幂等标记判定 → 抢锁 → 查线路用户 → 生成密码 → `IVpnUserService.resetPwdWithSync` → 记幂等标记

vpn-auth 用当前会话 `userId` 作为 `localUserId`，客户端不得指定他人 ID。

**超时与幂等**（易安联改密含代理会话登录，实测可达 15s 以上）：

- vpn-auth 侧走独立 Feign 客户端 `remoteVpnLinePasswordService`，Nacos `application-dev.yml` 配 `readTimeout: 60000`；登录查询等短调用仍用 10s 默认值。
- yianlian 侧按 `(localUserId, appId)` 加 Redis 互斥锁 `vpn:line_pwd_rotate:lock:*`（锁 TTL 120s，等锁最长 45s，须小于上面的读超时）。
- 成功后写 Redis 标记 `vpn:line_pwd_rotate:done:*`（TTL 120s）。客户端第 2 次尝试若命中标记（含等锁期间前一次刚成功），直接返回成功，不再调易安联，避免拿旧密码二次改密触发「原密码不正确」。
- 等锁超时返回 **线路密码正在更新，请稍后重试**。

### 4.3 客户端 RPC

`VpnCloudService` 新增 `rotateLinePassword(appId)`；须带 access token（与拉凭证相同）。失败走现有 `requestFailed`。

---

## 5. 错误处理

| 场景 | 行为 |
|------|------|
| 非 2040 控制器错误 | 现状，不轮换 |
| 轮换中会话过期 | 走现有会话失效回登录，不重复 toast 本条文案 |
| 生成或同步失败 | 计入 2 次；用尽后 toast 过期原文 |
| 代理/易安联改密失败 | 本地回滚，客户端视为轮换失败 |
| 第 1 次 Feign 超时但服务端实际成功 | 第 2 次命中幂等标记直接返回成功，客户端按成功路径提示并回登录页 |
| 两次请求并发到达 yianlian | 后到者等锁最长 45s；等到结果按幂等标记返回成功，等不到则返回「线路密码正在更新，请稍后重试」 |

---

## 6. 安全

- 级别 S1；不改 TLS。密码路径对照 `GET_USER_CREDENTIALS`：传输仍为 TLS + 字段级 AES（仅后续拉凭证）。
- 轮换接口必须鉴权 + 线路授权，防止改他人线路密码。
- 新密码只存在于 yianlian 内存与加密落库（`password` bcrypt、`encrypted_pwd` AES），响应不带回。

---

## 7. 验收标准（命令可直接复制）

### 7.1 管理端

本条 **无新页面 / 无新菜单**。不跑 `accept.py --path`。不得用 `/index` 冒烟代替本条验收。

### 7.2 构建与部署

含 **yianlian + vpn-auth + 客户端**，须编客户端并部署 vpn（含 93）。

```bash
python -u pipeline/bin/gate.py --run 2026-09-14-线路密码过期自动更新
python -u pipeline/bin/deploy.py --run 2026-09-14-线路密码过期自动更新
python -u pipeline/bin/deploy.py vpn --run 2026-09-14-线路密码过期自动更新
```

### 7.3 客户端走查

本条 **不跑** `accept.py client-ref`。门禁不写客户端「通过」。

### 7.4 人验（需求方自验客户端）

| 模块 | 方式 |
|------|------|
| M1 | 人审服务端：RPC 鉴权、只改线路用户、易安联侧密码已变、本地登录密码不变 |
| M2 | **需求方人工自验**，步骤如下 |

1. 用原本地账号登录桌面端，选线直到 `loginWithAccount` 返回 2040。
2. 应出现 toast **密码已经更新，需要退出重新登录**，界面回到登录页。
3. 用**同一本地密码**再登录，选同一线路，应能连上。
4. 登录页「修改密码」仍只改本地密码，与本条无关。
5. 离线遇 2040：只提示过期原文，不请求云端轮换。

---

## 8. 安全与合规

- M1/M2=S1。不得把本条降成「只改管理端」；也不得用脚本代替需求方对客户端的人工自验。
- 不改 TLS / 证书。
- 对话 / 报告 / 截图脱敏；密码与 token 不入库、不进对话。
- 不关桌面 / 选线验证码。

---

## 9. 非目标

定时 20 天轮换、只处理 `sync_proxy`/`system`、客户端本地生成密码、补偿队列、2040 策略字段、静默重登 SDK、管理端配置页、改本地登录密码、离线自动轮换。
