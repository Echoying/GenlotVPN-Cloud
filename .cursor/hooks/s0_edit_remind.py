#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""改到客户端 transport / vpn-auth TCP/TLS 时提醒：不自动验收，走人勾 PHASE2。"""
from __future__ import annotations

import json
import re
import sys

S0_PATH_RE = re.compile(
    r"(?ix)"
    r"ruoyi-vpn-client[/\\]src[/\\]transport"
    r"|ruoyi-vpn-client[/\\].*\.proto$"
    r"|ruoyi-vpn-client[/\\].*(tls|ssl|certpin|pinner)"
    r"|ruoyi-vpn-auth[/\\].*[/\\]tcp[/\\]"
    r"|ruoyi-vpn-auth[/\\].*(tls|ssl|cert).*\.(java|yml|yaml)$"
    r"|ruoyi-vpn-auth[/\\].*\.proto$"
    r"|scripts[/\\]vpn-tls"
    r"|docker[/\\].*vpn[/\\]auth[/\\]certs"
)

REMINDER = (
    "【S0】本次改动涉及客户端传输层或 vpn-auth TCP/TLS。"
    "不得自动验收，不得用 Playwright/QTest 代替。"
    "请人勾 ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md，"
    "并在当次目录质量门禁.md 与 pipeline/work/_shared/质量门禁记录.md 留行。"
)


def extract_path(payload: dict) -> str:
    if payload.get("file_path"):
        return str(payload["file_path"])
    inp = payload.get("tool_input") or payload.get("input") or {}
    if isinstance(inp, dict):
        return str(inp.get("path") or inp.get("file_path") or inp.get("target_notebook") or "")
    return ""


def is_s0_path(path: str) -> bool:
    if not path:
        return False
    norm = path.replace("\\", "/")
    return bool(S0_PATH_RE.search(norm) or S0_PATH_RE.search(path))


def decide(payload: dict) -> dict:
    if not is_s0_path(extract_path(payload)):
        return {}
    return {"additional_context": REMINDER}


def _self_test() -> int:
    cases = [
        (r"D:\repo\ruoyi-vpn-client\src\transport\TcpClient.cpp", True),
        (r"D:\repo\ruoyi-vpn-auth\src\main\java\com\ruoyi\vpn\auth\tcp\VpnTcpServerRunner.java", True),
        (r"D:\repo\ruoyi-ui\src\views\login.vue", False),
        (r"D:\repo\ruoyi-vpn-auth\src\main\java\com\ruoyi\vpn\auth\controller\TokenController.java", False),
        (r"D:\repo\scripts\vpn-tls\README.md", True),
    ]
    failed = 0
    for path, expect in cases:
        got = bool(decide({"file_path": path}))
        if got != expect:
            failed += 1
            sys.stderr.write("FAIL {} expected={} got={}\n".format(path, expect, got))
    if failed:
        sys.stderr.write("self-test failed: {}\n".format(failed))
        return 1
    sys.stderr.write("self-test ok\n")
    return 0


def main() -> int:
    if len(sys.argv) > 1 and sys.argv[1] == "--self-test":
        return _self_test()
    raw = sys.stdin.read()
    if not raw.strip():
        sys.stdout.write("{}")
        return 0
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        sys.stdout.write("{}")
        return 0
    sys.stdout.write(json.dumps(decide(payload), ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
