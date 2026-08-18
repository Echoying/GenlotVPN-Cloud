# pipeline/work — 本项目业务落盘

一条功能（一次六阶段闭环）一个目录，不要把多次的 spec / 计划 / 截图堆在同一层。

```
pipeline/work/
  bin/                         本项目适配器（92/93/91），不属于某一次
  _shared/                     跨功能：ADR、门禁总表
  YYYY-MM-DD-短标题/           当次流水线
    模块拆分清单.md
    spec.md
    plan.md
    质量门禁.md
    验收报告/
```

目录名：`YYYY-MM-DD-短标题`，与人批 spec 同一天、同一标题。新功能先建这个目录再写 spec。

`gate` / `deploy` / `accept` 都应带当次目录（命令以当次 spec 第 7 节为准）：

```bash
python -u pipeline/bin/gate.py server --run 2026-08-15-用户线路权限查询
python -u pipeline/bin/deploy.py --run 2026-08-15-用户线路权限查询
python -u pipeline/bin/accept.py --run 2026-08-15-用户线路权限查询 --path <路由> --expect-text <文案> --module M2 --round 0
```

移植时：`bin/` 和当次目录都换成新项目，不要把本夹当模板交出去。
