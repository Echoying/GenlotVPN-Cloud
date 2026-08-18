# 开发 + AI 流水线 — 仓库实施说明

| 项目 | 内容 |
|------|------|
| **文档编号** | SOP-AI-DEV-001-REPO |
| **版本** | V1.5 |
| **配套** | [主 SOP](./开发+AI复杂项目六阶段工作法-SOP.md)、[工程实施手册](./开发+AI复杂项目六阶段工作法-工程实施手册.md) |
| **作者** | 黄智清 |
| **日期** | 2026-08-16 |

本文件只回答：**六阶段在本仓库怎么通用地接到 skill 和脚本**。原则仍看 SOP，准入准出仍看工程手册。Agent 逐步怎么跑、流程图和第一条已执行实例见 [六阶段流水线-Agent执行指引.md](../六阶段流水线-Agent执行指引.md)。整夹迁到另一仓库见 [流水线搬迁到其他项目.md](./流水线搬迁到其他项目.md)。

这条流水线是 **GenlotVPN-Cloud 项目通用** 的：任何新功能都走同一套。不按功能新建 skill，也不再做第二条项目流水线。

---

## 1. 分层（不要每个项目建一个 skill 仓）

| 放哪 | 放什么 | 不放什么 |
|------|--------|----------|
| [echoying-skills](http://10.13.0.176/huangzhiqing/echoying-skills) | 各项目都能用的怎么做：brainstorming、写 plan、完成前必须验证、系统化排错、钉钉怎么发 | 本项目的 92/93 地址、菜单、账号、验证码、选择器 |
| **本仓库** | 本项目怎么接：全部在 `pipeline/`（见 [README](../README.md)） | 不要把六阶段再抄一份进 skill 仓 |

已迁入自己仓的闭环 skill：`_brainstorming`、`_writing-plans`、`_verification-before-completion`、`_systematic-debugging`、`_dingtalk-webhook`。不新建 7 个 `phase-*` Skill。

---

## 2. 本项目通用闭环（每条功能）

```
需求 → brainstorming 拆模块 + spec（验收路径/文案写进 spec）
         ↓ 人批准一次（HARD-GATE）
      按参照实现（熟悉域阶段四 / 陌生域阶段三）
         ↓
      gate → deploy → accept（参数来自当前 spec）
         ↓ 失败
      钉钉缺陷 → systematic-debugging → 再部署验收（≤2 轮）
         ↓ 通过
      钉钉发验收标准 + 报告
```

| 阶段 | 本仓库动作 | 人 |
|------|------------|----|
| 一 拆解 | `_brainstorming` 建 `pipeline/work/YYYY-MM-DD-短标题/`，在当次目录写模块清单 | 认边界 |
| 二 共创 | 当次目录写 spec；跨功能选型写 `pipeline/work/_shared/ADR-*.md` | **批准才写码** |
| 三 / 四 | 陌生域最小 Demo；熟悉域对照已有模块 | 抽查 |
| 五 门禁 | `python pipeline/bin/gate.py --run <当次>` → `python pipeline/bin/deploy.py --run <当次>` → `python pipeline/bin/accept.py --run <当次>` | 生产验证码保持开；S0 人验 |
| 六 迭代 | `python pipeline/bin/notify.py` + `_systematic-debugging` | 超 2 轮介入 |

回修不要丢回 brainstorming（需求理解错了才回阶段二）。

本机命令（密码只走环境变量；入口全部是 Python，不要再走 `*.bat`）：

```bash
python -u pipeline/bin/gate.py server --run <YYYY-MM-DD-短标题>
python -u pipeline/bin/gate.py client --run <YYYY-MM-DD-短标题>
python -u pipeline/bin/deploy.py --run <YYYY-MM-DD-短标题>
python -u pipeline/bin/deploy.py vpn --run <YYYY-MM-DD-短标题>
python -u pipeline/bin/accept.py --run <YYYY-MM-DD-短标题> --path <spec里的路由> --expect-text <spec里的文案> --module <编号> --round 0
python -u pipeline/bin/notify.py markdown "验收失败" --text-file pipeline/work/<当次>/验收报告/验收报告-YYYY-MM-DD.md
```

默认 `gate` 会编 **Java 全量 + ruoyi-ui + 桌面客户端**。只改管理端用 `python pipeline/bin/gate.py server`。`deploy` 覆盖 92 全部模块；`vpn` 再带上 93 的 vpn-auth。91 中间件不随功能重部。客户端产物要编出来，验收仍走人勾 PHASE2。非 Windows 上 `gate` 会跳过客户端打包并说明，不得假装编过。

`bin/run-*.bat`、`build-vpn-client.bat` 仍是本机 Windows 辅助（MSVC / windeployqt），不是流水线入口。客户端跨平台构建另议。

依赖：`pip install -r pipeline/bin/requirements-admin-e2e.txt`，再 `playwright install chromium`。无钉钉 token 时 notify 会 skip，退出码 0。

**客户端：** 人勾 [PHASE2_CHECKLIST.md](../../ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md)。传输层 S0，见 `pipeline/work/_shared/ADR-001`、`ADR-002`。

---

## 3. 第一条怎么跑（示例，不是流水线本身）

流水线不绑定某一页。下面只是用来把闭环跑通的第一条：

> 用 brainstorming，按六阶段流水线做：查看用户线路权限

- M1 汇总查询 API（复用 `IVpnLineAuthService` / `Yal*Auth`）
- M2 `ruoyi-ui/src/views/vpn` 查询页
- M3 菜单与权限标识
- 不含改易安联写授权；不含客户端自动验收

之后每条新功能：同样触发 brainstorming → 人批 spec → `gate` / `deploy` / `accept` 换 spec 里的路径和文案。

评委可核验：主 SOP + 工程手册 + **本说明** + 已批 spec + `pipeline/work/` + 钉钉截图 + PHASE2。不要讲成「无人审就能交整仓」。

---

## 4. 申报评分怎么对应

| 维度（参考权重） | 本仓库对应什么 | 不要讲成 |
|------------------|----------------|----------|
| 业务价值 | 陌生 Qt 客户端打磨到能用且安全；管理端能力可复用这套闭环 | 「AI 写了前后端」 |
| 效率 | 人只批一次 spec；本项目构建/部署/页面验收由 Agent 按脚本跑 | 无人审就合入 |
| 应用深度 | HARD-GATE、S0/S1/S2、回修 ≤2、skill 与项目分层 | 每个项目一个 skill 仓；每条功能一条流水线 |
| 知识沉淀 | SOP、手册、本说明、ADR、`pipeline/work/`、PHASE2 | 只交聊天截图 |
| 合规 | 脱敏、凭证走环境变量、生产验证码保持开、S0 人审 | 上传密码/密钥 |

一票否决相关：AI 结果无人审、向 AI 上传敏感数据。本仓库用「未批 spec 不写码」和脱敏 Rule 卡住。

---

## 5. 本机环境

- SSH 到 `10.27.0.92`（`vpn` 时还有 `10.27.0.93`），可重建对应容器
- 本机凭证：复制 `pipeline/deploy/envs.yaml.example` 为 `pipeline/deploy/envs.yaml`（或仍用 `.deploy/envs.yaml`）后填写。`deploy` / `accept` / `notify` 会自动读取。`envs.yaml` 已 gitignore，不要提交。
- 也可继续用系统环境变量（已存在的变量优先，文件不覆盖）。没有 yaml 时回退仓库根目录 `.env`。
- JDK8、`ruoyi-ui` 依赖已能编过
- 92 / Nacos `ruoyi-gateway-dev.yml` 已关管理端登录验证码（`security.captcha.enabled: false`，并放行 `/code`）。生产 profile 不要照抄。不关桌面/选线验证码。片段见 `sql/update/gateway_captcha_e2e_dev.yml`。

不上云端 Automation、不接 Jenkins、不加客户端 QTest。

菜单 SQL 打到 91（不要写密码）：

```bash
python -u pipeline/work/bin/apply_sql_91.py sql/update/20260815_line_auth_query_menu.sql
```

## 7. 本轮踩坑（必须按此写脚本）

详见 [ADR-003](../work/_shared/ADR-003-流水线踩坑.md)、[ADR-004](../work/_shared/ADR-004-流水线目录.md)。摘要：

- 流水线入口只用 Python（`pipeline/bin/gate.py` 等），不要再加 Windows bat 入口
- Java 镜像不要 `VOLUME /home/ruoyi`；compose 绑定 `.../jar/*.jar`
- deploy 上传 jar 后必须校验容器内 MD5，并等日志出现 `Started`
- accept 只点可见文案；有 `--click` 时断言弹窗标题，不要用页面任意「线路权限」
- 同日验收报告追加，不覆盖
- `dingtalk.access_token` 只填 token 段，不要整段 webhook URL

## 6. Hook（本仓库）

项目级 `.cursor/hooks.json`，改完后如未生效可重开 Cursor。

| 事件 | 脚本 | 行为 |
|------|------|------|
| `beforeSubmitPrompt` | `pipeline/cursor/hooks/before_submit_prompt.py` | 对话或附件像密码/密钥/`envs.yaml` 则拦截，提示改成 `***` |
| `afterFileEdit` / `postToolUse` | `pipeline/cursor/hooks/s0_edit_remind.py` | 改到客户端 `transport` 或 `vpn-auth` TCP/TLS 时提醒：不自动验收，人勾 PHASE2 |

本机自检：`python pipeline/cursor/hooks/before_submit_prompt.py --self-test` 与 `python pipeline/cursor/hooks/s0_edit_remind.py --self-test`。
