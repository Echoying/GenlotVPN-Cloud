# pipeline — 六阶段流水线（本仓库一份）

本目录是流水线的**唯一维护点**。方法、脚本、Cursor 约定、本机凭证模板、本项目落盘都在这里。移植到新项目时拷这一夹，再按下面改业务层。

```
pipeline/
  README.md                 本说明
  六阶段流水线-Agent执行指引.md  Agent 逐步怎么跑（含流程图与第一条实例）
  AGENTS.md                 Agent 硬门槛（根目录 AGENTS.md 指向这里）
  bin/                      通用入口：gate / deploy / accept / notify / local_env
  docs/                     SOP、工程手册、仓库实施说明
  cursor/                   rules + hooks 源（根上 .cursor/ 给 Cursor 读）
  deploy/                   envs.yaml.example（真凭证不要入库）
  work/                     本项目业务层（移植时整夹丢掉或换成新项目）
    bin/                    92/93 部署、Playwright 验收、91 打 SQL
    当次目录（模块清单 / spec / 门禁 / 验收）+ _shared ADR
```

## 本仓库怎么跑

```bash
python -u pipeline/bin/gate.py server --run <YYYY-MM-DD-短标题>
python -u pipeline/bin/deploy.py --run <YYYY-MM-DD-短标题>
python -u pipeline/bin/accept.py --run <YYYY-MM-DD-短标题> --path <路由> --expect-text <文案> --module <编号> --round 0
python -u pipeline/bin/notify.py markdown "标题" --text-file pipeline/work/<当次>/验收报告/验收报告-YYYY-MM-DD.md
python -u pipeline/work/bin/apply_sql_91.py sql/update/xxx.sql
```

凭证：复制 `pipeline/deploy/envs.yaml.example` 为 `pipeline/deploy/envs.yaml` 或 `.deploy/envs.yaml` 后填写。两处都会读，已存在的环境变量不覆盖。

依赖：`pip install -r pipeline/bin/requirements-admin-e2e.txt`，再 `playwright install chromium`。

不进本目录：`.agents/skills`（走 echoying-skills）、`bin/run-*.bat` / `build-vpn-client.bat`（本机 Windows 辅助）。

## 拷到新项目

搬什么、不搬什么、目标仓怎么接、接上后跑哪些命令：见 [docs/流水线搬迁到其他项目.md](docs/流水线搬迁到其他项目.md)。

摘要：拷 `pipeline/` 骨架（bin / docs / cursor / envs.example）；**不要**拷 `work/` 里的当次目录和 92/93 脚本。根上放薄 `AGENTS.md`，Rule/Hook 从 `pipeline/cursor/` 同步到 `.cursor/`。改 `gate.py` 的构建命令，在 `work/bin` 重写部署/验收适配器。凭证只走本机 yaml。
