#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""管理端页面验收（Playwright）。打开 92、登录、进目标页、按参数断言、写报告。

账号密码只读 ADMIN_E2E_USER / ADMIN_E2E_PASSWORD，不入库。
验证码仍开时失败并提示先做第 4 步（只关 92 Nacos）。
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
    p = argparse.ArgumentParser(description="管理端 Playwright 验收")
    p.add_argument("--base-url", default=env("ADMIN_E2E_BASE_URL", "http://10.27.0.92"))
    p.add_argument("--path", default=env("ADMIN_E2E_PATH", "/index"), help="登录后打开的路由，如 /vpn/user")
    p.add_argument("--expect-text", default=env("ADMIN_E2E_EXPECT_TEXT"), help="页面应出现的文案")
    p.add_argument("--title", default=env("ADMIN_E2E_TITLE", "管理端页面验收"))
    p.add_argument("--module", default=env("ADMIN_E2E_MODULE", ""), help="模块编号，如 M2")
    p.add_argument("--round", type=int, default=int(env("ADMIN_E2E_ROUND") or "0"), help="回修轮次 0-2")
    p.add_argument("--headed", action="store_true", help="有头模式，便于人工看")
    p.add_argument("--click", action="append", default=[], help="打开目标页后依次点击的可见文案，可重复")
    p.add_argument(
        "--run",
        default=env("ADMIN_E2E_RUN"),
        help="pipeline/work 下的当次目录名，如 2026-08-15-用户线路权限查询",
    )
    return p.parse_args()


def click_visible_text(page, label: str, timeout: int = 15000) -> None:
    """只点当前可见节点。Element UI 下拉会在表格里留一份隐藏副本，.first 会点到它。"""
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
        try:
            page.locator(".el-dropdown-menu__item:visible", has_text=label).first.click(timeout=800)
            return
        except Exception as exc:
            last_err = exc
        page.wait_for_timeout(250)
    raise last_err or RuntimeError("未找到可见文案: {}".format(label))


def write_report(path: Path, rows: list[str]) -> None:
    """同日多次 accept 追加，不覆盖已有人工整理内容。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    block = "\n".join(rows).rstrip() + "\n"
    if path.is_file():
        old = path.read_text(encoding="utf-8")
        path.write_text(old.rstrip() + "\n\n---\n\n" + block, encoding="utf-8")
    else:
        path.write_text(block, encoding="utf-8")
    print(f"[accept-admin] 报告: {path}", flush=True)


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
        print("[accept-admin] 无钉钉凭证，跳过通知", flush=True)
        return
    script = _dingtalk_script()
    if not script.is_file():
        print("[accept-admin] 无 dingtalk-send.py，跳过通知", flush=True)
        return
    result = subprocess.run(
        [sys.executable, str(script), "markdown", "--title", title, "--text", text],
        check=False,
    )
    print("[accept-admin] 钉钉退出码 {}".format(result.returncode), flush=True)


def main() -> int:
    args = parse_args()
    user = env("ADMIN_E2E_USER")
    password = env("ADMIN_E2E_PASSWORD")
    if not user or not password:
        raise SystemExit("缺少 ADMIN_E2E_USER / ADMIN_E2E_PASSWORD，只放环境变量，不要写进仓库。")
    if not args.run:
        raise SystemExit(
            "缺少 --run（或 ADMIN_E2E_RUN）。当次目录例如 --run 2026-08-15-用户线路权限查询"
        )
    try:
        current_run = run_dir(args.run)
    except ValueError as exc:
        raise SystemExit(str(exc))
    if not current_run.is_dir():
        raise SystemExit("找不到当次目录: {}（先在 pipeline/work 下建好这次的 spec/计划）".format(current_run))
    report_dir = current_run / "验收报告"

    try:
        from playwright.sync_api import TimeoutError as PwTimeout
        from playwright.sync_api import sync_playwright
    except ImportError:
        raise SystemExit("缺少 playwright。请执行: pip install -r pipeline/bin/requirements-admin-e2e.txt && playwright install chromium")

    day = date.today().isoformat()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    report_path = report_dir / f"验收报告-{day}.md"
    shot_path = report_dir / f"screenshot-{stamp}.png"
    base = args.base_url.rstrip("/")
    login_url = f"{base}/login"
    target = args.path if args.path.startswith("/") else f"/{args.path}"
    target_url = f"{base}{target}"

    checks: list[tuple[str, str]] = []
    ok = True
    error = ""

    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=not args.headed)
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        try:
            page.goto(login_url, wait_until="domcontentloaded", timeout=30000)
            page.locator("input[placeholder='账号']").wait_for(timeout=15000)
            # 登录页 captchaEnabled 默认 true，等 /code 返回后框才会卸掉
            captcha = page.locator("input[placeholder='验证码']")
            try:
                captcha.wait_for(state="detached", timeout=15000)
                checks.append(("登录页无验证码", "通过"))
            except PwTimeout:
                ok = False
                error = "登录页仍有验证码。只关 92 Nacos 的 security.captcha.enabled，不要删代码。"
                checks.append(("登录页无验证码", "失败"))
            if ok:
                page.fill("input[placeholder='账号']", user)
                page.fill("input[placeholder='密码']", password)
                page.click("button.el-button--primary")
                try:
                    page.wait_for_selector(
                        ".navbar, .sidebar-container, .tags-view-container",
                        timeout=20000,
                    )
                    checks.append(("登录成功进入后台", "通过"))
                except PwTimeout:
                    ok = False
                    error = "登录后未进入后台（账号错误或验证码未关）。"
                    checks.append(("登录成功进入后台", "失败"))

            if ok:
                page.goto(target_url, wait_until="domcontentloaded", timeout=30000)
                page.wait_for_timeout(800)
                try:
                    page.locator(".el-table__body, .app-main").first.wait_for(timeout=15000)
                    if args.click:
                        page.locator(".el-table__row, .el-button").first.wait_for(timeout=15000)
                    page.wait_for_timeout(400)
                except PwTimeout:
                    pass
                if "/login" in page.url:
                    ok = False
                    error = f"打开 {target} 被踢回登录页。"
                    checks.append((f"打开 {target}", "失败"))
                else:
                    checks.append((f"打开 {target}", "通过"))
                if ok:
                    for label in args.click:
                        try:
                            click_visible_text(page, label)
                            page.wait_for_timeout(400)
                            checks.append((f"点击「{label}」", "通过"))
                        except Exception:
                            ok = False
                            error = f"未找到可点击文案: {label}"
                            checks.append((f"点击「{label}」", "失败"))
                            break
                if ok and args.expect_text:
                    try:
                        if args.click:
                            # 点过按钮后优先看弹窗标题，避免整页模糊匹配；
                            # 「保存成功」在 $modal.msgSuccess 的 toast 上，不在 .el-dialog__title。
                            title = page.locator(".el-dialog__title:visible").filter(
                                has_text=args.expect_text
                            )
                            toast = page.locator(".el-message:visible").filter(
                                has_text=args.expect_text
                            )
                            title.or_(toast).first.wait_for(timeout=8000)
                        else:
                            page.get_by_text(args.expect_text, exact=False).first.wait_for(timeout=8000)
                        checks.append((f"出现文案「{args.expect_text}」", "通过"))
                    except PwTimeout:
                        ok = False
                        error = f"页面未出现期望文案: {args.expect_text}"
                        checks.append((f"出现文案「{args.expect_text}」", "失败"))

            page.screenshot(path=str(shot_path), full_page=True)
        except Exception as exc:
            ok = False
            error = str(exc)
            try:
                page.screenshot(path=str(shot_path), full_page=True)
            except Exception:
                pass
        finally:
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
        print(f"[accept-admin] 失败: {error}", file=sys.stderr)
        return 1
    print("[accept-admin] 通过")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
