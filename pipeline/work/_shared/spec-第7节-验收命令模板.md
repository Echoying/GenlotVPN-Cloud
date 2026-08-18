# spec 第 7 节必须是可复制命令

阶段二写 spec 时，验收标准不能只写「打开某某页」。必须给出 Agent 能直接粘贴的命令。`--run` 与当次目录名一致。

建议标题：`## 7. 验收标准（命令可直接复制）`

只改管理端：

```bash
python -u pipeline/bin/gate.py server --run YYYY-MM-DD-短标题
python -u pipeline/bin/deploy.py --run YYYY-MM-DD-短标题
```

有菜单 SQL 时再加：

```bash
python -u pipeline/work/bin/apply_sql_91.py sql/update/xxx.sql
```

每个页面场景一条 accept（路径、文案、点击来自上文行为节）：

```bash
python -u pipeline/bin/accept.py --run YYYY-MM-DD-短标题 \
  --path /yianlian/xxx --click 查看权限 \
  --expect-text 线路权限 --title 场景名 --module M2 --round 0
```

不得用 `/index` 冒烟代替本功能验收。客户端不编则不要跑默认全量 `gate.py`。

改了客户端时，在管理端 accept 之后加 **AI 基础走查**（参考，≠ PHASE2 通过）。怎么认验证码、怎么点登录、踩坑见 [client-ref-走查流程.md](client-ref-走查流程.md)。桌面验证码保持开；脚本自动截验证码，**执行走查的 Agent 自己认图、自己 `--step submit --code` 回填**，不等人填验证码。门禁只能写 **参考完成 / 参考失败**。

```bash
python -u pipeline/bin/accept.py client-ref --run YYYY-MM-DD-短标题 --scene <off|admin-set|on> --step prepare
python -u pipeline/bin/accept.py client-ref --run YYYY-MM-DD-短标题 --scene <off|admin-set|on> --step submit --code <算式结果>
```

场景含义与本条差异写在当次 spec 第 7 节。首条范例：[2026-08-16-最低客户端版本拦截/spec.md](../2026-08-16-最低客户端版本拦截/spec.md) 第 7.3 节。

范例（仅管理端）见 [2026-08-15-用户线路权限查询/spec.md](../2026-08-15-用户线路权限查询/spec.md) 第 7 节。
