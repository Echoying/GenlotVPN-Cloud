# SPEC：查看用户线路权限

| 项 | 内容 |
|----|------|
| 日期 | 2026-08-15 |
| 状态 | 设计已批；实现计划见 [plan-2026-08-15-用户线路权限查询.md](./plan-2026-08-15-用户线路权限查询.md) |
| 安全级 | S2 |
| 模块 | M1 / M2 / M3 |
| 熟悉度 | 熟悉（对照现有 `IVpnLineAuthService`、`views/vpn/user`） |

人批准本文件后才写业务代码。实现后按本文件的路径/文案跑 `gate` → `deploy` → `accept`。

---

## 1. 背景与目标

运维要查：**某个用户在各条线路上有哪些应用组 / 应用，授权从哪来（部门 / 角色 / 用户）**。

现有能力不够：

- `GET /vpn/user/authorized-lines/{userId}` 仅给客户端线路列表（含 `spaKey`），且 `InnerAuth`。
- 线路用户页「授权」是**写入**当前线路的用户授权，不是只读汇总。
- 本地用户的 `getAuthorizedLines` 只反映「已同步到哪些线路」，不展开应用与来源。

本功能只读、只管理端。

---

## 2. 范围

**做：**

- 线路用户（`vpn_user`）与本地用户（`vpn_local_user`）都能查。
- 在现有两个列表操作里加「查看权限」，弹窗只读展示。
- 新建两条管理端只读 API，复用部门 / 角色 / 用户授权并集，并标来源。
- 按钮权限（菜单类型 `F`），不新建目录页。

**不做：**

- 不改 `authorized-lines`，不改客户端，不改选线 / TLS。
- 不写易安联、不改 `Yal*Auth` 保存逻辑。
- 不在弹窗里编辑授权。
- 不返回 `spaKey`、密码、`encryptedPwd`。

---

## 3. 行为

### 3.1 线路用户

入口：VPN 管理 → 线路用户管理。操作列「更多」增加「查看权限」（与「授权」并列，权限不同）。

弹窗标题：`线路权限 - {userName}`。

内容：按线路分组。每条线路下列出应用组、应用、来源。同一应用因多条授权命中时，来源合并显示（如 `部门, 角色`），不重复铺行。

无任何授权：弹窗内文案 **「暂无线路权限」**。用户不存在：接口失败，前端提示错误，不弹空成功窗。

### 3.2 本地用户

入口：VPN 管理 → 本地用户管理。操作列增加「查看权限」。

解析：先查该本地用户已同步出的线路用户（按 `localUserId` + 线路），再对每个线路用户做与 3.1 相同的汇总，按线路展示。

未同步到任何线路：弹窗标题仍为 `线路权限 - {userName}`，正文 **「暂无线路权限」**。

### 3.3 名称解析

`Yal*Auth` 的 `appGroupIds` / `appIds` 按本地 `vpn_service_group.groupName`、`vpn_service.appName` 解析；解析不到则显示原始 ID。线路名用 `line_app.appName`。

来源文案固定：`部门`、`角色`、`用户`。

---

## 4. 接口

均走网关、需登录，**不是** `InnerAuth`。

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/vpn/user/{userId}/line-auths` | `yianlian:user:queryAuth` |
| GET | `/vpn/local/user/{localUserId}/line-auths` | `vpn:localUser:queryAuth` |

成功：`AjaxResult`，`data` 为：

```json
{
  "userId": 1,
  "userName": "zhangsan",
  "userType": "line",
  "lines": [
    {
      "lineId": "app-id",
      "lineName": "线路甲",
      "items": [
        {
          "appGroupId": "1",
          "appGroupName": "办公",
          "appId": "2",
          "appName": "OA",
          "sources": ["部门", "用户"]
        }
      ]
    }
  ]
}
```

本地用户：`userType` 为 `local`，`userId` 为 `localUserId`。

用户不存在：业务失败（非 200 空成功）。`lines` 为空表示无权限。禁止出现 `spaKey`。

实现落在 `IVpnLineAuthService` 新方法（或同包只读查询服务），内部仍读 `YalDeptAuth` / `YalRoleAuth` / `YalUserAuth`。不要改 `toAuthorizedLineVos`。

---

## 5. 前端

- `ruoyi-ui/src/views/vpn/user/index.vue`：更多菜单加「查看权限」，`v-hasPermi="['yianlian:user:queryAuth']"`。
- `ruoyi-ui/src/views/vpn/local/user/index.vue`：操作列加「查看权限」，`v-hasPermi="['vpn:localUser:queryAuth']"`。
- 弹窗只读表列：应用组、应用、来源；按线路分组标题。
- API 封装放 `ruoyi-ui/src/api/vpn/`，对照现有 user / localUser。
- 文案必须与第 3、7 节一致，供 Playwright 断言。

---

## 6. 菜单（M3）

不新建 `C` 菜单。`sql/update/` 增加可重入脚本，插入两条 `F`：

| 父菜单 | 名称 | perms |
|--------|------|-------|
| 1065 线路用户管理 | 查看线路权限 | `yianlian:user:queryAuth` |
| 1072 本地用户管理 | 查看线路权限 | `vpn:localUser:queryAuth` |

挂到 admin 角色（`role_id=1`）。92 部署后执行该 SQL（或等价插入），否则按钮不可见、验收失败。

---

## 7. 验收标准（accept 参数来自这里）

页面路由（父菜单 `yianlian`）：

| 场景 | `--path` | 操作 | `--expect-text` |
|------|----------|------|-----------------|
| 线路用户 | `/yianlian/vpn/user` | 打开「更多」点「查看权限」（任一行） | `线路权限` |
| 本地用户 | `/yianlian/local/user` | 点「查看权限」（任一行） | `线路权限` |

不得用 `/index` 冒烟代替本功能验收。前置：92 两条列表各至少有一条用户，admin 已具备第 6 节按钮权限。无数据时本条验收失败，补种子用户，不改断言文案。客户端不用为这条重编。

可直接复制：

```bash
python -u pipeline/bin/gate.py server --run 2026-08-15-用户线路权限查询
python -u pipeline/bin/deploy.py --run 2026-08-15-用户线路权限查询
python -u pipeline/work/bin/apply_sql_91.py sql/update/20260815_line_auth_query_menu.sql
python -u pipeline/bin/accept.py --run 2026-08-15-用户线路权限查询 \
  --path /yianlian/vpn/user --click 更多 --click 查看权限 \
  --expect-text 线路权限 --title 线路用户线路权限 --module M2 --round 0
python -u pipeline/bin/accept.py --run 2026-08-15-用户线路权限查询 \
  --path /yianlian/local/user --click 查看权限 \
  --expect-text 线路权限 --title 本地用户线路权限 --module M2 --round 0
```

---

## 8. 安全与合规

- S2：标准只读查询。不得把本接口做成 Inner、不得带密钥。
- 对话 / 报告 / 截图脱敏。
- 不关桌面 / 选线验证码。

---

## 9. 非目标（防止膨胀）

导出 Excel、按线路反查用户、改授权、对接易安联写回、本地用户未同步时的「预测权限」。
