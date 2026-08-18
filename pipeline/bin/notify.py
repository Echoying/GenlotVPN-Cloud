#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""钉钉通知入口。无凭证则 skip、退出 0。

  python pipeline/bin/notify.py markdown "标题" "正文"
  python pipeline/bin/notify.py markdown "标题" --text-file pipeline/work/<当次>/验收报告/验收报告-YYYY-MM-DD.md
  python pipeline/bin/notify.py text "正文"
  python pipeline/bin/notify.py markdown --title 标题 --text 正文
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

from paths import repo_root

BIN = Path(__file__).resolve().parent
REPO = repo_root()
sys.path.insert(0, str(BIN))
from local_env import load as load_local_env  # noqa: E402

load_local_env()


def find_send() -> Path:
    candidates = (
        REPO / ".agents" / "skills" / "_dingtalk-webhook__skills__dingtalk-webhook" / "scripts" / "dingtalk-send.py",
        REPO / ".skillshare" / "skills" / "_dingtalk-webhook__skills__dingtalk-webhook" / "scripts" / "dingtalk-send.py",
        REPO / ".kiro" / "skills" / "_dingtalk-webhook__skills__dingtalk-webhook" / "scripts" / "dingtalk-send.py",
        REPO.parent / "echoying-skills" / "skills" / "dingtalk-webhook" / "scripts" / "dingtalk-send.py",
    )
    for path in candidates:
        if path.is_file():
            return path
    raise SystemExit("[notify] 缺少 dingtalk-send.py，请先 skillshare sync")


def build_argv(raw: list) -> list:
    if not raw:
        raise SystemExit(
            "[notify] 用法: python pipeline/bin/notify.py markdown \"标题\" \"正文\"\n"
            "         python pipeline/bin/notify.py text \"正文\""
        )
    send = [sys.executable, "-u", str(find_send())]
    cmd = raw[0].lower()
    if cmd == "markdown":
        if len(raw) >= 2 and not raw[1].startswith("-"):
            title = raw[1]
            rest = raw[2:]
            if rest[:1] == ["--text-file"] and len(rest) >= 2:
                return send + ["markdown", "--title", title, "--text-file", rest[1]]
            if rest:
                return send + ["markdown", "--title", title, "--text", rest[0]]
            return send + ["markdown", "--title", title, "--text", ""]
        return send + raw
    if cmd == "text":
        if len(raw) >= 2 and not raw[1].startswith("-"):
            return send + ["text", "--text", raw[1]]
        return send + raw
    return send + raw


def main() -> int:
    token = os.environ.get("DINGTALK_ACCESS_TOKEN", "").strip()
    webhook = os.environ.get("DINGTALK_WEBHOOK", "").strip()
    if not token and not webhook:
        print("[notify] 无钉钉凭证，跳过", flush=True)
        return 0
    return subprocess.call(build_argv(sys.argv[1:]))


if __name__ == "__main__":
    raise SystemExit(main())
