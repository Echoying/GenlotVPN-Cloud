#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""验收入口。默认管理端 Playwright；client-ref 为桌面端 AI 基础走查（参考，≠ 通过）。"""
from __future__ import annotations

import sys
from pathlib import Path

from paths import work_bin

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(work_bin()))


def main() -> int:
    args = sys.argv[1:]
    if args and args[0] == "client-ref":
        sys.argv = [sys.argv[0]] + args[1:]
        from accept_client_ref import main as client_main

        return client_main()
    from accept_admin import main as admin_main

    return admin_main()


if __name__ == "__main__":
    raise SystemExit(main())
