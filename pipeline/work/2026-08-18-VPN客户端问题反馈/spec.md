# SPEC：VPN 客户端问题反馈

| 项 | 内容 |
|----|------|
| 日期 | 2026-08-18 |
| 状态 | **spec 已批准**（2026-08-18「按这个做」）；计划见 [plan.md](./plan.md) |
| 安全级 | 建议 M1=S2；M2/M3=S1（人审 / PHASE2；不改 TLS）。级别由人定，AI 不得自行降级 |
| 模块 | M1 / M2 / M3 |
| 熟悉度 | M1/M2 熟悉；M3 一般（设置页熟，选图/压缩/连传是新面） |

人批准本文件后才写业务代码。实现后按第 7 节跑 `gate` → `deploy` → `accept`；客户端人勾 PHASE2。`client-ref` 为参考走查，不算验收通过。

对照：[模块拆分清单.md](./模块拆分清单.md)。方案 1（9443 Protobuf + yianlian 落库）。

---

## 1. 背景与目标

用户在桌面客户端提交问题；超管在管理端查看并改状态（待处理 / 已排期 / 已修复），形成产品正向循环。

现状：三层都没有问题反馈；桌面与云端只走 9443 TLS + Protobuf（ADR-002）。

目标：做薄的反馈闭环，**不是** TAPD 式工单。

---

## 2. 范围

**做：**

- yianlian：表 `vpn_feedback` + 管理端列表 / 详情 / 改状态 / 看图。
- vpn-auth：`UPLOAD_FEEDBACK_IMAGE`、`SUBMIT_FEEDBACK`；图走 `RemoteFileService`；文字经 Feign Inner 写入 yianlian。
- proto：新文件 `proto/vpn/feedback.proto`，`MessageType` 19 / 20。
- 桌面端：设置页「问题反馈」；未登录必填账号；已登录自动绑用户；最多 3 张截图，先上传再提交。
- 菜单 SQL + 字典 + 一条种子行，仅给超级管理员。

**不做：**

- 客户端查自己的单、管理员回复、评论、指派、优先级、工时、计划日期、处理备注。
- 日志包、任意文件类型、一条 RPC 塞多图、抬高 `maxFrameBytes`。
- 孤儿文件清理任务、同步到易安联设备、导出 / 删除反馈。
- 不改 TLS / 证书 Pin / 帧格式。

---

## 3. 行为

### 3.1 管理端（M1）

入口：VPN 管理 → **问题反馈**。路由 `/yianlian/feedback`。页标题：**问题反馈**。

筛选：账号、状态、分类。按钮：**搜索** / **重置**。

列表列：标题、账号、分类、状态、客户端版本、提交时间。操作列：**详情**。

详情弹窗标题：**反馈详情**。展示标题、账号、分类、描述、版本、平台、IP、时间、最多 3 张图（缩略图，点击放大）。

状态用三个**可见**选项：**待处理** / **已排期** / **已修复**（不用 Element 隐藏下拉副本），再点 **保存**。三态可任意改。成功提示：**保存成功**。非法 status 提示：**状态不正确**。

权限只给超级管理员（`role_id=1`）：

| 权限 | 用途 |
|------|------|
| `vpn:feedback:list` | 进页、列表 |
| `vpn:feedback:query` | 详情 |
| `vpn:feedback:edit` | 改状态 |

### 3.2 云端 RPC（M2）

与 `LIST_PUBLIC_LINES` 一样：未登录可不带 token、不做 MAC。有合法 token 则按登录用户绑定，**忽略**手填账号。

**上传** `UPLOAD_FEEDBACK_IMAGE = 20`：

- 只收 jpeg / png（校验文件头，不只看 `content_type`）。
- **400KB 限制的是上传 RPC 里 `image` 字段的字节数**（客户端压缩之后的结果），不是用户选中的原图大小。原图可以更大；压完仍超过 400KB 则拒绝。服务端按 payload 再拦一道。
- vpn-auth 调 `RemoteFileService` 存文件；Redis 键 `vpn_feedback_img:{image_id}` → url，TTL **2 小时**。
- 回包只带 `image_id`。
- 按 IP 限流：20 次/小时。超限文案：**上传过于频繁，请稍后再试**。
- 文件服务失败：**截图上传失败，请重试**。

**提交** `SUBMIT_FEEDBACK = 19`：

- 标题必填，最长 80；描述必填，最长 2000。
- 分类可空；合法值 `connect` / `login` / `ui` / `other`；非法当空，不报错。
- 未登录：`user_name` 必填（不校验密码）。能在 `vpn_user` 对上则回填 `user_id`，对不上也落库。
- 已登录：用 token 用户的 id / userName。
- `image_ids` 0～3 个；过期或无效则整单失败，文案：**截图已失效，请重新添加**。多于 3 个：**截图最多 3 张**。
- 服务端写入 IP、`client_version`、`client_platform`（`windows` / `macos` / `unknown`）。
- 新建 `status=0`。
- 按 IP 限流：5 次/小时。超限：**提交过于频繁，请稍后再试**。
- vpn-auth 把 image_id 换成 url 后 `POST /vpn/feedback/inner`。

不改 TLS，不抬帧上限（两端仍 1MB/帧）。

### 3.3 桌面端（M3）

设置左侧与「关于」并列新增 **问题反馈**（登录前也显示）。已登录时在 `main.qml` 顶栏「退出登录」左侧加 **设置**（否则选线后进不了设置页）。

| 界面 | 文案 / 规则 |
|------|-------------|
| 账号 | 仅未登录显示、必填，占位 **请输入账号** |
| 标题 | 必填 |
| 描述 | 必填 |
| 分类 | 界面：**连接** / **登录** / **界面** / **其他**；上报值 `connect` / `login` / `ui` / `other`；可空 |
| 截图 | 最多 3 槽；**添加截图**；本地预览；可删未提交的槽 |
| 按钮 | **提交** |
| 成功 | **提交成功** |

选图后先压成 jpeg（或保留已合规且 ≤400KB 的 png）。**400KB 看压缩后的文件**，不是原图。压完再逐张 `UPLOAD`，最后 `SUBMIT` 带 id。压不下来则提示 **图片过大或格式不支持**，不要把超限原图直接上传。

客户端校验文案：请填写账号 / 请填写标题 / 请填写描述 / 图片过大或格式不支持。服务端文案原样 toast。9443 断开走现有重连。

不提供「我的反馈」列表。

### 3.4 人验 PHASE2（M3）

实现时在 `ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md` 增加 **C-07**：设置页可提交问题反馈；未登录必填账号；已登录不填账号；最多 3 张截图先传后交；超管在管理端能看到并改状态。人勾，不自动验收。

---

## 4. 数据与接口

### 4.1 表 `vpn_feedback`

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigint PK 自增 | |
| title | varchar(80) | 必填 |
| content | varchar(2000) | 必填 |
| category | varchar(16) | 可空：`connect` / `login` / `ui` / `other` |
| status | char(1) | `0` 待处理 · `1` 已排期 · `2` 已修复；新建 `0` |
| user_id | bigint | 可空 |
| user_name | varchar(64) | 必填 |
| client_version | varchar(32) | 可空 |
| client_platform | varchar(16) | `windows` / `macos` / `unknown` |
| ipaddr | varchar(128) | 服务端写 |
| image_urls | varchar(2048) | JSON 数组，最多 3 个 url，可 `[]` |
| 审计 | BaseEntity | `create_time` / `update_time` 等 |

索引：`status`、`create_time`、`user_name`。

脚本：`sql/update/20260818_vpn_feedback.sql`（建表 + 字典 + 菜单 + 种子行，可重入）。

种子行：`title=验收用反馈`，`content=流水线种子数据`，`user_name=accept`，`status=0`，无图。供 Playwright 点「详情」。

字典：

| dict_type | 标签 | 值 |
|-----------|------|-----|
| `vpn_feedback_status` | 待处理 / 已排期 / 已修复 | `0` / `1` / `2` |
| `vpn_feedback_category` | 连接 / 登录 / 界面 / 其他 | `connect` / `login` / `ui` / `other` |

### 4.2 管理端 API（网关，需登录，非 Inner）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/vpn/feedback/list` | `vpn:feedback:list` |
| GET | `/vpn/feedback/{id}` | `vpn:feedback:query` |
| PUT | `/vpn/feedback` | `vpn:feedback:edit` |

PUT 体只认 `id`、`status`，其它字段忽略。成功 `AjaxResult`，提示走前端 **保存成功**。

### 4.3 内部写接口（供 vpn-auth）

| 方法 | 路径 | 鉴权 |
|------|------|------|
| POST | `/vpn/feedback/inner` | `@InnerAuth` |

体：title、content、category、userId、userName、clientVersion、clientPlatform、ipaddr、imageUrls。Feign 放在 `ruoyi-api-yianlian`。未登录时 `userId` 可空，由 yianlian 按 `userName` 回填能对上的 `vpn_user.user_id`。

### 4.4 Proto

`envelope.proto` 增加：

```text
SUBMIT_FEEDBACK = 19;
UPLOAD_FEEDBACK_IMAGE = 20;
```

`proto/vpn/feedback.proto`：

```text
message UploadFeedbackImageRequest {
  bytes image = 1;
  string content_type = 2; // image/jpeg | image/png
}
message UploadFeedbackImageResponse {
  string image_id = 1;
}
message SubmitFeedbackRequest {
  string title = 1;
  string content = 2;
  string category = 3;
  string user_name = 4;
  string client_version = 5;
  string client_platform = 6;
  repeated string image_ids = 7;
}
message SubmitFeedbackResponse {
  int64 feedback_id = 1;
}
```

---

## 5. 前端（管理端）

- 新页：`ruoyi-ui/src/views/vpn/feedback/index.vue`（列表 + 详情弹窗）。
- API：`ruoyi-ui/src/api/vpn/feedback.js`。
- 路由由菜单下发：`path`=`feedback`，`component`=`vpn/feedback/index`。
- 文案必须与第 3.1、第 7 节一致，供 Playwright 断言。点过按钮后断言**弹窗标题**，不要用页面任意 toast。

---

## 6. 菜单

父菜单：`1061` VPN 管理。菜单 ID 实现时按库查重（建议 **1092** C / **1093** list / **1094** query / **1095** edit）。

| 名称 | 类型 | perms |
|------|------|-------|
| 问题反馈 | C | `vpn:feedback:list` |
| 反馈查询 | F | `vpn:feedback:list` |
| 反馈详情 | F | `vpn:feedback:query` |
| 反馈改状态 | F | `vpn:feedback:edit` |

仅挂 `role_id=1`。92 部署后执行 update SQL，否则验收进不去页。

---

## 7. 验收标准（命令可直接复制）

### 7.1 管理端（M1）

| 场景 | `--path` | 操作 | `--expect-text` |
|------|----------|------|-----------------|
| 列表页 | `/yianlian/feedback` | 打开页面 | `问题反馈` |
| 详情 | `/yianlian/feedback` | 点 **详情** | `反馈详情` |
| 改状态 | `/yianlian/feedback` | 点 **详情** → **已排期** → **保存** | `保存成功` |

不得用 `/index` 冒烟代替。前置：SQL 已执行，admin 具备权限，种子行「验收用反馈」存在。

### 7.2 构建与部署

本条含 **yianlian + vpn-auth + 客户端**，须全量 `gate` 并 `deploy.py vpn`（含 93）。92 上 `ruoyi-file` 保持运行（传图依赖）。

```bash
python -u pipeline/bin/gate.py --run 2026-08-18-VPN客户端问题反馈
python -u pipeline/bin/deploy.py vpn --run 2026-08-18-VPN客户端问题反馈
python -u pipeline/work/bin/apply_sql_91.py sql/update/20260818_vpn_feedback.sql
python -u pipeline/bin/accept.py --run 2026-08-18-VPN客户端问题反馈 \
  --path /yianlian/feedback \
  --expect-text 问题反馈 --title 问题反馈列表 --module M1 --round 0
python -u pipeline/bin/accept.py --run 2026-08-18-VPN客户端问题反馈 \
  --path /yianlian/feedback --click 详情 \
  --expect-text 反馈详情 --title 反馈详情弹窗 --module M1 --round 0
python -u pipeline/bin/accept.py --run 2026-08-18-VPN客户端问题反馈 \
  --path /yianlian/feedback --click 详情 --click 已排期 --click 保存 \
  --expect-text 保存成功 --title 反馈改状态 --module M1 --round 0
```

### 7.3 客户端 AI 基础走查（参考，≠ 验收通过）

登录怎么认验证码见 [client-ref-走查流程.md](../_shared/client-ref-走查流程.md)。本期 **必须** 跑登录回归 + 下面两条反馈走查。实现阶段扩展 `accept_client_ref.py` 增加 `feedback-guest` / `feedback-login`（不改客户端源码，按窗口标题「Genlot VPN」和相对坐标；登录页右上角 **设置** → 侧栏 **问题反馈**）。

选图：**用走查自己截的客户端窗图**，写入当次 `验收报告/client-ref-attach-*.jpg`（若 PNG 超过 400KB 则压成 JPEG）。点 **添加截图** 后，对系统文件框填该绝对路径并回车。每条走查 **至少 1 张**，不必凑满 3 张。门禁只写 **参考完成 / 参考失败**。账号用 `envs.yaml` 的 `accept.client_user`（对话不写密码）。

| `--scene` | 做什么 | 参考期望 | 不做 |
|-----------|--------|----------|------|
| `off` | 登录回归（改了 proto 不能把登录打坏） | 离开登录页（`left-login-choose` / `left-login-wide`） | — |
| `feedback-guest` | **未登录**：设置 → 问题反馈 → 填账号、标题 `client-ref未登录`、描述 `走查` → 选 1 张走查截图 → 提交 | **提交成功**；有账号框；槽里有图 | 不凑 3 张 |
| `feedback-login` | **已登录**：先登录，再设置 → 问题反馈 → 标题 `client-ref已登录`、描述 `走查` → 选 1 张走查截图 → 提交 | **提交成功**；**没有**账号框；槽里有图 | 不凑 3 张 |

`feedback-guest` 无桌面验证码，`submit` 可不带 `--code`。`feedback-login` 的 `prepare` 截登录验证码，`submit --code` 里完成登录并继续选图、交反馈。

```bash
python -u pipeline/bin/accept.py client-ref --run 2026-08-18-VPN客户端问题反馈 --scene off --step prepare
python -u pipeline/bin/accept.py client-ref --run 2026-08-18-VPN客户端问题反馈 --scene off --step submit --code <算式结果>
python -u pipeline/bin/accept.py client-ref --run 2026-08-18-VPN客户端问题反馈 --scene feedback-guest --step prepare
python -u pipeline/bin/accept.py client-ref --run 2026-08-18-VPN客户端问题反馈 --scene feedback-guest --step submit
python -u pipeline/bin/accept.py client-ref --run 2026-08-18-VPN客户端问题反馈 --scene feedback-login --step prepare
python -u pipeline/bin/accept.py client-ref --run 2026-08-18-VPN客户端问题反馈 --scene feedback-login --step submit --code <算式结果>
```

截图进当次 `验收报告/client-ref-*.png`。认错验证码可 `--step refresh`，同一场景最多 2 次。

### 7.4 人验（仍算正式验收）

| 模块 | 方式 |
|------|------|
| M2 | 人审：未登录填账号可落库；已登录绑用户；无 token 缺账号拒绝；image_id 过期拒绝；不改 TLS |
| M3 | 人工勾 PHASE2 **C-07**；`client-ref` 只作参考，不得声称客户端已自动验收 |

---

## 8. 安全与合规

- 建议 M1=S2；M2/M3=S1。不得把本条降成「只改管理端、跳过客户端人验」。
- 未登录可写可传图：必须按 IP 限流；单张 ≤400KB；只收 jpeg/png 文件头。
- 不改 TLS / Pin / 帧上限。
- 日志不打密码、token、图片二进制；只记 image_id / url。
- 对话 / 报告 / 截图脱敏。
- 不关桌面 / 选线验证码；管理端验证码仅允许按流水线关 92 网关开关。

---

## 9. 非目标（防止膨胀）

客户端工单列表、回复推送、评论、指派、关闭/驳回工作流、排期日期、处理备注、日志附件、任意文件、一条 RPC 多图、抬帧上限、孤儿清理任务、同步易安联、导出、删除、匿名不填账号。
