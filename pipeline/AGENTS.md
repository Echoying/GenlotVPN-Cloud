# Agent 工作约定（GenlotVPN-Cloud）

本文件约束 Cursor Agent。六阶段流水线是**当前项目通用**的：本仓库里任何新功能、改模块，都走同一套，不按单条需求另做一条流水线。

方法原则见 [SOP](docs/开发+AI复杂项目六阶段工作法-SOP.md)，准入准出见 [工程实施手册](docs/开发+AI复杂项目六阶段工作法-工程实施手册.md)，本仓库怎么跑见 [流水线实施说明](docs/开发+AI流水线-仓库实施说明.md)、[Agent 执行指引](六阶段流水线-Agent执行指引.md) 与 [README.md](README.md)。

通用怎么做用 [echoying-skills](http://10.13.0.176/huangzhiqing/echoying-skills)。**本项目差异**（92/93 地址、菜单、账号、Playwright 选择器、编哪些包）只写在 `pipeline/work/`。不新建 `phase-*` Skill。对话和注释用简体中文。

## 硬门槛（未满足不得往下）

1. **创造性工作先 `_brainstorming`**：拆模块、出方案、写 spec 与验收标准。
2. **spec 未经人批准，不得写业务代码、不得搭脚手架。**
3. 声称完成前必须按顺序跑：`python pipeline/bin/gate.py server --run <当次>` → `python pipeline/bin/deploy.py --run <当次>` → `python pipeline/bin/accept.py --run <当次>`（参数来自**当前这条** spec 第 7 节可复制命令）。默认 `gate` 含 Java + 前端 + **客户端编译打包**；`deploy.py vpn` 带上 93 vpn-auth。未跑过、或跑了但没带 `--run` 因而没写入当次 `质量门禁.md`，不得声称已门禁。
4. **客户端要编、要打包，但不自动验收**，不写 QTest / 五步法 E2E。人工勾 [PHASE2_CHECKLIST.md](../ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md)。可另跑 `accept.py client-ref` 作 AI 基础走查（截图进当次报告，门禁只记「参考完成/失败」），**不得**据此声称客户端已验收。
5. 页面验收失败：钉钉发缺陷 → `_systematic-debugging` 回修 → 再 deploy + accept，**最多 2 轮**。超轮次停下来等人。需求理解错了才回阶段二；不要把实现缺陷丢回 brainstorming。
6. 密码、钉钉 token、SSH 只走本机 `pipeline/deploy/envs.yaml` 或 `.deploy/envs.yaml` 或环境变量，不入库、不写进对话。复制 `pipeline/deploy/envs.yaml.example` 后填写。项目 Hook：提交前提示脱敏；改传输层提醒走人验 PHASE2。

## 六阶段 × 本项目（每条功能都走）

| 阶段 | AI | 人 |
|------|----|----|
| 一 拆解 | `_brainstorming` 建 `pipeline/work/YYYY-MM-DD-短标题/`，在当次目录写 `模块拆分清单.md` | 认边界 |
| 二 共创 | 方案 + spec + 验收标准（页面路径/文案写进 spec） | **批准一次（HARD-GATE）** |
| 三 / 四 | 陌生域最小 Demo；熟悉域按参照实现 | 抽查 |
| 五 门禁 | `gate` → `deploy` → `accept`（按 spec） | 生产验证码保持开；S0 人验 |
| 六 迭代 | 钉钉 + `_systematic-debugging` ≤2 轮 | 超轮次介入 |

管理端验证码：只允许关 **92** Nacos `ruoyi-gateway-dev.yml` 的 `security.captcha.enabled`，不删代码，生产保持开。不关桌面/选线验证码。

## 安全分级（人定级，AI 不得自行降级）

| 级 | 范围 | 验收 |
|----|------|------|
| S0 | `ruoyi-vpn-client` 传输层、`ruoyi-vpn-auth` TCP/TLS、证书/密钥 | 人主导；不自动验收 |
| S1 | 多线路同步、审计、钉钉验证 | AI 可写，人审逻辑 |
| S2 | 管理端列表/查询/标准 CRUD | 构建 + 页面验收 |

## 落盘（聊天记录不算沉淀）

| 工件 | 路径 |
|------|------|
| 当次闭环 | `pipeline/work/YYYY-MM-DD-短标题/`（模块清单 / spec / 计划 / 门禁 / 验收） |
| 跨功能 ADR | `pipeline/work/_shared/ADR-*.md` |
| 门禁总表 | `pipeline/work/_shared/质量门禁记录.md` |

未跑构建/验收，不得按 `_verification-before-completion` 声称完成。
