#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""构建门禁（跨平台）。默认 Java + 前端 + 客户端打包。客户端仍不自动验收。

  python pipeline/bin/gate.py server --run 2026-08-15-用户线路权限查询
  python pipeline/bin/gate.py              全量（建议加 --run）
  python pipeline/bin/gate.py java|ui|client
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path

from paths import repo_root
from run_log import append_step, pop_run_arg

REPO = repo_root()
CLIENT_BIN = REPO / "bin"

try:
    sys.stdout.reconfigure(line_buffering=True)
    sys.stderr.reconfigure(line_buffering=True)
except Exception:
    pass


def which(name: str):
    found = shutil.which(name)
    if found:
        return found
    if os.name == "nt":
        return shutil.which(name + ".cmd") or shutil.which(name + ".bat")
    return None


def run(cmd, cwd=None) -> int:
    print("[gate]", " ".join(cmd), flush=True)
    return subprocess.call(cmd, cwd=str(cwd or REPO))


def ensure_java8() -> None:
    home = os.environ.get("JAVA_HOME", "").strip()
    win_default = Path(r"C:\Program Files\Java\jdk-1.8")
    if not home and win_default.is_dir():
        os.environ["JAVA_HOME"] = str(win_default)
        os.environ["PATH"] = str(win_default / "bin") + os.pathsep + os.environ.get("PATH", "")
    java = which("java")
    if not java:
        raise SystemExit("[gate] 失败: 找不到 java")
    proc = subprocess.Popen([java, "-version"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out = (proc.communicate()[0] or b"").decode("utf-8", errors="ignore")
    if "1.8" not in out:
        raise SystemExit("[gate] 失败: java 不是 1.8，当前 JAVA_HOME={}".format(os.environ.get("JAVA_HOME", "")))


def build_java() -> None:
    print("[gate] 全量 Java（JDK8 + clean，含 92 全部模块 + vpn-auth）", flush=True)
    ensure_java8()
    mvn = which("mvn")
    if not mvn:
        raise SystemExit("[gate] 失败: 找不到 mvn")
    if run([mvn, "clean", "package", "-DskipTests"]) != 0:
        raise SystemExit("[gate] 失败: Maven 全量构建")


def build_ui() -> None:
    print("[gate] 构建 ruoyi-ui production", flush=True)
    ui = REPO / "ruoyi-ui"
    if not (ui / "node_modules").is_dir():
        raise SystemExit("[gate] 失败: 缺少 ruoyi-ui/node_modules，请先在 ruoyi-ui 执行 npm install")
    npm = which("npm")
    if not npm:
        raise SystemExit("[gate] 失败: 找不到 npm")
    if run([npm, "run", "build:prod"], cwd=ui) != 0:
        raise SystemExit("[gate] 失败: npm run build:prod")
    if not (ui / "dist" / "index.html").is_file():
        raise SystemExit("[gate] 失败: 未生成 ruoyi-ui/dist/index.html")


def build_client() -> None:
    print("[gate] 编译并打包桌面客户端（不自动验收）", flush=True)
    bat = CLIENT_BIN / "build-vpn-client.bat"
    if os.name == "nt" and bat.is_file():
        if run(["cmd", "/c", str(bat), "--package"]) != 0:
            raise SystemExit("[gate] 失败: 客户端构建/打包")
        return
    print("[gate] 非 Windows：跳过客户端打包（请在 Windows 上执行 python pipeline/bin/gate.py client）", flush=True)


def main() -> int:
    os.chdir(REPO)
    rest, run = pop_run_arg(sys.argv[1:])
    scope = (rest[0] if rest else "").strip().lower()
    if scope in ("-h", "--help"):
        print(__doc__.strip())
        return 0
    do_java = do_ui = do_client = True
    if scope == "ui":
        do_java = do_client = False
    elif scope == "java":
        do_ui = do_client = False
    elif scope == "client":
        do_java = do_ui = False
    elif scope == "server":
        do_client = False
    elif scope and scope not in ("", "all"):
        raise SystemExit("[gate] 用法: python pipeline/bin/gate.py [server|java|ui|client] [--run 当次目录]")

    try:
        if do_java:
            build_java()
        if do_ui:
            build_ui()
        if do_client:
            build_client()
    except SystemExit as exc:
        raw = exc.code
        code = 0 if raw is None else (raw if isinstance(raw, int) else 1)
        append_step(
            run,
            "gate",
            "通过" if code == 0 else "失败",
            detail="" if code == 0 else str(exc),
            scope=scope or "all",
        )
        raise
    append_step(run, "gate", "通过", scope=scope or "all")
    print("[gate] 通过", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
