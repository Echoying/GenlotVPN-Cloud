# Agent 工作约定（GenlotVPN-Cloud）

流水线已收拢到 [pipeline/](pipeline/README.md)。**完整约定以 [pipeline/AGENTS.md](pipeline/AGENTS.md) 为准。**

方法原则见 [SOP](pipeline/docs/开发+AI复杂项目六阶段工作法-SOP.md)，准入准出见 [工程实施手册](pipeline/docs/开发+AI复杂项目六阶段工作法-工程实施手册.md)，本仓库怎么跑见 [流水线实施说明](pipeline/docs/开发+AI流水线-仓库实施说明.md)、[Agent 执行指引](pipeline/六阶段流水线-Agent执行指引.md)。

通用怎么做用 [echoying-skills](http://10.13.0.176/huangzhiqing/echoying-skills)。**本项目差异**只写在 `pipeline/work/`。不新建 `phase-*` Skill。对话和注释用简体中文。

## 硬门槛（未满足不得往下）

1. **创造性工作先 `_brainstorming`**：拆模块、出方案、写 spec 与验收标准。
2. **spec 未经人批准，不得写业务代码、不得搭脚手架。**
3. 声称完成前必须按顺序跑：`python pipeline/bin/gate.py server --run <当次>` → `python pipeline/bin/deploy.py --run <当次>` → `python pipeline/bin/accept.py --run <当次>`（命令来自当前 spec 第 7 节）。`deploy.py vpn` 带上 93 vpn-auth。没带 `--run`、当次 `质量门禁.md` 无脚本记录，不得声称已门禁。
4. **客户端要编、要打包，但不自动验收**。人工勾 [PHASE2_CHECKLIST.md](ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md)。`accept.py client-ref` 只作 AI 基础走查参考，不得声称客户端已验收。
5. 页面验收失败：钉钉发缺陷 → `_systematic-debugging` 回修 → 再 deploy + accept，**最多 2 轮**。
6. 密码、钉钉 token、SSH 只走本机 `pipeline/deploy/envs.yaml` 或 `.deploy/envs.yaml` 或环境变量，不入库、不写进对话。

## 落盘

| 工件 | 路径 |
|------|------|
| 当次闭环 | `pipeline/work/YYYY-MM-DD-短标题/`（含模块清单 / spec / 计划 / 门禁 / 验收） |
| 跨功能 ADR | `pipeline/work/_shared/ADR-*.md` |
| 门禁总表 | `pipeline/work/_shared/质量门禁记录.md` |

未跑构建/验收，不得按 `_verification-before-completion` 声称完成。
