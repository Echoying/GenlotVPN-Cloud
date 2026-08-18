# 最低客户端版本拦截 Implementation Plan

> **For agentic workers:** 按本计划逐任务实现。步骤用 checkbox（`- [ ]`）跟踪。本仓库门禁以 `pipeline/bin/gate.py` / `deploy.py` / `accept.py` 为准；纯逻辑用 JUnit。**未批书面 spec 不写业务代码**（本条 spec 已批准）。不要自动 `git commit`，除非用户明确要求提交。

**Goal:** 超管可配客户端最低版本策略；桌面端云端登录页过低/缺版本被 vpn-auth 硬拦（426 + 下载链接）；客户端弹窗引导升级。不拦易安联 SDK / 本地控制器登录。

**Architecture:** yianlian 单行表 `vpn_client_version_policy` + 管理 CRUD；内部 Feign 供 vpn-auth 读取。`LOGIN` 在验证码校验前做版本门禁。`LoginRequest` 带 `client_version`/`client_platform`；失败 `RpcResponse.code=426` 且 `data=ClientVersionReject`。客户端解析后弹窗，可打开下载 URL。

**Tech Stack:** Java 8、Spring Cloud OpenFeign、MyBatis、Protobuf（`ruoyi-vpn-protocol`）、Vue 2 + Element UI、Qt 6 客户端、Playwright accept、JUnit 4。

**Spec:** [spec.md](./spec.md)

## Global Constraints

- 拦截点仅云端 `MessageType.LOGIN`（桌面端登录页）；禁止改控制器 / SDK 登录路径
- 不改 TLS / 证书 Pin / 帧 `VpnFrameConstants.VERSION`
- 文案固定：页面标题 `客户端版本策略`；失败 `msg`=`客户端版本过低，请升级至 {min_version} 或以上`；弹窗标题 `需要升级客户端`；按钮 `打开下载页`；保存成功 `保存成功`
- 权限：`vpn:clientVersion:query` / `vpn:clientVersion:edit`；仅 `role_id=1`
- 版本格式 `x.y.z`；平台值 `windows` | `macos` | `unknown`
- 默认 `enabled=0`；菜单 ID 从 **1089** 起（1085–1088 已占用）
- 安全级：M1=S2；M2/M3=S1；客户端不自动 accept
- 门禁命令见 spec 第 7 节；提交：仅当用户明确说「提交」时才 `git commit`

## 文件地图

| 文件 | 职责 |
|------|------|
| `sql/update/20260816_client_version_policy.sql` | 建表 + 默认行 + 菜单 1089–1091 + role_menu |
| `.../domain/VpnClientVersionPolicy.java` | 策略实体 |
| `.../mapper/VpnClientVersionPolicyMapper.java` + XML | 按 id=1 查询/更新 |
| `.../service/vpn/ClientVersionComparator.java` | 纯逻辑：合法版本、比较 |
| `.../service/vpn/IVpnClientVersionPolicyService.java` + impl | 读/写、校验、缓存失效 |
| `.../controller/VpnClientVersionController.java` | GET/PUT 管理端 + GET inner |
| `ruoyi-api-yianlian/.../VpnClientVersionPolicyDTO.java` | Feign 传输对象 |
| `ruoyi-api-yianlian/.../RemoteVpnClientVersionService.java` + Fallback | vpn-auth 读策略 |
| `proto/vpn/auth.proto` | `client_version`/`client_platform` + `ClientVersionReject` |
| `ruoyi-vpn-auth/.../TcpRpcDispatcher.java` | LOGIN 门禁；`RpcResult.fail(code,msg,data)` |
| `ruoyi-vpn-auth/.../VpnClientVersionGateService.java` | Feign + 比对 + 组装 Reject |
| `ruoyi-ui/src/api/vpn/clientVersion.js` | 管理端 API |
| `ruoyi-ui/src/views/vpn/clientVersion/index.vue` | 策略表单页 |
| `ruoyi-vpn-client/.../ClientDeviceInfo.*` | `platformId()` |
| `ruoyi-vpn-client/.../VpnCloudService.*` | 登录填版本；426 → 专用信号 |
| `ruoyi-vpn-client/.../VpnFlowController.*` | 弹窗 + 打开 URL |
| `ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md` | 人验条目 |
| `.../ClientVersionComparatorTest.java` | JUnit |

---

### Task 1: ClientVersionComparator（纯逻辑）

**Files:**
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/ClientVersionComparator.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/test/java/com/ruoyi/yianlian/service/vpn/ClientVersionComparatorTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `ClientVersionComparator.isValid(String version) -> boolean`（匹配 `^\d+\.\d+\.\d+$`）
  - `ClientVersionComparator.compare(String a, String b) -> int`（非法抛 `IllegalArgumentException`；返回负/0/正）
  - `ClientVersionComparator.isLower(String client, String min) -> boolean`（`compare(client,min) < 0`）

- [x] **Step 1: 写会失败的测试**

```java
package com.ruoyi.yianlian.service.vpn;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClientVersionComparatorTest
{
    @Test
    public void isValid_acceptsSemver()
    {
        assertTrue(ClientVersionComparator.isValid("1.2.0"));
        assertFalse(ClientVersionComparator.isValid("1.2"));
        assertFalse(ClientVersionComparator.isValid(""));
        assertFalse(ClientVersionComparator.isValid(null));
    }

    @Test
    public void compare_numericSegments()
    {
        assertTrue(ClientVersionComparator.compare("1.2.0", "1.2.0") == 0);
        assertTrue(ClientVersionComparator.compare("1.2.0", "1.10.0") < 0);
        assertTrue(ClientVersionComparator.isLower("1.1.9", "1.2.0"));
        assertFalse(ClientVersionComparator.isLower("1.2.0", "1.2.0"));
    }
}
```

- [x] **Step 2: 跑测试，确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-yianlian -Dtest=ClientVersionComparatorTest test`

Expected: 编译失败（类不存在）。

- [x] **Step 3: 写最小实现**

```java
package com.ruoyi.yianlian.service.vpn;

import java.util.regex.Pattern;

public final class ClientVersionComparator
{
    private static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    private ClientVersionComparator() {}

    public static boolean isValid(String version)
    {
        return version != null && SEMVER.matcher(version.trim()).matches();
    }

    public static int compare(String a, String b)
    {
        if (!isValid(a) || !isValid(b))
        {
            throw new IllegalArgumentException("版本号必须为 x.y.z");
        }
        String[] pa = a.trim().split("\\.");
        String[] pb = b.trim().split("\\.");
        for (int i = 0; i < 3; i++)
        {
            int diff = Integer.parseInt(pa[i]) - Integer.parseInt(pb[i]);
            if (diff != 0)
            {
                return diff < 0 ? -1 : 1;
            }
        }
        return 0;
    }

    public static boolean isLower(String client, String min)
    {
        return compare(client, min) < 0;
    }
}
```

- [x] **Step 4: 跑测试，确认通过**

Run: `mvn -pl ruoyi-modules/ruoyi-yianlian -Dtest=ClientVersionComparatorTest test`

Expected: BUILD SUCCESS，测试通过。

- [ ] **Step 5: 提交（仅用户要求时）** — 跳过除非用户说提交。

---

### Task 2: 策略表 + yianlian 读写 API（含 Inner）

**Files:**
- Create: `sql/update/20260816_client_version_policy.sql`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/domain/VpnClientVersionPolicy.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/mapper/VpnClientVersionPolicyMapper.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/resources/mapper/yianlian/VpnClientVersionPolicyMapper.xml`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/IVpnClientVersionPolicyService.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/impl/VpnClientVersionPolicyServiceImpl.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/controller/VpnClientVersionController.java`
- Create: `ruoyi-api/ruoyi-api-yianlian/src/main/java/com/ruoyi/yianlian/api/domain/VpnClientVersionPolicyDTO.java`
- Create: `ruoyi-api/ruoyi-api-yianlian/src/main/java/com/ruoyi/yianlian/api/RemoteVpnClientVersionService.java`
- Create: `ruoyi-api/ruoyi-api-yianlian/src/main/java/com/ruoyi/yianlian/api/factory/RemoteVpnClientVersionFallbackFactory.java`

**Interfaces:**
- Consumes: `ClientVersionComparator`
- Produces:
  - `IVpnClientVersionPolicyService.getPolicy() -> VpnClientVersionPolicy`
  - `IVpnClientVersionPolicyService.updatePolicy(VpnClientVersionPolicy) -> int`（校验；清缓存）
  - `GET /vpn/clientVersion`、`PUT /vpn/clientVersion`、`GET /vpn/clientVersion/inner`
  - Feign: `RemoteVpnClientVersionService.getPolicy(String source) -> R<VpnClientVersionPolicyDTO>`

- [x] **Step 1: 写 SQL（可重入）**

```sql
-- 客户端版本策略（可重入）
CREATE TABLE IF NOT EXISTS vpn_client_version_policy (
  id bigint NOT NULL COMMENT '主键，固定为1',
  enabled char(1) NOT NULL DEFAULT '0' COMMENT '0关 1开',
  min_version varchar(32) DEFAULT '' COMMENT '最低版本 x.y.z',
  download_url_windows varchar(512) DEFAULT '' COMMENT 'Windows下载链接',
  download_url_macos varchar(512) DEFAULT '' COMMENT 'macOS下载链接',
  create_by varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPN客户端版本策略';

INSERT INTO vpn_client_version_policy (id, enabled, min_version, download_url_windows, download_url_macos, create_by, create_time, remark)
SELECT 1, '0', '', '', '', 'admin', NOW(), '默认关闭拦截'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vpn_client_version_policy WHERE id = 1);

-- 菜单 1089 C / 1090 F query / 1091 F edit（1085-1088 已占用）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1089, '客户端版本策略', 1061, 11, 'clientVersion', 'vpn/clientVersion/index', NULL, '', 1, 0, 'C', '0', '0', 'vpn:clientVersion:query', 'education', 'admin', NOW(), '最低客户端版本拦截策略'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1089 OR perms = 'vpn:clientVersion:query' AND menu_type = 'C');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1090, '版本策略查询', 1089, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:clientVersion:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1090);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1091, '版本策略修改', 1089, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:clientVersion:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1091);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (1089, 1090, 1091)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
```

- [x] **Step 2: 实体 + Mapper + Service + Controller**

要点（对照现有 `Vpn*` 风格）：

- 实体字段：`id`、`enabled`、`minVersion`、`downloadUrlWindows`、`downloadUrlMacos` + `BaseEntity`
- Mapper：`selectById(1L)`、`update(VpnClientVersionPolicy)`
- Service `updatePolicy`：
  - `enabled` 只能是 `0`/`1`
  - 若 `enabled=1`：`minVersion` 必须 `ClientVersionComparator.isValid`
  - URL 非空时须以 `http://` 或 `https://` 开头（忽略大小写）
  - 使用 `RedisService`：缓存键 `vpn:clientVersion:policy`，TTL **60 秒**；`getPolicy` 先读缓存；`updatePolicy` 成功后 `deleteObject`
- Controller：

```java
@RestController
@RequestMapping("/vpn/clientVersion")
public class VpnClientVersionController extends BaseController
{
    @RequiresPermissions("vpn:clientVersion:query")
    @GetMapping
    public AjaxResult get() { return success(service.getPolicy()); }

    @RequiresPermissions("vpn:clientVersion:edit")
    @Log(title = "客户端版本策略", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VpnClientVersionPolicy policy)
    {
        return toAjax(service.updatePolicy(policy));
    }

    @InnerAuth
    @GetMapping("/inner")
    public R<VpnClientVersionPolicyDTO> inner()
    {
        VpnClientVersionPolicy p = service.getPolicy();
        // 手工拷贝到 DTO 后 return R.ok(dto);
    }
}
```

- [x] **Step 3: Feign 契约**

```java
@FeignClient(contextId = "remoteVpnClientVersionService",
        value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteVpnClientVersionFallbackFactory.class)
public interface RemoteVpnClientVersionService
{
    @GetMapping("/vpn/clientVersion/inner")
    R<VpnClientVersionPolicyDTO> getPolicy(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
```

DTO 字段与实体业务字段一致（`enabled`、`minVersion`、`downloadUrlWindows`、`downloadUrlMacos`）。Fallback 返回 `R.fail("获取客户端版本策略失败:" + throwable.getMessage())`。

- [x] **Step 4: 本机构建模块**

Run: `mvn clean package -pl ruoyi-modules/ruoyi-yianlian -am -DskipTests`

Expected: BUILD SUCCESS（含 `ruoyi-api-yianlian`）。

---

### Task 3: 管理端表单页（M1 UI）

**Files:**
- Create: `ruoyi-ui/src/api/vpn/clientVersion.js`
- Create: `ruoyi-ui/src/views/vpn/clientVersion/index.vue`

**Interfaces:**
- Consumes: `GET/PUT /vpn/clientVersion`
- Produces: 动态路由页，标题文案含 `客户端版本策略`

- [x] **Step 1: API**

```js
import request from '@/utils/request'

// 前端 request 须带服务前缀 /yianlian（对照 localUser.js）
export function getClientVersionPolicy() {
  return request({ url: '/yianlian/vpn/clientVersion', method: 'get' })
}

export function updateClientVersionPolicy(data) {
  return request({ url: '/yianlian/vpn/clientVersion', method: 'put', data })
}
```

- [x] **Step 2: 页面**

单页表单（非列表）。必须包含可见标题文本 **客户端版本策略**（可用 `<el-card>` header 或页面内 `<h`/`div`）。

字段 label 与 spec 一致：`启用版本拦截`、`最低客户端版本`、`Windows 下载链接`、`macOS 下载链接`、`备注`。

- `el-switch` 绑定 `enabled`，active-value=`'1'` inactive-value=`'0'`
- 保存按钮：`v-hasPermi="['vpn:clientVersion:edit']"`，文案 **保存**
- `created`/`mounted` 调 `getClientVersionPolicy` 填表
- 保存成功：`this.$modal.msgSuccess("保存成功")` 后重新 get

校验：开启时 `minVersion` 必填且 `/^\d+\.\d+\.\d+$/`；URL 可空，非空须 `/^https?:\/\//i`。

- [ ] **Step 3: 前端构建自检（可选本机）**

Run: `cd ruoyi-ui && npm run build:prod`（或等 Task 7 的 gate）

Expected: 产物含新页 chunk，无编译错误。

---

### Task 4: Proto + vpn-auth 登录硬拦（M2）

**Files:**
- Modify: `proto/vpn/auth.proto`
- Modify: `ruoyi-vpn-auth/src/main/java/com/ruoyi/vpn/auth/tcp/TcpRpcDispatcher.java`（`RpcResult` + `handleLogin`）
- Create: `ruoyi-vpn-auth/src/main/java/com/ruoyi/vpn/auth/service/VpnClientVersionGateService.java`
- Ensure: `ruoyi-vpn-auth` 已依赖 `ruoyi-api-yianlian`（已有则不改 pom）

**Interfaces:**
- Consumes: `RemoteVpnClientVersionService`、`ClientVersionComparator` 逻辑（**在 vpn-auth 内复制一份同名工具类** `com.ruoyi.vpn.auth.util.ClientVersionComparator`，避免 vpn-auth 依赖 yianlian 实现模块；保持算法与 Task 1 一致）
- Produces: 开启时拒绝返回 `code=426` + `ClientVersionReject`

- [x] **Step 1: 改 proto**

在 `LoginRequest` 末尾增加：

```protobuf
  string client_version = 11;
  string client_platform = 12;
```

在 `auth.proto` 增加：

```protobuf
message ClientVersionReject {
  string min_version = 1;
  string download_url = 2;
  string client_version = 3;
}
```

- [x] **Step 2: 重新生成 Java protobuf**

Run: `mvn clean install -pl ruoyi-vpn-protocol -am -DskipTests`

Expected: 生成含新字段的类。

- [x] **Step 3: 扩展 RpcResult**

```java
public static RpcResult fail(int code, String msg, com.google.protobuf.Message data)
{
    RpcResponse.Builder b = RpcResponse.newBuilder().setCode(code).setMsg(msg == null ? "" : msg);
    if (data != null)
    {
        b.setData(data.toByteString());
    }
    return new RpcResult(b.build());
}
```

保留原 `fail(String msg)` → `fail(500, msg, null)`。

- [x] **Step 4: VpnClientVersionGateService**

```java
@Service
public class VpnClientVersionGateService
{
    @Autowired
    private RemoteVpnClientVersionService remoteVpnClientVersionService;

    /** @return null 表示放行；非 null 为应返回的 RpcResult */
    public TcpRpcDispatcher.RpcResult check(String clientVersion, String clientPlatform)
    {
        R<VpnClientVersionPolicyDTO> r = remoteVpnClientVersionService.getPolicy(SecurityConstants.INNER);
        if (r == null || r.getCode() != 200 || r.getData() == null)
        {
            // 读策略失败：为免全站无法登录，记日志并放行（或按你们运维偏好改为拒绝；本计划默认放行并 warn）
            return null;
        }
        VpnClientVersionPolicyDTO p = r.getData();
        if (!"1".equals(p.getEnabled()))
        {
            return null;
        }
        String min = p.getMinVersion() == null ? "" : p.getMinVersion().trim();
        String cv = clientVersion == null ? "" : clientVersion.trim();
        boolean reject = !ClientVersionComparator.isValid(cv)
                || !ClientVersionComparator.isValid(min)
                || ClientVersionComparator.isLower(cv, min);
        if (!reject)
        {
            return null;
        }
        String url = resolveUrl(p, clientPlatform);
        String msg = "客户端版本过低，请升级至 " + min + " 或以上";
        ClientVersionReject data = ClientVersionReject.newBuilder()
                .setMinVersion(min)
                .setDownloadUrl(url == null ? "" : url)
                .setClientVersion(cv)
                .build();
        return TcpRpcDispatcher.RpcResult.fail(426, msg, data);
    }

    private String resolveUrl(VpnClientVersionPolicyDTO p, String platform)
    {
        if ("windows".equalsIgnoreCase(platform)) return p.getDownloadUrlWindows();
        if ("macos".equalsIgnoreCase(platform)) return p.getDownloadUrlMacos();
        return "";
    }
}
```

- [x] **Step 5: 接入 handleLogin**

在 `handleLogin` 中，**captcha 校验之前**插入：

```java
RpcResult versionReject = vpnClientVersionGateService.check(
        req.getClientVersion(), req.getClientPlatform());
if (versionReject != null)
{
    return versionReject;
}
```

不要写入控制器登录、不要改 `SessionPing`。

- [x] **Step 6: 构建 vpn-auth**

Run: `mvn clean package -pl ruoyi-vpn-auth -am -DskipTests`

Expected: BUILD SUCCESS。

---

### Task 5: 桌面端上报 + 426 弹窗（M3）

**Files:**
- Modify: `ruoyi-vpn-client/src/core/ClientDeviceInfo.h` / `.cpp` — 增加 `platformId()`
- Modify: `ruoyi-vpn-client/src/core/VpnCloudService.h` / `.cpp` — 登录填字段；426 专用信号
- Modify: `ruoyi-vpn-client/src/core/VpnFlowController.cpp`（及 `.h` 若需）— 弹窗
- Modify: `ruoyi-vpn-client/i18n/genlotvpn_zh_CN.ts` / `genlotvpn_en.ts` — 新文案
- Modify: `ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md` — 人验条目

**Interfaces:**
- Consumes: `AppInfo.version()`、`ClientVersionReject`
- Produces: signal `versionUpgradeRequired(QString message, QString downloadUrl)`

- [x] **Step 1: platformId**

```cpp
QString ClientDeviceInfo::platformId()
{
#if defined(Q_OS_WIN)
    return QStringLiteral("windows");
#elif defined(Q_OS_MACOS)
    return QStringLiteral("macos");
#else
    return QStringLiteral("unknown");
#endif
}
```

- [x] **Step 2: login 填充 + 失败分支**

在 `login()` 的 `LoginRequest` 上：

```cpp
req.set_client_version(AppInfo().version().toStdString()); // 或注入已有 AppInfo 实例
req.set_client_platform(ClientDeviceInfo::platformId().toStdString());
```

`login` 改用带 `onFailure` 的 `sendRpc` **或**在 `sendRpcWithRetry` 失败路径识别：当 `messageType==LOGIN && result.code==426` 时，解析 `ClientVersionReject`，`emit versionUpgradeRequired(msg, url)`，**不要**再走普通 `requestFailed`（避免只显示红字无链接）。其它失败仍 `requestFailed`。

解析示例：

```cpp
vpn::ClientVersionReject rej;
if (rej.ParseFromArray(result.data.constData(), result.data.size())) {
    emit versionUpgradeRequired(result.msg,
        QString::fromStdString(rej.download_url()));
}
```

- [x] **Step 3: VpnFlowController 弹窗**

连接 `versionUpgradeRequired`：

- `m_loginPending = false`; `setLoading(false)`
- `setLoginError(message)`（登录页仍可见错误）
- 使用 `QMessageBox`（或现有 toast + 自定义 Dialog）：标题 `tr("需要升级客户端")`，正文 `message`；若 `downloadUrl` 非空，加按钮 `tr("打开下载页")`，点击 `QDesktopServices::openUrl(QUrl(downloadUrl))`
- 刷新验证码：`m_cloud->fetchCaptcha()`
- **禁止**继续 `fetchAuthorizedLines` / 控制器登录

- [x] **Step 4: i18n**

为 `需要升级客户端`、`打开下载页` 写入 zh/en `.ts`（按仓库现有 lupdate 流程；至少源码 `tr()` 字符串正确）。

- [x] **Step 5: PHASE2 条目**

在 `PHASE2_CHECKLIST.md` 合适小节增加一行，例如：

| [ ] | S-xx | 最低客户端版本拦截 | 管理端开启后低/空版本无法云端登录，弹窗含最低版本与下载链接；关闭后可登录；控制器/SDK 登录不被拦 | 人验 Win/Mac |

- [x] **Step 6: 客户端编译**

Run: 由 Task 7 `gate.py` 覆盖；本机可 `bin\build-vpn-client.bat --package`（Windows）。

Expected: 编译通过。

---

### Task 6:（可选自检）版本门禁联调清单

不写自动测试。实现者本地勾选：

- [ ] SQL 已应用到 91；管理端能打开「客户端版本策略」并保存
- [ ] `enabled=1`，`min_version` 高于当前客户端 → 登录页登录失败，文案含最低版本；有 Win/Mac URL 时按钮可开浏览器
- [ ] `enabled=0` → 同客户端可登录
- [ ] 登录成功后控制器/SDK 流程不被本策略打断

---

### Task 7: 门禁 gate → deploy → SQL → accept

**Files:**
- Append: `pipeline/work/2026-08-16-最低客户端版本拦截/质量门禁.md`（脚本自动追加）
- Append: `pipeline/work/_shared/质量门禁记录.md`（若流水线惯例要求）

- [x] **Step 1: gate（含客户端）**

```bash
python -u pipeline/bin/gate.py --run 2026-08-16-最低客户端版本拦截
```

Expected: BUILD SUCCESS；客户端打包成功。

- [x] **Step 2: deploy（含 93 vpn-auth）**

```bash
python -u pipeline/bin/deploy.py --run 2026-08-16-最低客户端版本拦截
```

Expected: 92/93 相关容器 jar 一致且 Started。

- [x] **Step 3: 应用 SQL**

```bash
python -u pipeline/work/bin/apply_sql_91.py sql/update/20260816_client_version_policy.sql
```

Expected: 表与菜单存在。

- [x] **Step 4: accept M1**

```bash
python -u pipeline/bin/accept.py --run 2026-08-16-最低客户端版本拦截 \
  --path /yianlian/clientVersion \
  --expect-text 客户端版本策略 --title 客户端版本策略页 --module M1 --round 0
```

Expected: 退出码 0；截图进 `验收报告/`。

- [x] **Step 5: 钉钉** — accept 结束已自动发送。

- [x] **Step 6: 更新模块清单状态** — M1 已验收；M2/M3 待人验。

---

## Spec 覆盖自检

| Spec 项 | Task |
|---------|------|
| 独立表 + 默认关闭 | T2 |
| 管理端表单文案/权限 | T3 + T2 SQL |
| Inner Feign | T2 |
| LOGIN 硬拦 + 426 + Reject | T4 |
| 不拦 SDK | T4 范围 + T5 不改 ControllerService 登录成功路径 |
| 客户端带版本/平台 + 弹窗链接 | T5 |
| PHASE2 | T5 |
| gate/deploy/accept 命令 | T7 |
| ClientVersion 比较 `1.2` vs `1.10` | T1 |

## 占位符扫描

无 TBD/TODO；vpn-auth 内嵌 Comparator 已写明原因（不依赖 yianlian 实现 jar）。

## 类型一致性

- DB/Java：`minVersion` / `downloadUrlWindows` / `downloadUrlMacos` / `enabled`
- Proto：`client_version`、`client_platform`、`ClientVersionReject.*`
- 平台字符串：`windows` / `macos` / `unknown`
- 业务码：`426`
