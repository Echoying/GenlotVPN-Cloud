# SPEC：最低客户端版本拦截

| 项 | 内容 |
|----|------|
| 日期 | 2026-08-16 |
| 状态 | **spec 已批准**；实现计划见 [plan.md](./plan.md) |
| 安全级 | M1=S2；M2/M3=S1（人审 / PHASE2；不改 TLS） |
| 模块 | M1 / M2 / M3 |
| 熟悉度 | 熟悉（对照现有 yianlian CRUD、vpn-auth `LOGIN`、客户端 `VpnCloudService::login`） |

人批准本文件后才写业务代码。实现后按第 7 节跑 `gate` → `deploy` → `accept`；客户端人勾 PHASE2。`client-ref` 为参考走查，不算验收通过。

---

## 1. 背景与目标

需要强制淘汰过旧桌面客户端：过低版本不得完成**云端登录**，并给出升级说明与下载链接。

现状：

- 客户端连本地控制器后会拉版本并写日志，**不拦截**。
- `ruoyi-vpn-auth` 无应用版本校验；`LoginRequest` 无 `client_version`。
- 自动更新安装器（PHASE2 E-03）不在本期。

目标：超管可配策略；云端登录硬拦；客户端友好弹窗；**不拦**易安联 SDK / 本地控制器登录。

---

## 2. 范围

**做：**

- yianlian：单行策略表 + 管理端表单页（启用开关、最低版本、Win/Mac 下载 URL）。
- vpn-auth：云端 `LOGIN` 时读策略并比对；失败 `code=426` + 结构化 `data`。
- proto：`LoginRequest` 增加版本/平台；新增 `ClientVersionReject`。
- 桌面端：登录带版本与平台；收到 426 弹窗展示文案与可点链接。
- 菜单 SQL：挂「客户端版本策略」到 VPN 管理，权限仅给超级管理员角色。

**不做：**

- 不拦本地 30303 / 易安联 SDK 登录。
- 不做包内自动下载安装。
- 不按 Win/Mac 设不同最低版本（仅下载链接分平台）。
- 不改 TLS / 证书 Pin / 帧协议版本字节。
- 不在登录前增加独立 CheckVersion RPC；不做心跳踢会话。
- 不用 `sys_config`（已定方案 2：独立表）。

---

## 3. 行为

### 3.1 管理端（M1）

入口：VPN 管理 → **客户端版本策略**。

页面标题文案：**客户端版本策略**。

表单字段与文案：

| 字段 | 界面文案 | 规则 |
|------|----------|------|
| enabled | 启用版本拦截 | 开关；关则 vpn-auth 不校验 |
| min_version | 最低客户端版本 | 开启时必填；格式 `x.y.z` |
| download_url_windows | Windows 下载链接 | 可空；非空须 `http://` 或 `https://` |
| download_url_macos | macOS 下载链接 | 可空；非空须 `http://` 或 `https://` |
| remark | 备注 | 可空 |

按钮：**保存**。成功提示：**保存成功**。

权限：`vpn:clientVersion:query`（进页）、`vpn:clientVersion:edit`（保存）。菜单与按钮只赋给 **超级管理员**（`role_id=1`）。

初始化：表内一行，`enabled=0`，版本与链接空。

### 3.2 云端登录硬拦（M2）

时机：桌面端登录页调用的 `MessageType.LOGIN`（`TcpRpcDispatcher` 处理登录），在校验验证码 / 账号密码**之前或紧前**完成版本门禁（建议紧挨 captcha 校验之前，避免无谓打认证）。

规则（仅 `enabled=1`）：

| 情况 | 结果 |
|------|------|
| `client_version` 空或非 `x.y.z` | 拒绝 |
| `client_version < min_version` | 拒绝 |
| `client_version >= min_version` | 放行，走原登录 |

比较：按三段整数逐段比较；相等通过。

拒绝时：

- `RpcResponse.code = 426`
- `msg`：固定前缀文案 **「客户端版本过低，请升级至 {min_version} 或以上」**（`{min_version}` 为策略中的值）
- `data`：`ClientVersionReject`：`min_version`、`download_url`（按 `client_platform` 选 Win/Mac；`unknown` 则空字符串）、可选回显 `client_version`

`enabled=0`：不读版本，行为与现网一致。

策略读取：vpn-auth 经 Feign 调 yianlian **内部接口**（`INNER`），可短缓存；管理端保存成功后应使缓存失效或 TTL ≤ 60s。

### 3.3 桌面端（M3）

- `VpnCloudService::login` 填充 `client_version`（`AppInfo.version()`）与 `client_platform`（`windows` / `macos` / `unknown`）。
- 登录失败且 `code==426`：弹窗标题 **「需要升级客户端」**；正文为服务端 `msg`；若有 `download_url` 显示可点击「打开下载页」（系统浏览器打开）；中止后续选线 / 控制器流程。
- 其它登录错误：保持现有提示。
- 本地控制器 `fetchVersions` 仅日志的现状可保留，**不作为**本条拦截依据。

### 3.4 人验 PHASE2（M3）

在 `ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md` 增加一条（实现时写入），大意：开启拦截后，故意低版本或缺版本无法云端登录，弹窗含最低版本与下载链接；关闭拦截后同包可登录；连控制器 / SDK 登录不被本策略拦截。

---

## 4. 数据与接口

### 4.1 表 `vpn_client_version_policy`

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigint PK | 固定为 1 |
| enabled | char(1) | `0`/`1` |
| min_version | varchar(32) | 如 `1.2.0` |
| download_url_windows | varchar(512) | |
| download_url_macos | varchar(512) | |
| 审计 | BaseEntity 常规字段 | |

脚本：`sql/update/20260816_client_version_policy.sql`（建表 + 默认行 + 菜单权限，可重入）。

### 4.2 管理端 API（网关，需登录，非 Inner）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/vpn/clientVersion` | `vpn:clientVersion:query` |
| PUT | `/vpn/clientVersion` | `vpn:clientVersion:edit` |

成功：`AjaxResult`；GET 的 `data` 为策略对象；PUT 保存后返回成功。

### 4.3 内部读接口（供 vpn-auth）

| 方法 | 路径 | 鉴权 |
|------|------|------|
| GET | `/vpn/clientVersion/inner` | `@InnerAuth` |

返回当前策略（含 enabled / min_version / 两链接）。Feign 放在 `ruoyi-api-yianlian`。

### 4.4 Proto

`LoginRequest` 新增：

- `string client_version = 11;`
- `string client_platform = 12;`（`windows` \| `macos` \| `unknown`）

新增：

```text
message ClientVersionReject {
  string min_version = 1;
  string download_url = 2;
  string client_version = 3;
}
```

`RpcResult` 支持 `fail(code, msg, data)`，426 时带上 `ClientVersionReject`。

---

## 5. 前端（管理端）

- 新页：`ruoyi-ui/src/views/vpn/clientVersion/index.vue`（表单，非列表）。
- API：`ruoyi-ui/src/api/vpn/clientVersion.js`。
- 路由由菜单动态下发：`path` 建议 `clientVersion`，`component`=`vpn/clientVersion/index`。
- 文案必须与第 3.1、第 7 节一致，供 Playwright 断言。

---

## 6. 菜单

父菜单：`1061` VPN管理。新建 `C` 菜单「客户端版本策略」+ 两条 `F`（查询 / 修改）。菜单 ID 在 update SQL 中取当前库未占用号段（建议从 `1085` 起，插入前按 `menu_id` 查重）。

| 名称 | 类型 | perms |
|------|------|-------|
| 客户端版本策略 | C | `vpn:clientVersion:query` |
| 版本策略查询 | F | `vpn:clientVersion:query` |
| 版本策略修改 | F | `vpn:clientVersion:edit` |

仅挂到 admin 角色（`role_id=1`）。92 部署后执行 update SQL，否则验收进不去页。

---

## 7. 验收标准（命令可直接复制）

### 7.1 管理端（M1）

| 场景 | `--path` | 操作 | `--expect-text` |
|------|----------|------|-----------------|
| 策略页 | `/yianlian/clientVersion` | 打开页面 | `客户端版本策略` |

不得用 `/index` 冒烟代替。前置：SQL 已执行，admin 具备权限。

### 7.2 构建与部署

本条改动含 **vpn-auth + 客户端**，须编客户端并部署 vpn（含 93）。

可直接复制：

```bash
python -u pipeline/bin/gate.py --run 2026-08-16-最低客户端版本拦截
python -u pipeline/bin/deploy.py --run 2026-08-16-最低客户端版本拦截
python -u pipeline/work/bin/apply_sql_91.py sql/update/20260816_client_version_policy.sql
python -u pipeline/bin/accept.py --run 2026-08-16-最低客户端版本拦截 \
  --path /yianlian/clientVersion \
  --expect-text 客户端版本策略 --title 客户端版本策略页 --module M1 --round 0
```

### 7.3 客户端 AI 基础走查（参考，≠ 验收通过）

改了客户端时，在管理端 accept 之后加 `client-ref`。登录怎么跑见 [client-ref-走查流程.md](../_shared/client-ref-走查流程.md)。全程由**执行走查的 Cursor Agent 自己做**：脚本自动截验证码 → Agent 读图算出式 → Agent 调 `--step submit --code` 回填并点登录。**不把验证码交给人看或手填。** **不改客户端源码**（不靠 objectName，按窗口标题「Genlot VPN」和相对坐标点）。门禁结果只能写 **参考完成 / 参考失败**，禁止写成「通过」。PHASE2 仍人勾。

桌面验证码保持开。账号用 `envs.yaml` 的 `accept.client_user` / `accept.client_password`。

| `--scene` | 做什么 | 参考期望 | 不做 |
|-----------|--------|----------|------|
| `off` | 策略关闭后客户端登录 | 离开登录页 | — |
| `admin-set` | 管理端填：启用拦截 + 高于当前包的 `min_version`（如 `9.9.9`）+ 可选下载链接，点保存 | 出现 **保存成功** | 不测自动下载 |
| `on` | `admin-set` 之后客户端登录 | 弹窗 **需要升级客户端**，正文含最低版本 | **不点**「打开下载页」 |

可直接复制（**Agent 自己**在 `prepare` 与 `submit` 之间读 `CAPTCHA_PATH`，人不用填 `--code`）：

```bash
python -u pipeline/bin/accept.py client-ref --run 2026-08-16-最低客户端版本拦截 --scene off --step prepare
python -u pipeline/bin/accept.py client-ref --run 2026-08-16-最低客户端版本拦截 --scene off --step submit --code <算式结果>
python -u pipeline/bin/accept.py client-ref --run 2026-08-16-最低客户端版本拦截 --scene admin-set --step prepare
python -u pipeline/bin/accept.py client-ref --run 2026-08-16-最低客户端版本拦截 --scene admin-set --step submit
python -u pipeline/bin/accept.py client-ref --run 2026-08-16-最低客户端版本拦截 --scene on --step prepare
python -u pipeline/bin/accept.py client-ref --run 2026-08-16-最低客户端版本拦截 --scene on --step submit --code <算式结果>
```

`admin-set` 无桌面验证码，`submit` 可不带 `--code`。认错验证码可 `--step refresh` 再截，同一场景最多 2 次。截图进当次 `验收报告/client-ref-*.png`。

### 7.4 人验（仍算正式验收）

| 模块 | 方式 |
|------|------|
| M2 | 人审 / 联调：开拦截后低/空版本云端登录失败且提示含最低版本；关拦截后放行 |
| M3 | 人工勾 PHASE2 **S-10**；`client-ref` 只作参考，不得声称客户端已自动验收 |

---

## 8. 安全与合规

- M1=S2；M2/M3=S1。不得把本条降成「只改管理端、跳过客户端人验」。
- 不改 TLS / 证书；不把下载链接当成鉴权绕过手段（硬拦在服务端）。
- 对话 / 报告 / 截图脱敏；密码与 token 不入库、不进对话。
- 不关桌面 / 选线验证码；管理端验证码仅允许按流水线关 92 网关开关。

---

## 9. 非目标（防止膨胀）

自动更新安装器、点击「打开下载页」或包内自动下载、按线路差异化最低版本、版本发布流水线、登录前 CheckVersion RPC、心跳踢旧会话、拦 SDK 登录、多行策略历史表。
