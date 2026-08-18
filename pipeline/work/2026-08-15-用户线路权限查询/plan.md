# 用户线路权限查询 Implementation Plan

> **For agentic workers:** 按本计划逐任务实现。步骤用 checkbox（`- [ ]`）跟踪。本仓库无独立测试框架作门禁；纯逻辑用一条 JUnit，页面用 `bin/accept.bat`。未批书面 spec 不写业务代码。不要自动 `git commit`，除非用户明确要求提交。

**Goal:** 在线路用户、本地用户两个现有列表上提供只读「查看权限」弹窗，按线路展示应用组 / 应用及来源（部门 / 角色 / 用户）。

**Architecture:** 不改 `authorized-lines`。在 `IVpnLineAuthService` 增加只读汇总；`LineAuthMerger` 负责把多条 `Yal*Auth` 合成 spec 中的 `lines/items/sources`。两个 Controller 各加一条 GET。前端只加弹窗和按钮。菜单只加两条 `F`。

**Tech Stack:** Java 8、Spring Boot 2.7、MyBatis、Vue 2 + Element UI、Playwright（`bin/accept_admin.py`）。

**Spec:** [spec-2026-08-15-用户线路权限查询.md](./spec-2026-08-15-用户线路权限查询.md)

## Global Constraints

- 安全级 S2；接口必须登录鉴权，禁止 `@InnerAuth`，禁止返回 `spaKey` / 密码 / `encryptedPwd`
- 来源文案只能是：`部门`、`角色`、`用户`
- 弹窗标题：`线路权限 - {userName}`；空列表正文：`暂无线路权限`
- 按钮文案：`查看权限`；权限标识：`yianlian:user:queryAuth`、`vpn:localUser:queryAuth`
- 不改 `toAuthorizedLineVos`、不写易安联、不改客户端
- 应用名称用 `VpnService.name`（不是非 DB 字段 `appName`）；应用组用 `VpnServiceGroup.groupName`；线路用 `LineApp.appName`
- `Yal*Auth.appIds` / `appGroupIds` 是本地主键、逗号分隔（与现有授权弹窗 `service_` / `group_` 去前缀后写入的值一致）
- 门禁：`bin/gate.bat server` → `bin/deploy.bat` → 两次 accept；本条不编客户端
- 提交：仅当用户明确说「提交」时才 `git commit`

## 文件地图

| 文件 | 职责 |
|------|------|
| `.../domain/vo/VpnLineAuthView.java` | 接口 `data` 根对象 |
| `.../domain/vo/VpnLineAuthLineView.java` | 一条线路 |
| `.../domain/vo/VpnLineAuthItemView.java` | 一行应用/组 + sources |
| `.../service/vpn/LineAuthMerger.java` | 无 Spring：合并来源、拆逗号 ID |
| `.../service/vpn/IVpnLineAuthService.java` | 增加 `buildLineAuthView` / `buildLocalLineAuthView` |
| `.../service/vpn/impl/VpnLineAuthServiceImpl.java` | 查库、解析名称、调 Merger |
| `.../controller/VpnUserController.java` | `GET /{userId}/line-auths` |
| `.../controller/VpnLocalUserController.java` | `GET /{localUserId}/line-auths` |
| `sql/update/20260815_line_auth_query_menu.sql` | 两条 F 菜单 + admin 角色 |
| `ruoyi-ui/src/api/vpn/user.js` | `getUserLineAuths` |
| `ruoyi-ui/src/api/vpn/localUser.js` | `getLocalUserLineAuths` |
| `ruoyi-ui/src/views/vpn/user/index.vue` | 更多 → 查看权限 + 弹窗 |
| `ruoyi-ui/src/views/vpn/local/user/index.vue` | 操作列查看权限 + 弹窗 |
| `bin/accept_admin.py` | `--click` 可重复，依次点文案 |

---

### Task 1: LineAuthMerger（纯逻辑）

**Files:**
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/LineAuthMerger.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/test/java/com/ruoyi/yianlian/service/vpn/LineAuthMergerTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `LineAuthMerger.splitIds(String csv) -> List<String>`
  - `LineAuthMerger.itemKey(String appGroupId, String appId) -> String`
  - `LineAuthMerger.mergeSource(Map<String, LinkedHashSet<String>> acc, String key, String source)`
  - `LineAuthMerger.sourcesOf(Map<String, LinkedHashSet<String>> acc, String key) -> List<String>`

- [ ] **Step 1: 写会失败的测试**

```java
package com.ruoyi.yianlian.service.vpn;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;

public class LineAuthMergerTest
{
    @Test
    public void splitIds_skipsBlank()
    {
        List<String> ids = LineAuthMerger.splitIds("1, 2,,3");
        assertEquals(3, ids.size());
        assertEquals("1", ids.get(0));
    }

    @Test
    public void mergeSource_dedupsAndKeepsOrder()
    {
        Map<String, LinkedHashSet<String>> acc = new LinkedHashMap<String, LinkedHashSet<String>>();
        String key = LineAuthMerger.itemKey("10", "20");
        LineAuthMerger.mergeSource(acc, key, "部门");
        LineAuthMerger.mergeSource(acc, key, "用户");
        LineAuthMerger.mergeSource(acc, key, "部门");
        List<String> sources = LineAuthMerger.sourcesOf(acc, key);
        assertEquals("部门", sources.get(0));
        assertEquals("用户", sources.get(1));
        assertEquals(2, sources.size());
    }
}
```

- [ ] **Step 2: 跑测试，确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-yianlian -Dtest=LineAuthMergerTest test`

Expected: 编译失败或 `LineAuthMerger` 找不到。

- [ ] **Step 3: 写最小实现**

```java
package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.utils.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class LineAuthMerger
{
    private LineAuthMerger() {}

    public static List<String> splitIds(String csv)
    {
        List<String> ids = new ArrayList<String>();
        if (StringUtils.isEmpty(csv))
        {
            return ids;
        }
        for (String part : csv.split(","))
        {
            if (StringUtils.isNotEmpty(part) && StringUtils.isNotEmpty(part.trim()))
            {
                ids.add(part.trim());
            }
        }
        return ids;
    }

    public static String itemKey(String appGroupId, String appId)
    {
        String g = appGroupId == null ? "" : appGroupId;
        String a = appId == null ? "" : appId;
        return g + "|" + a;
    }

    public static void mergeSource(Map<String, LinkedHashSet<String>> acc, String key, String source)
    {
        LinkedHashSet<String> set = acc.get(key);
        if (set == null)
        {
            set = new LinkedHashSet<String>();
            acc.put(key, set);
        }
        set.add(source);
    }

    public static List<String> sourcesOf(Map<String, LinkedHashSet<String>> acc, String key)
    {
        LinkedHashSet<String> set = acc.get(key);
        return set == null ? new ArrayList<String>() : new ArrayList<String>(set);
    }
}
```

- [ ] **Step 4: 再跑测试**

Run: `mvn -pl ruoyi-modules/ruoyi-yianlian -Dtest=LineAuthMergerTest test`

Expected: Tests run: 2, Failures: 0

- [ ] **Step 5: 提交（仅用户要求时）**

```
git add ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/LineAuthMerger.java ruoyi-modules/ruoyi-yianlian/src/test/java/com/ruoyi/yianlian/service/vpn/LineAuthMergerTest.java
git commit -m "feat: 线路权限查看来源合并逻辑"
```

---

### Task 2: VO + IVpnLineAuthService 汇总

**Files:**
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/domain/vo/VpnLineAuthView.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/domain/vo/VpnLineAuthLineView.java`
- Create: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/domain/vo/VpnLineAuthItemView.java`
- Modify: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/IVpnLineAuthService.java`
- Modify: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/service/vpn/impl/VpnLineAuthServiceImpl.java`

**Interfaces:**
- Consumes: `LineAuthMerger.*`；`IVpnUserService.selectUserById` / `selectUsersByLocalUserId`；`IVpnLocalUserService.selectLocalUserById`；`IVpnLineAppService.selectLineAppById`；`IVpnServiceService.selectServiceById`；`IVpnServiceGroupService.selectServiceGroupById`；现有三个 `Yal*AuthMapper`
- Produces:
  - `VpnLineAuthView buildLineAuthView(Long userId)` — 用户不存在抛 `ServiceException("用户不存在")`
  - `VpnLineAuthView buildLocalLineAuthView(Long localUserId)` — 本地用户不存在抛 `ServiceException("用户不存在")`；无同步线路则 `lines` 空列表，不抛错
  - `VpnLineAuthView` 字段：`Long userId`、`String userName`、`String userType`（`line` 或 `local`）、`List<VpnLineAuthLineView> lines`
  - `VpnLineAuthLineView`：`String lineId`、`String lineName`、`List<VpnLineAuthItemView> items`
  - `VpnLineAuthItemView`：`String appGroupId`、`String appGroupName`、`String appId`、`String appName`、`List<String> sources`
  - 禁止往 View 里放 `spaKey`

- [ ] **Step 1: 三个 VO**

字段按上面 Interfaces，JavaBean getter/setter（与模块内其他 vo 一致，可用普通 class，不必上 Lombok）。`userType` 线路用户写 `"line"`，本地用户写 `"local"`。

- [ ] **Step 2: 接口加方法**

在 `IVpnLineAuthService` 末尾追加：

```java
VpnLineAuthView buildLineAuthView(Long userId);

VpnLineAuthView buildLocalLineAuthView(Long localUserId);
```

- [ ] **Step 3: 实现 `buildLineAuthView`**

注入（已有 mapper 保留）：`IVpnUserService`、`IVpnLocalUserService`、`IVpnLineAppService`、`IVpnServiceService`、`IVpnServiceGroupService`。

算法：

1. `VpnUser user = userService.selectUserById(userId)`；`user == null` 则 `throw new ServiceException("用户不存在")`。
2. 若 `user.getRoles()` 为空，用 `IVpnRoleService.selectUserRolesByUserId(userId)` 补上（`selectUserById` 的 XML 虽有 roles collection，缺关联时可能为空）。
3. 建 `Map<String, Map<String, LinkedHashSet<String>>>`：外键 `lineId`，内键 `LineAuthMerger.itemKey(groupId, appId)`。
4. 部门：`yalDeptAuthMapper.selectYalDeptAuthByDeptId(user.getDeptId())`，对每条 auth 的每个 groupId、每个 appId 调 `mergeSource(..., "部门")`。只处理 `matchUserLine(user.getAppId(), auth.getLineId())` 为 true 的授权（与现网选线逻辑一致）。
5. 角色：对每个 role 的 `selectYalRoleAuthByRoleId`，来源 `"角色"`。
6. 用户：`selectYalUserAuthByUserId`，来源 `"用户"`。
7. 仅有应用组、没有应用时：`itemKey(groupId, "")`，`appName` 空串。
8. 组装 `VpnLineAuthView`：`userType="line"`，`userName=user.getUserName()`。每条 line：`lineName` 来自 `lineAppService.selectLineAppById(lineId)`，找不到则用 `lineId`。item 的名称：`selectServiceGroupById` / `selectServiceById`，找不到则用原始 ID。`Long.parseLong` 失败则名称=原始 ID。
9. 不要调用 `toAuthorizedLineVos`。

- [ ] **Step 4: 实现 `buildLocalLineAuthView`**

1. `VpnLocalUser local = localUserService.selectLocalUserById(localUserId)`；空则 `ServiceException("用户不存在")`。
2. `List<VpnUser> lineUsers = userService.selectUsersByLocalUserId(localUserId)`。
3. 对每个线路用户调用与 Step 3 相同的私有方法 `fillFromVpnUser(VpnUser, acc)`，再合成一个 View：`userType="local"`，`userId=localUserId`，`userName=local.getUserName()`。
4. `lineUsers` 空：返回 `lines=[]`，不要失败。

- [ ] **Step 5: 编译**

Run: `mvn -pl ruoyi-modules/ruoyi-yianlian -am -DskipTests compile`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交（仅用户要求时）**

```
git commit -m "feat: 用户线路权限只读汇总服务"
```

---

### Task 3: 两条管理端 GET

**Files:**
- Modify: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/controller/VpnUserController.java`
- Modify: `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/controller/VpnLocalUserController.java`

**Interfaces:**
- Consumes: `IVpnLineAuthService.buildLineAuthView` / `buildLocalLineAuthView`
- Produces:
  - `GET /vpn/user/{userId}/line-auths` → `AjaxResult.success(view)`，`@RequiresPermissions("yianlian:user:queryAuth")`
  - `GET /vpn/local/user/{localUserId}/line-auths` → `AjaxResult.success(view)`，`@RequiresPermissions("vpn:localUser:queryAuth")`

- [ ] **Step 1: 线路用户接口**

插在 `VpnUserController` 里 `authorized-lines` **旁边但不要改它**。不要加 `@InnerAuth`。

```java
@RequiresPermissions("yianlian:user:queryAuth")
@GetMapping("/{userId}/line-auths")
public AjaxResult lineAuths(@PathVariable("userId") Long userId)
{
    return success(lineAuthService.buildLineAuthView(userId));
}
```

注意：`/{userId}/line-auths` 不要和现有 `GET /{userId}` 冲突（更具体的路径优先）。若启动后路由歧义，改成 `@GetMapping("/line-auths/{userId}")`，并同步改前端 URL（以能区分为准，优先 spec 的 `/{id}/line-auths`）。

- [ ] **Step 2: 本地用户接口**

```java
@RequiresPermissions("vpn:localUser:queryAuth")
@GetMapping("/{localUserId}/line-auths")
public AjaxResult lineAuths(@PathVariable("localUserId") Long localUserId)
{
    return success(lineAuthService.buildLocalLineAuthView(localUserId));
}
```

- [ ] **Step 3: 编译**

Run: `mvn -pl ruoyi-modules/ruoyi-yianlian -am -DskipTests compile`

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交（仅用户要求时）**

```
git commit -m "feat: 线路权限只读查询接口"
```

---

### Task 4: 菜单按钮权限 SQL

**Files:**
- Create: `sql/update/20260815_line_auth_query_menu.sql`

**Interfaces:**
- Consumes: 父菜单 `1065`、`1072`；admin `role_id=1`
- Produces: `menu_id=1087` `yianlian:user:queryAuth`；`menu_id=1088` `vpn:localUser:queryAuth`（1085/1086 已被同步任务占用）

- [ ] **Step 1: 可重入 SQL**

```sql
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1087, '查看线路权限', 1065, 8, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'yianlian:user:queryAuth', '#', 'admin', NOW(), '只读查看用户各线路权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1087 OR perms = 'yianlian:user:queryAuth');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1088, '查看线路权限', 1072, 8, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:queryAuth', '#', 'admin', NOW(), '只读查看本地用户各线路权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1088 OR perms = 'vpn:localUser:queryAuth');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN ('yianlian:user:queryAuth', 'vpn:localUser:queryAuth')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
```

- [ ] **Step 2: 提交（仅用户要求时）**

```
git commit -m "feat: 查看线路权限按钮菜单"
```

本机/92 执行放到 Task 8，不要在本任务 SSH。

---

### Task 5: 线路用户页弹窗

**Files:**
- Modify: `ruoyi-ui/src/api/vpn/user.js`
- Modify: `ruoyi-ui/src/views/vpn/user/index.vue`

**Interfaces:**
- Consumes: `GET /yianlian/vpn/user/{userId}/line-auths`
- Produces: 更多菜单「查看权限」；弹窗标题 `线路权限 - {userName}`；空数据展示 `暂无线路权限`

- [ ] **Step 1: API**

在 `user.js` 追加：

```javascript
export function getUserLineAuths(userId) {
  return request({
    url: '/yianlian/vpn/user/' + parseStrEmpty(userId) + '/line-auths',
    method: 'get'
  })
}
```

- [ ] **Step 2: 按钮**

在「授权」`el-dropdown-item` 后增加：

```html
<el-dropdown-item command="handleViewAuth" icon="el-icon-view" v-hasPermi="['yianlian:user:queryAuth']">查看权限</el-dropdown-item>
```

`handleCommand` 增加 `case "handleViewAuth": this.handleViewAuth(row)`。

下拉的 `v-hasPermi` 数组加上 `'yianlian:user:queryAuth'`，否则只有查询权时「更多」不出现。

- [ ] **Step 3: 弹窗与方法**

data 增加：`viewAuthOpen: false`、`viewAuthTitle: ''`、`viewAuthLines: []`、`viewAuthEmpty: false`。

```javascript
handleViewAuth(row) {
  getUserLineAuths(row.userId).then(res => {
    const data = res.data || {}
    this.viewAuthTitle = '线路权限 - ' + (data.userName || row.userName)
    this.viewAuthLines = data.lines || []
    this.viewAuthEmpty = !this.viewAuthLines.length
    this.viewAuthOpen = true
  }).catch(() => {
    this.$modal.msgError('加载线路权限失败')
  })
}
```

弹窗（只读，无 footer 提交）：

```html
<el-dialog :title="viewAuthTitle" :visible.sync="viewAuthOpen" width="720px" append-to-body>
  <div v-if="viewAuthEmpty">暂无线路权限</div>
  <div v-for="line in viewAuthLines" :key="line.lineId" style="margin-bottom:16px">
    <div style="font-weight:bold;margin-bottom:8px">{{ line.lineName || line.lineId }}</div>
    <el-table :data="line.items" size="mini" border>
      <el-table-column label="应用组" prop="appGroupName" />
      <el-table-column label="应用" prop="appName" />
      <el-table-column label="来源">
        <template slot-scope="scope">{{ (scope.row.sources || []).join(', ') }}</template>
      </el-table-column>
    </el-table>
  </div>
</el-dialog>
```

失败不打开弹窗。

- [ ] **Step 4: 提交（仅用户要求时）**

```
git commit -m "feat: 线路用户查看权限弹窗"
```

---

### Task 6: 本地用户页弹窗

**Files:**
- Modify: `ruoyi-ui/src/api/vpn/localUser.js`
- Modify: `ruoyi-ui/src/views/vpn/local/user/index.vue`

**Interfaces:**
- Consumes: `GET /yianlian/vpn/local/user/{localUserId}/line-auths`
- Produces: 与 Task 5 相同文案；按钮在操作列（「离线登录」和「更多」之间）

- [ ] **Step 1: API**

```javascript
export function getLocalUserLineAuths(localUserId) {
  return request({
    url: '/yianlian/vpn/local/user/' + parseStrEmpty(localUserId) + '/line-auths',
    method: 'get'
  })
}
```

- [ ] **Step 2: 按钮 + 弹窗**

操作列增加（`v-hasPermi="['vpn:localUser:queryAuth']"`）：

```html
<el-button size="mini" type="text" icon="el-icon-view" @click="handleViewAuth(scope.row)" v-hasPermi="['vpn:localUser:queryAuth']">查看权限</el-button>
```

列宽不够则把 `300` 改成 `360`。`handleViewAuth` 调 `getLocalUserLineAuths(row.localUserId)`，标题与空文案与 Task 5 相同（复制同一段 dialog 模板，不要抽公共组件，避免超范围重构）。

- [ ] **Step 3: 提交（仅用户要求时）**

```
git commit -m "feat: 本地用户查看权限弹窗"
```

---

### Task 7: accept 支持依次点击

**Files:**
- Modify: `bin/accept_admin.py`

**Interfaces:**
- Consumes: 现有登录 / 打开 path / `--expect-text`
- Produces: `--click` 可重复；打开目标页后按顺序点击，再断言文案

- [ ] **Step 1: 增加参数**

```python
p.add_argument("--click", action="append", default=[], help="打开目标页后依次点击的可见文案，可重复")
```

- [ ] **Step 2: 在打开 target 成功后、expect-text 前**

```python
for label in args.click:
    page.get_by_text(label, exact=True).first.click(timeout=8000)
    page.wait_for_timeout(400)
    checks.append(("点击「{}」".format(label), "通过"))
```

点不到则 `ok=False`，`error` 写明哪个文案。不要打印账号密码。

- [ ] **Step 3: 本机语法**

Run: `python bin/accept_admin.py --help`

Expected: 输出含 `--click`

- [ ] **Step 4: 提交（仅用户要求时）**

```
git commit -m "feat: 验收脚本支持依次点击"
```

---

### Task 8: 门禁、部署、92 菜单、页面验收

**Files:**
- Modify: `docs/work/质量门禁记录.md`（跑完后填一行）
- Modify: `docs/work/模块拆分清单.md`（M1–M3 改为已实现 / 已验收）
- 报告由 accept 写入 `docs/work/验收报告/`

**Interfaces:**
- Consumes: Task 1–7 全部产物；`.deploy/envs.yaml`；spec 第 7 节路径/文案
- Produces: 两份 accept 通过（或失败后 systematic-debugging ≤2 轮）

- [ ] **Step 1: 构建**

Run: `bin\gate.bat server`

Expected: Java + `ruoyi-ui` `build:prod` 成功。

- [ ] **Step 2: 部署 92**

Run: `bin\deploy.bat`

Expected: `[deploy] 完成`，退出码 0。

- [ ] **Step 3: 92 执行菜单 SQL**

在 92 的 MySQL（`ry-cloud`）执行 `sql/update/20260815_line_auth_query_menu.sql`。密码只走已有部署通道或本机手工，不要写进对话。执行后 `SELECT perms FROM sys_menu WHERE perms LIKE '%queryAuth%';` 应有两行。

admin 需重新登录（或清 Redis 用户权限缓存）后按钮才出现。

- [ ] **Step 4: 验收线路用户**

Run:

```
bin\accept.bat --path /yianlian/vpn/user --click 更多 --click 查看权限 --expect-text 线路权限 --title 线路用户线路权限 --module M2 --round 0
```

Expected: `[accept-admin] 通过`。列表无用户则先在 92 补一条线路用户，不改文案。

- [ ] **Step 5: 验收本地用户**

Run:

```
bin\accept.bat --path /yianlian/local/user --click 查看权限 --expect-text 线路权限 --title 本地用户线路权限 --module M2 --round 0
```

Expected: `[accept-admin] 通过`。

- [ ] **Step 6: 失败则回修**

钉钉发报告（有 token 才发）→ `_systematic-debugging` → 再 deploy + accept，最多 2 轮。

- [ ] **Step 7: 填门禁表**

`docs/work/质量门禁记录.md` 增加日期行：gate-admin / deploy-92 / accept-admin / 轮次 / 结果。模块清单 M1–M3 改为已验收。未跑完不得写「已完成」。

---

## Spec 覆盖核对

| Spec 节 | 任务 |
|---------|------|
| 3.1 线路用户弹窗 / 文案 | Task 5 |
| 3.2 本地用户 / 未同步空列表 | Task 2 Step 4、Task 6 |
| 3.3 名称解析 | Task 2 Step 3 |
| 4 两条 API、非 Inner、无 spaKey | Task 3 |
| 5 前端按钮与权限 | Task 5、6 |
| 6 菜单 F | Task 4、8 Step 3 |
| 7 accept 路径与点击 | Task 7、8 |
| 8 安全 | Global + Task 3 |
| 9 非目标 | 计划未包含导出/反查/写授权 |
