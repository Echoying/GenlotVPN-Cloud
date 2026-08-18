#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""部署入口。转发 deploy_admin_92.py。

  python pipeline/bin/deploy.py --run 2026-08-15-用户线路权限查询
  python pipeline/bin/deploy.py ui --run 2026-08-15-用户线路权限查询
  python pipeline/bin/deploy.py vpn --run 2026-08-15-用户线路权限查询
"""
from __future__ import annotations

import sys
from pathlib import Path

from paths import work_bin
from run_log import append_step, pop_run_arg

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(work_bin()))


def _code(exc_or_val) -> int:
    if isinstance(exc_or_val, SystemExit):
        raw = exc_or_val.code
        if raw is None:
            return 0
        return raw if isinstance(raw, int) else 1
    if exc_or_val is None:
        return 0
    return int(exc_or_val)


def main() -> int:
    args, run = pop_run_arg(sys.argv[1:])
    is_help = any(a in ("-h", "--help") for a in args)
    mapped = list(args)
    scope = "92"
    if args and args[0].lower() in ("ui", "vpn", "all"):
        scope = args[0].lower()
        mapped = ["--" + args[0].lower()] + args[1:]
    elif args and args[0].lower() == "java":
        mapped = args[1:]
    sys.argv = [sys.argv[0]] + mapped
    from deploy_admin_92 import main as deploy_main

    try:
        code = _code(deploy_main())
    except SystemExit as exc:
        code = _code(exc)
        if not is_help:
            append_step(
                run,
                "deploy",
                "通过" if code == 0 else "失败",
                detail="" if code == 0 else str(exc),
                scope=scope,
            )
        raise
    if not is_help:
        append_step(run, "deploy", "通过" if code == 0 else "失败", scope=scope)
    return code


if __name__ == "__main__":
    raise SystemExit(main())
