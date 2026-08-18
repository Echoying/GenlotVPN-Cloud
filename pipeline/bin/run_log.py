#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 gate / deploy / accept 结果追加到当次目录 质量门禁.md。无 --run 则跳过、不失败。"""
from __future__ import annotations

from datetime import datetime
from typing import List, Tuple

from paths import run_dir

MARKER = "## 脚本记录"
TABLE_HEAD = "| 时间 | 步骤 | 范围 | 结果 | 说明 |\n|------|------|------|------|------|\n"


def pop_run_arg(argv: List[str]) -> Tuple[List[str], str]:
    """从 argv 抽出 --run NAME 或 --run=NAME，其余原样返回。"""
    out: List[str] = []
    run = ""
    i = 0
    while i < len(argv):
        item = argv[i]
        if item == "--run" and i + 1 < len(argv):
            run = argv[i + 1].strip()
            i += 2
            continue
        if item.startswith("--run="):
            run = item.split("=", 1)[1].strip()
            i += 1
            continue
        out.append(item)
        i += 1
    return out, run


def append_step(run: str, step: str, result: str, detail: str = "", scope: str = "") -> None:
    if not (run or "").strip():
        print("[run-log] 未指定 --run，跳过写入质量门禁", flush=True)
        return
    try:
        folder = run_dir(run)
    except ValueError as exc:
        print("[run-log] {}".format(exc), flush=True)
        return
    if not folder.is_dir():
        print("[run-log] 当次目录不存在，跳过: {}".format(folder), flush=True)
        return
    path = folder / "质量门禁.md"
    safe = (detail or "").replace("|", "/").replace("\n", " ").strip()
    line = "| {} | {} | {} | {} | {} |\n".format(
        datetime.now().isoformat(timespec="seconds"),
        step,
        scope or "—",
        result,
        safe,
    )
    if path.is_file():
        text = path.read_text(encoding="utf-8")
        if MARKER not in text:
            text = text.rstrip() + "\n\n" + MARKER + "\n\n" + TABLE_HEAD
        path.write_text(text.rstrip() + "\n" + line, encoding="utf-8")
    else:
        path.write_text(
            "# 质量门禁 — {}\n\n".format(run) + MARKER + "\n\n" + TABLE_HEAD + line,
            encoding="utf-8",
        )
    print("[run-log] 已写入 {}".format(path), flush=True)
