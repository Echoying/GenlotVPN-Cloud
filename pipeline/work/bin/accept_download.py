#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""下载站页面验收（Playwright）。免登录，打开 93 静态页、按参数断言、写报告。

不打开 92、不读管理端账号。自签证书忽略校验。
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
import time
from datetime import date, datetime
from pathlib import Path

try:
    sys.stdout.reconfigure(line_buffering=True)
    sys.stderr.reconfigure(line_buffering=True)
except Exception:
    pass

_PIPE_BIN = Path(__file__).resolve().parents[2] / "bin"
sys.path.insert(0, str(_PIPE_BIN))
from paths import repo_root, run_dir  # noqa: E402
from local_env import load as load_local_env  # noqa: E402
from run_log import append_step  # noqa: E402

REPO = repo_root()
load_local_env()


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="下载站 Playwright 验收（免登录）")
    p.add_argument("--base-url", default=env("DOWNLOAD_E2E_BASE_URL", "https://10.27.0.93"))
    p.add_argument("--path", default="/genlotvpn/download/")
    p.add_argument("--expect-text", required=True, help="页面应出现的文案")
    p.add_argument("--title", default="下载站页面验收")
    p.add_argument("--module", default="", help="模块编号，如 M1")
    p.add_argument("--round", type=int, default=int(env("DOWNLOAD_E2E_ROUND") or "0"), help="回修轮次 0-2")
    p.add_argument("--headed", action="store_true", help="有头模式，便于人工看")
    p.add_argument("--click", action="append", default=[], help="打开目标页后依次点击的可见文案，可重复")
    p.add_argument(
        "--check-files",
        action="store_true",
        help="核对当前页「下载」链接 HEAD 为 200 且有体积（不全量拉取安装包）",
    )
    p.add_argument(
        "--run",
        default=env("DOWNLOAD_E2E_RUN") or env("ADMIN_E2E_RUN"),
        help="pipeline/work 下的当次目录名，如 2026-08-19-客户端下载页",
    )
    return p.parse_args()


def click_visible_text(page, label: str, timeout: int = 15000) -> None:
    """只点当前可见节点。顶栏 中 / EN / Windows 可能有多处同文案。"""
    deadline = time.time() + timeout / 1000.0
    last_err = None
    while time.time() < deadline:
        loc = page.get_by_text(label, exact=True)
        n = loc.count()
        for i in range(n):
            item = loc.nth(i)
            try:
                if item.is_visible():
                    item.click(timeout=3000)
                    return
            except Exception as exc:
                last_err = exc
        page.wait_for_timeout(250)
    raise last_err or RuntimeError("未找到可见文案: {}".format(label))


def check_download_files(page, context, base: str, checks: list) -> None:
    """当前平台两张卡的下载链接必须能 HEAD 到实体文件。"""
    page.wait_for_selector("a.btn[href*='/genlotvpn/download/files/']", timeout=15000)
    links = page.locator("a.btn[href*='/genlotvpn/download/files/']")
    n = links.count()
    if n < 2:
        raise AssertionError("可见下载链接不足 2 条，实际 {}".format(n))
    for i in range(n):
        href = links.nth(i).get_attribute("href") or ""
        url = href if href.startswith("http") else base.rstrip("/") + href
        resp = context.request.head(url, timeout=30000)
        name = href.rsplit("/", 1)[-1]
        clen = int(resp.headers.get("content-length") or "0")
        if resp.status != 200:
            raise AssertionError("下载 {} HTTP {}".format(name, resp.status))
        if clen < 1024:
            raise AssertionError("下载 {} 体积过小: {} 字节".format(name, clen))
        checks.append(("下载 {} HTTP 200 {} 字节".format(name, clen), "通过"))


def write_report(path: Path, rows: list[str]) -> None:
    """同日多次 accept 追加，不覆盖已有人工整理内容。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    block = "\n".join(rows).rstrip() + "\n"
    if path.is_file():
        old = path.read_text(encoding="utf-8")
        path.write_text(old.rstrip() + "\n\n---\n\n" + block, encoding="utf-8")
    else:
        path.write_text(block, encoding="utf-8")
    print(f"[accept-download] 报告: {path}", flush=True)


def _dingtalk_script() -> Path:
    candidates = (
        REPO / ".agents" / "skills" / "_dingtalk-webhook__skills__dingtalk-webhook" / "scripts" / "dingtalk-send.py",
        REPO / ".skillshare" / "skills" / "_dingtalk-webhook__skills__dingtalk-webhook" / "scripts" / "dingtalk-send.py",
        REPO / ".kiro" / "skills" / "_dingtalk-webhook__skills__dingtalk-webhook" / "scripts" / "dingtalk-send.py",
    )
    for path in candidates:
        if path.is_file():
            return path
    return candidates[0]


def notify_dingtalk(title: str, text: str) -> None:
    token = env("DINGTALK_ACCESS_TOKEN")
    webhook = env("DINGTALK_WEBHOOK")
    if not token and not webhook:
        print("[accept-download] 无钉钉凭证，跳过通知", flush=True)
        return
    script = _dingtalk_script()
    if not script.is_file():
        print("[accept-download] 无 dingtalk-send.py，跳过通知", flush=True)
        return
    result = subprocess.run(
        [sys.executable, str(script), "markdown", "--title", title, "--text", text],
        check=False,
    )
    print("[accept-download] 钉钉退出码 {}".format(result.returncode), flush=True)


def main() -> int:
    args = parse_args()
    if not args.run:
        raise SystemExit(
            "缺少 --run（或 DOWNLOAD_E2E_RUN）。当次目录例如 --run 2026-08-19-客户端下载页"
        )
    try:
        current_run = run_dir(args.run)
    except ValueError as exc:
        raise SystemExit(str(exc))
    if not current_run.is_dir():
        raise SystemExit("找不到当次目录: {}（先在 pipeline/work 下建好这次的 spec/计划）".format(current_run))
    report_dir = current_run / "验收报告"

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        raise SystemExit("缺少 playwright。请执行: pip install -r pipeline/bin/requirements-admin-e2e.txt && playwright install chromium")

    day = date.today().isoformat()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    report_path = report_dir / f"验收报告-{day}.md"
    shot_path = report_dir / f"screenshot-{stamp}.png"
    base = args.base_url.rstrip("/")
    target = args.path if args.path.startswith("/") else f"/{args.path}"
    target_url = f"{base}{target}"

    checks: list[tuple[str, str]] = []
    ok = True
    error = ""

    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=not args.headed)
        context = browser.new_context(
            ignore_https_errors=True,
            viewport={"width": 1440, "height": 900},
        )
        page = context.new_page()
        try:
            page.goto(target_url, wait_until="domcontentloaded", timeout=30000)
            page.wait_for_timeout(800)
            if "/login" in page.url:
                ok = False
                error = f"打开 {target} 被带到登录页（下载站不得走 92 登录）。"
                checks.append((f"打开 {target}", "失败"))
            else:
                checks.append((f"打开 {target}", "通过"))
            if ok:
                for label in args.click:
                    try:
                        if label in ("中", "中文", "EN"):
                            sel = page.locator("select#lang-toggle")
                            if sel.count():
                                sel.first.select_option("en" if label == "EN" else "zh")
                                page.wait_for_timeout(400)
                                checks.append((f"点击「{label}」", "通过"))
                                continue
                            tog = page.locator("#lang-toggle")
                            if tog.count() and tog.first.is_visible():
                                tog.first.click(timeout=3000)
                                page.wait_for_timeout(200)
                        click_visible_text(page, label)
                        page.wait_for_timeout(400)
                        checks.append((f"点击「{label}」", "通过"))
                    except Exception:
                        ok = False
                        error = f"未找到可点击文案: {label}"
                        checks.append((f"点击「{label}」", "失败"))
                        break
            if ok:
                try:
                    page.wait_for_timeout(200)
                    if args.expect_text not in page.content():
                        raise AssertionError("页面未出现期望文案: {}".format(args.expect_text))
                    checks.append((f"出现文案「{args.expect_text}」", "通过"))
                except Exception as exc:
                    ok = False
                    error = str(exc)
                    checks.append((f"出现文案「{args.expect_text}」", "失败"))
            if ok and args.check_files:
                try:
                    check_download_files(page, context, base, checks)
                except Exception as exc:
                    ok = False
                    error = str(exc)
                    checks.append(("核对安装包下载", "失败"))

            page.screenshot(path=str(shot_path), full_page=True)
        except Exception as exc:
            ok = False
            error = str(exc)
            try:
                page.screenshot(path=str(shot_path), full_page=True)
            except Exception:
                pass
        finally:
            context.close()
            browser.close()

    rel_shot = shot_path.relative_to(REPO).as_posix()
    rows = [
        f"# 验收报告 {day}",
        "",
        f"- 标题: {args.title}",
        f"- 模块: {args.module or '（未填）'}",
        f"- 时间: {datetime.now().isoformat(timespec='seconds')}",
        f"- 回修轮次: {args.round}",
        f"- 打开: {target_url}",
        f"- 结果: {'通过' if ok else '失败'}",
        f"- 截图: `{rel_shot}`",
        "",
        "| 断言 | 结果 |",
        "|------|------|",
    ]
    for name, result in checks:
        rows.append(f"| {name} | {result} |")
    if error:
        rows.extend(["", "## 失败原因", "", error])
    write_report(report_path, rows)

    summary = "#### {}\n- 打开: {}\n- 模块: {}\n- 轮次: {}\n- 结果: {}\n- 截图: `{}`\n".format(
        args.title,
        target_url,
        args.module or "（未填）",
        args.round,
        "通过" if ok else "失败",
        rel_shot,
    )
    if error:
        summary += "- 原因: {}\n".format(error)
    notify_dingtalk(
        "通知 验收{} {}".format("通过" if ok else "失败", args.title),
        summary,
    )
    append_step(
        args.run,
        "accept",
        "通过" if ok else "失败",
        detail="{} {} {}".format(target, args.expect_text or "", error).strip(),
        scope=args.module or args.title,
    )

    if not ok:
        print(f"[accept-download] 失败: {error}", file=sys.stderr)
        return 1
    print("[accept-download] 通过")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
