# VPN 客户端问题反馈 Implementation Plan

> **For agentic workers:** 按本计划逐任务实现。步骤用 checkbox（`- [ ]`）跟踪。门禁以 `pipeline/bin/gate.py` / `deploy.py` / `accept.py` 为准；纯逻辑用 JUnit。**未批书面 spec 不写业务代码。** 不要自动 `git commit`，除非用户明确要求提交。跨模块可用 `_subagent-driven-development`，本会话内联实现也可以。

**Goal:** 桌面端设置页提交问题（未登录必填账号，最多 3 张压缩后 ≤400KB 的截图）；超管在管理端查看并改三态。

**Architecture:** 9443 新增 `UPLOAD_FEEDBACK_IMAGE=20` / `SUBMIT_FEEDBACK=19`。vpn-auth 将图交给 `RemoteFileService`，image_id 进 Redis（2 小时）；提交时换成 url，Feign Inner 写入 yianlian `vpn_feedback`。管理端列表/详情/改状态。客户端不查自己的单。不改 TLS、不抬帧上限。

**Tech Stack:** Java 8、Feign、MyBatis、Protobuf、Vue 2 + Element UI、Qt 6、`ruoyi-file`、Playwright、client-ref（Win32 坐标）。

**Spec:** [spec.md](./spec.md)

## Global Constraints

- 文案：页标题 `问题反馈`；弹窗 `反馈详情`；保存 `保存成功`；提交 `提交成功`；设置侧栏 `问题反馈`
- 分类上报值：`connect` / `login` / `ui` / `other`；非法当空
- 状态：`0` 待处理 / `1` 已排期 / `2` 已修复；详情用三个可见选项，不用隐藏 el-select
- 400KB = 上传 RPC `image` 字段字节数（压缩后）；原图可更大
- 未登录可提交/传图，必填账号，不校验密码；已登录忽略手填账号
- 权限仅 `role_id=1`：`vpn:feedback:list` / `query` / `edit`
- 菜单建议 1092–1095，插入前按 `menu_id` 查重
- 不改 TLS / Pin / `maxFrameBytes`；不做评论/指派/删除/导出/孤儿清理
- 安全级建议 M1=S2、M2/M3=S1；客户端不自动验收
- 提交：仅当用户说「提交」时才 `git commit`

## 文件地图

| 文件 | 职责 |
|------|------|
| `sql/update/20260818_vpn_feedback.sql` | 建表、字典、菜单 1092–1095、种子行「验收用反馈」 |
| `.../domain/VpnFeedback.java` | 实体 |
| `.../mapper/VpnFeedbackMapper.java` + `mapper/yianlian/VpnFeedbackMapper.xml` | 列表/详情/插入/改状态 |
| `.../service/vpn/FeedbackRules.java` | 分类/状态/长度校验（可单测） |
| `.../service/vpn/FeedbackImageRules.java` | 文件头 + 400KB（可单测） |
| `.../service/vpn/IVpnFeedbackService.java` + impl | 管理查询、改状态、Inner 落库、按 userName 回填 userId |
| `.../controller/VpnFeedbackController.java` | list / `{id}` / PUT / inner POST |
| `ruoyi-api-yianlian/.../VpnFeedbackCreateDTO.java` | Inner 体 |
| `ruoyi-api-yianlian/.../RemoteVpnFeedbackService.java` + Fallback | vpn-auth 写库 |
| `ruoyi-ui/src/api/vpn/feedback.js` | 管理端 API |
| `ruoyi-ui/src/views/vpn/feedback/index.vue` | 列表 + 详情 |
| `proto/vpn/envelope.proto` | MessageType 19/20 |
| `proto/vpn/feedback.proto` | 上传/提交消息 |
| `ruoyi-vpn-auth/.../VpnTcpRateLimitService.java` | 上传 20/小时、提交 5/小时 |
| `ruoyi-vpn-auth/.../VpnTcpBusinessHandler.java` | 19/20 不做 MAC |
| `ruoyi-vpn-auth/.../TcpRpcDispatcher.java` | 分发上传/提交 |
| `ruoyi-vpn-auth/.../VpnFeedbackRpcService.java` | 限流、存图、换 url、Feign |
| `ruoyi-vpn-auth/.../InMemoryMultipartFile.java` | bytes → MultipartFile |
| `ruoyi-vpn-client/.../VpnCloudService.*` | uploadFeedbackImage / submitFeedback |
| `ruoyi-vpn-client/.../VpnFlowController.*` | QML 可调提交、压缩 |
| `ruoyi-vpn-client/qml/pages/SettingsPage.qml` | 问题反馈分区 |
| `ruoyi-vpn-client/qml/main.qml` | 已登录顶栏加「设置」 |
| `ruoyi-vpn-client/src/core/PacketLogUtil.cpp` | 新类型名 |
| `ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md` | C-07 |
| `pipeline/work/bin/accept_client_ref.py` | `feedback-guest` / `feedback-login` |

---

### Task 1: SQL（表 + 字典 + 菜单 + 种子）

**Files:**
- Create: `sql/update/20260818_vpn_feedback.sql`

**Interfaces:**
- Consumes: 无
- Produces: `vpn_feedback`；字典 `vpn_feedback_status` / `vpn_feedback_category`；菜单 1092–1095；种子 `title=验收用反馈`

- [ ] **Step 1: 写可重入 SQL**

```sql
-- VPN 客户端问题反馈（可重入）
CREATE TABLE IF NOT EXISTS vpn_feedback (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  title varchar(80) NOT NULL COMMENT '标题',
  content varchar(2000) NOT NULL COMMENT '描述',
  category varchar(16) DEFAULT '' COMMENT 'connect/login/ui/other',
  status char(1) NOT NULL DEFAULT '0' COMMENT '0待处理 1已排期 2已修复',
  user_id bigint DEFAULT NULL COMMENT 'vpn_user.user_id',
  user_name varchar(64) NOT NULL COMMENT '账号',
  client_version varchar(32) DEFAULT '' COMMENT '客户端版本',
  client_platform varchar(16) DEFAULT '' COMMENT 'windows/macos/unknown',
  ipaddr varchar(128) DEFAULT '' COMMENT '提交IP',
  image_urls varchar(2048) DEFAULT '[]' COMMENT 'JSON数组最多3个url',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_vpn_feedback_status (status),
  KEY idx_vpn_feedback_ct (create_time),
  KEY idx_vpn_feedback_un (user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPN客户端问题反馈';

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '反馈状态', 'vpn_feedback_status', '0', 'admin', NOW(), '问题反馈状态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'vpn_feedback_status');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '反馈分类', 'vpn_feedback_category', '0', 'admin', NOW(), '问题反馈分类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'vpn_feedback_category');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 1, '待处理', '0', 'vpn_feedback_status', 'warning', 'Y', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vpn_feedback_status' AND dict_value='0');
-- 同样插入 已排期=1 info、已修复=2 success
-- 分类：连接 connect / 登录 login / 界面 ui / 其他 other

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1092, '问题反馈', 1061, 12, 'feedback', 'vpn/feedback/index', NULL, '', 1, 0, 'C', '0', '0', 'vpn:feedback:list', 'message', 'admin', NOW(), 'VPN客户端问题反馈'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE menu_id = 1092 OR (perms = 'vpn:feedback:list' AND menu_type = 'C')
);
-- 1093 list F / 1094 query F / 1095 edit F；role_id=1

INSERT INTO vpn_feedback (title, content, category, status, user_name, image_urls, create_by, create_time)
SELECT '验收用反馈', '流水线种子数据', '', '0', 'accept', '[]', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vpn_feedback WHERE title = '验收用反馈' AND user_name = 'accept');
```

若 1092 已被占用，改用当前库 `MAX(menu_id)+1` 并回写 spec 第 6 节。

- [ ] **Step 2: 本机可读检查** — 打开文件确认无密码、可重入。
- [ ] **Step 3: 提交（仅用户要求时）** — 跳过。

---

### Task 2: 校验纯逻辑（JUnit）

**Files:**
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/FeedbackRules.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/FeedbackImageRules.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/test/java/com/ruoyi/yianlian/service/vpn/FeedbackRulesTest.java`

**Interfaces:**
- Produces:
  - `FeedbackRules.normalizeCategory(String raw) -> String`（合法返回原值 trim，否则 `""`）
  - `FeedbackRules.isValidStatus(String status) -> boolean`（仅 `0|1|2`）
  - `FeedbackRules.assertTitle(String title)` / `assertContent(String content)`（空或超长抛 `ServiceException`，文案：请填写标题 / 标题过长 / 请填写描述 / 描述过长）
  - `FeedbackImageRules.MAX_BYTES = 400 * 1024`
  - `FeedbackImageRules.detectContentType(byte[] image) -> String`（`image/jpeg` / `image/png` / `null`）
  - `FeedbackImageRules.assertUpload(byte[] image)`（空、超长、非法头抛 `ServiceException`：图片过大或格式不支持）

- [ ] **Step 1: 写会失败的测试**

```java
@Test
public void normalizeCategory_illegalBecomesEmpty() {
    assertEquals("connect", FeedbackRules.normalizeCategory("connect"));
    assertEquals("", FeedbackRules.normalizeCategory("bug"));
    assertEquals("", FeedbackRules.normalizeCategory(null));
}

@Test
public void image_jpegHeaderOkPngHeaderOk() {
    byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    assertEquals("image/jpeg", FeedbackImageRules.detectContentType(jpeg));
    assertEquals("image/png", FeedbackImageRules.detectContentType(png));
    assertNull(FeedbackImageRules.detectContentType(new byte[] {0x00}));
}
```

- [ ] **Step 2: 跑测确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-yianlian -Dtest=FeedbackRulesTest test`

Expected: 编译失败（类不存在）。

- [ ] **Step 3: 写最小实现** — `FeedbackRules` / `FeedbackImageRules` 按上面接口。JPEG 头至少 `FF D8 FF`；PNG 头 `89 50 4E 47`。
- [ ] **Step 4: 再跑测确认通过**
- [ ] **Step 5: 提交（仅用户要求时）**

---

### Task 3: yianlian 落库 + 管理 API

**Files:**
- Create: `.../domain/VpnFeedback.java`（字段与表一致，`imageUrls` 为 String）
- Create: mapper 接口 + `src/main/resources/mapper/yianlian/VpnFeedbackMapper.xml`
- Create: `IVpnFeedbackService` + `VpnFeedbackServiceImpl`
- Create: `VpnFeedbackController`
- Create: `VpnFeedbackCreateDTO`、`RemoteVpnFeedbackService`、`RemoteVpnFeedbackFallbackFactory`

**Interfaces:**
- Consumes: `FeedbackRules`；`IVpnUserService.selectUserByUserName`
- Produces:
  - `GET /vpn/feedback/list` → `TableDataInfo`（`startPage()`）
  - `GET /vpn/feedback/{id}` → `AjaxResult`
  - `PUT /vpn/feedback` 体 `{id, status}`，只改状态
  - `POST /vpn/feedback/inner` `@InnerAuth`，`R<Long>` 新 id
  - Feign: `R<Long> create(@RequestBody VpnFeedbackCreateDTO dto, @RequestHeader FROM_SOURCE source)`

对照：`VpnLogininforController` 列表分页；`VpnClientVersionController` 的 `@InnerAuth`。

- [ ] **Step 1: 实体 + Mapper XML** — `selectFeedbackList` 按 userName/status/category；`insertFeedback` 默认 status=`0`、image_urls 空则 `[]`；`updateStatus(id, status)`。
- [ ] **Step 2: Service** — Inner：`userName` 空抛「请填写账号」；`userId==null` 时 `selectUserByUserName`，对上则 setUserId；`normalizeCategory`；`assertTitle/assertContent`；imageUrls 解析 JSON 数组，长度 >3 抛「截图最多 3 张」。PUT：`isValidStatus` 否则「状态不正确」。
- [ ] **Step 3: Controller + Feign** — 权限字面量与 spec 一致。不要自造返回包装。
- [ ] **Step 4: 编译**

Run: `mvn clean package -pl ruoyi-modules/ruoyi-yianlian -am -DskipTests`

Expected: BUILD SUCCESS（JDK8 + clean）。

---

### Task 4: 管理端 Vue

**Files:**
- Create: `ruoyi-ui/src/api/vpn/feedback.js`
- Create: `ruoyi-ui/src/views/vpn/feedback/index.vue`

**Interfaces:**
- Consumes: `/yianlian/vpn/feedback/list` 等（与 `clientVersion.js` 同样加 `/yianlian` 前缀）
- Produces: 页标题「问题反馈」；详情弹窗 `title="反馈详情"`（`el-dialog` 的 title，accept 靠这个）

- [ ] **Step 1: API**

```js
export function listFeedback(query) {
  return request({ url: '/yianlian/vpn/feedback/list', method: 'get', params: query })
}
export function getFeedback(id) {
  return request({ url: '/yianlian/vpn/feedback/' + id, method: 'get' })
}
export function updateFeedbackStatus(data) {
  return request({ url: '/yianlian/vpn/feedback', method: 'put', data })
}
```

- [ ] **Step 2: 列表页** — 对照 `views/vpn/logininfor/index.vue`。筛选账号/状态/分类（`dict.type.vpn_feedback_status` / `vpn_feedback_category`）。列：标题、账号、分类、状态、客户端版本、提交时间。操作：**详情**（`v-hasPermi vpn:feedback:query`）。不要导出/删除。
- [ ] **Step 3: 详情弹窗** — `:title="'反馈详情'"`。描述、最多 3 个 `el-image`（`JSON.parse(imageUrls||'[]')`）。三个可见按钮文案必须是 **待处理** / **已排期** / **已修复**（`el-radio-group` 或三个 `el-button`，当前态高亮）。**保存** 调 PUT，成功 `this.$modal.msgSuccess("保存成功")`。
- [ ] **Step 4: 本机构建前端（实现阶段）** — `npm run build:prod` 可放到 Task 10 的 gate。

dicts 在页上：

```js
dicts: ['vpn_feedback_status', 'vpn_feedback_category']
```

---

### Task 5: Proto

**Files:**
- Modify: `proto/vpn/envelope.proto`（enum 末尾加 19/20）
- Create: `proto/vpn/feedback.proto`

**Interfaces:**
- Produces: `MessageType.SUBMIT_FEEDBACK=19`、`UPLOAD_FEEDBACK_IMAGE=20`；四个 message 字段号与 spec 4.4 一致。

```protobuf
syntax = "proto3";
package vpn;
option java_package = "com.ruoyi.vpn.protocol";
option java_multiple_files = true;

message UploadFeedbackImageRequest {
  bytes image = 1;
  string content_type = 2;
}
message UploadFeedbackImageResponse { string image_id = 1; }
message SubmitFeedbackRequest {
  string title = 1;
  string content = 2;
  string category = 3;
  string user_name = 4;
  string client_version = 5;
  string client_platform = 6;
  repeated string image_ids = 7;
}
message SubmitFeedbackResponse { int64 feedback_id = 1; }
```

`java_package` 与现有 proto 一致。客户端 CMake 已 `GLOB vpn/*.proto`，新文件会自动编。

- [ ] **Step 1: 改 proto**
- [ ] **Step 2: 编协议模块**

Run: `mvn -pl ruoyi-vpn-protocol -am -DskipTests package`

Expected: BUILD SUCCESS。

---

### Task 6: vpn-auth 上传 + 提交

**Files:**
- Modify: `VpnTcpBusinessHandler.requiresMac` — 排除 `SUBMIT_FEEDBACK`、`UPLOAD_FEEDBACK_IMAGE`（与 `LIST_PUBLIC_LINES` 同类）
- Modify: `TcpRpcDispatcher.dispatch` switch
- Modify: `VpnTcpRateLimitService` — `tryAcquireFeedbackUpload(ip)` / `tryAcquireFeedbackSubmit(ip)`
- Create: `InMemoryMultipartFile`（实现 `MultipartFile`，从 byte[] 构造）
- Create: `VpnFeedbackRpcService`
- Modify: `ruoyi-vpn-auth/pom.xml` — 若编译缺 `RemoteFileService`，显式加 `ruoyi-api-system`
- Copy 或复用：把 `FeedbackImageRules` 逻辑放到 vpn-auth（**不要**让 vpn-auth 依赖 yianlian 实现类）。复制一份 `com.ruoyi.vpn.auth.util.FeedbackImageRules`，规则与 yianlian 相同（400KB、JPEG/PNG 头）。

**Interfaces:**
- Consumes: `RemoteFileService.upload`、`RemoteVpnFeedbackService.create`
- Produces: 上传成功 `UploadFeedbackImageResponse.image_id`；提交成功 `SubmitFeedbackResponse.feedback_id`

Redis：`vpn_feedback_img:{uuid}` → url 字符串，TTL 7200 秒。

限流：上传键 `vpn_tcp:feedback_upload:` + ip，窗口 3600s，上限 20；提交键 `vpn_tcp:feedback_submit:`，上限 5。超限文案与 spec 一致。

- [ ] **Step 1: requiresMac + dispatch case**
- [ ] **Step 2: 上传** — 读 client IP（现有 dispatcher 取 IP 的方式）；`assertUpload`；`content_type` 仅作提示，以文件头为准；`InMemoryMultipartFile` 文件名 `feedback.jpg` / `feedback.png`；Feign 失败 →「截图上传失败，请重试」。
- [ ] **Step 3: 提交** — 有合法 token（`resolveToken` 非空且 Redis 有会话）则用 `resolveUserId` / `resolveUsername`，忽略 `user_name`；否则 `user_name` 空 →「请填写账号」。`image_ids` >3 →「截图最多 3 张」；每个 id 从 Redis 取 url，缺一个 →「截图已失效，请重新添加」。组装 DTO 调 Inner。标题/描述校验文案与 spec 一致。`client_version` / `client_platform` 原样写入。
- [ ] **Step 4: 编译 vpn-auth**

Run: `mvn clean package -pl ruoyi-vpn-auth -am -DskipTests`

取 IP：对照 `handleLogin` 里现有 `session.getClientIp()`（或等价字段），没有就用连接远端地址，不要新造协议字段。

---

### Task 7: Qt 设置页 + RPC

**Files:**
- Modify: `VpnCloudService.h/.cpp` — `uploadFeedbackImage`、`submitFeedback`
- Modify: `VpnFlowController` — Q_INVOKABLE 给 QML：压缩、连传、提交
- Modify: `SettingsPage.qml` — sections 增加 `{ key: "feedback", title: qsTr("问题反馈") }`
- Modify: `main.qml` — 顶栏「退出登录」左侧加 GhostButton **设置**，`vpnFlow.goToSettings()`
- Modify: `PacketLogUtil.cpp` — 19/20 名称；payload 日志**不要**打印 image bytes
- Modify: `PHASE2_CHECKLIST.md` — C-07

**Interfaces:**
- Consumes: proto 生成的 `UploadFeedbackImageRequest` 等；`AppInfo::version()`、`ClientDeviceInfo::platformId()`
- Produces: 成功 toast「提交成功」；失败用服务端 `msg` 或「请填写账号」等客户端文案

压缩（Flow 或独立 `FeedbackImageCompressor`）：

```cpp
// 读入 QImage；输出 JPEG quality 从 85 递减到 40，直到 size <= 400*1024
// 已是 png 且 raw.size()<=400*1024 且文件头合法可原样传
// 压不下来返回空 QByteArray，QML 提示「图片过大或格式不支持」
```

- [ ] **Step 1: CloudService RPC** — `sendRpc(UPLOAD_FEEDBACK_IMAGE / SUBMIT_FEEDBACK)`。未登录也要能发（不要 `assertTokenExists`）。上传回 `image_id`；提交回 `feedback_id`。
- [ ] **Step 2: Flow** — `Q_INVOKABLE void submitFeedback(QString title, QString content, QString category, QString userName, QVariantList localPaths)`。未登录且 userName 空 → toast「请填写账号」。已登录不把账号框的值当身份（QML 直接不传或传空）。先逐张 upload，再 submit。
- [ ] **Step 3: Settings QML** — `vpnFlow.loggedIn` 为 false 时显示账号框，占位「请输入账号」。标题、描述、分类 Combo（空 + 四项，值为 connect/login/ui/other）。三个槽 +「添加截图」（`FileDialog` `nameFilters: ["Images (*.png *.jpg *.jpeg)"]`）。按钮「提交」。
- [ ] **Step 4: 顶栏设置** — `main.qml` 的 `topBar` 在「退出登录」左边加「设置」。
- [ ] **Step 5: PHASE2 C-07** — 按 spec 3.4 原文写入清单表。
- [ ] **Step 6: 本机编客户端** — `bin\build-vpn-client.bat --package` 或等 Task 10 的 `gate.py`。

---

### Task 8: client-ref 两条反馈走查

**Files:**
- Modify: `pipeline/work/bin/accept_client_ref.py`
- Modify: 可选补一句到 `pipeline/work/_shared/client-ref-走查流程.md`：当次额外 scene 以 spec 第 7.3 为准

**Interfaces:**
- Produces: `--scene` 增加 `feedback-guest`、`feedback-login`（保留 `off` / `on` / `admin-set`）

- [ ] **Step 1: 附件图** — prepare/submit 时把当前窗截成 `验收报告/client-ref-attach-{scene}-{stamp}.jpg`，Pillow 压到 ≤400KB。
- [ ] **Step 2: guest** — 确保未登录（在选线则先点「退出登录」）。点登录页右上「设置」（约客户区 `0.93, 0.08`，实现时按截图微调）。点侧栏「问题反馈」（侧栏宽 168，第 5 项约 y= 标题下第 5 行）。填账号=`CLIENT_E2E_USER`、标题=`client-ref未登录`、描述=`走查`。点「添加截图」，等文件框，`Ctrl+L` 或点文件名框，输入附件绝对路径，回车。点「提交」。探测 toast「提交成功」或结果图含该文案。
- [ ] **Step 3: login** — 复用现有登录坐标 + 验证码。登录成功后点顶栏「设置」（「退出登录」左侧）。表单**不应**有「请输入账号」。标题=`client-ref已登录`，同样选 1 张走查图后提交。
- [ ] **Step 4: 探测** — 增加 `kind`：`feedback-ok` / `feedback-missing-user` / `still-login-or-unknown`。guest 成功且图里还有账号框 → 参考完成；login 成功且图里没有「请输入账号」→ 参考完成。
- [ ] **Step 5: 不改客户端源码** 凑 objectName。文件框用键盘填路径。

---

### Task 9: 门禁（spec 第 7 节，批准且实现后才跑）

不要在未实现时声称完成。顺序：

```bash
python -u pipeline/bin/gate.py --run 2026-08-18-VPN客户端问题反馈
python -u pipeline/bin/deploy.py vpn --run 2026-08-18-VPN客户端问题反馈
python -u pipeline/work/bin/apply_sql_91.py sql/update/20260818_vpn_feedback.sql
```

然后三条 `accept.py`（列表 / 详情 / 改状态）+ `client-ref` 的 `off` / `feedback-guest` / `feedback-login`。命令原文见 spec 第 7.2 / 7.3。

`ruoyi-file` 必须在 92 上已运行。管理端验证码只关 92 Nacos `security.captcha.enabled`。

---

## Spec 覆盖自检

| spec 节 | 任务 |
|---------|------|
| 3.1 管理端列表/详情/三态/文案 | 3、4 |
| 3.2 上传/提交/限流/身份 | 2、5、6 |
| 3.3 设置页字段/压缩/顶栏设置 | 7 |
| 3.4 PHASE2 C-07 | 7 |
| 4.1 表/字典/种子 | 1 |
| 4.2–4.4 API/Inner/proto | 3、5 |
| 5–6 前端路径/菜单 | 1、4 |
| 7.3 guest/login + 选图 | 8 |
| 7.2 gate/deploy/accept | 9 |
| 不做项 | Global Constraints |

无 TBD。实现前必须听到人说「批准」或「按这个做」。
