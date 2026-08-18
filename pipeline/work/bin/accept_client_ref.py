#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""桌面端 AI 基础走查（参考，≠ PHASE2 通过）。不改客户端源码。

  python -u pipeline/bin/accept.py client-ref --run <当次> --scene off --step prepare
  python -u pipeline/bin/accept.py client-ref --run <当次> --scene off --step submit --code 12

账号只读 CLIENT_E2E_USER / CLIENT_E2E_PASSWORD。验证码由执行走查的 Agent 读图后回填。
流程与踩坑：pipeline/work/_shared/client-ref-走查流程.md

"""
from __future__ import annotations

import argparse
import ctypes
import json
import os
import subprocess
import sys
import time
from ctypes import wintypes
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

USER32 = ctypes.windll.user32
GDI32 = ctypes.windll.gdi32

ULONG_PTR = ctypes.c_size_t


class KEYBDINPUT(ctypes.Structure):
    _fields_ = [
        ("wVk", wintypes.WORD),
        ("wScan", wintypes.WORD),
        ("dwFlags", wintypes.DWORD),
        ("time", wintypes.DWORD),
        ("dwExtraInfo", ULONG_PTR),
    ]


class INPUT(ctypes.Structure):
    _fields_ = [("type", wintypes.DWORD), ("ki", KEYBDINPUT), ("padding", ctypes.c_ubyte * 8)]


class BITMAPINFOHEADER(ctypes.Structure):
    _fields_ = [
        ("biSize", wintypes.DWORD),
        ("biWidth", ctypes.c_long),
        ("biHeight", ctypes.c_long),
        ("biPlanes", wintypes.WORD),
        ("biBitCount", wintypes.WORD),
        ("biCompression", wintypes.DWORD),
        ("biSizeImage", wintypes.DWORD),
        ("biXPelsPerMeter", ctypes.c_long),
        ("biYPelsPerMeter", ctypes.c_long),
        ("biClrUsed", wintypes.DWORD),
        ("biClrImportant", wintypes.DWORD),
    ]

# 登录窗客户区相对位置（720x480 设计稿，不依赖 objectName）
# 紧凑登录页实测：验证码行约 0.50，复选框约 0.59，主按钮约 0.68
POS_USER = (0.50, 0.30)
POS_PASS = (0.50, 0.40)
POS_CAPTCHA = (0.38, 0.50)
POS_LOGIN = (0.50, 0.68)
NAVY_RGB = (11, 45, 91)  # Theme.navy #0B2D5B

STATE_NAME = "client-ref-state.json"


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="客户端 AI 基础走查（参考）")
    p.add_argument("--run", required=True, help="当次目录名")
    p.add_argument("--scene", required=True, choices=("off", "admin-set", "on"))
    p.add_argument("--step", required=True, choices=("prepare", "submit", "refresh"))
    p.add_argument("--code", default="", help="验证码算式结果，submit 时由 Agent 填入")
    p.add_argument("--exe", default="", help="可选，指定客户端 exe")
    return p.parse_args()


def find_client_exe(explicit: str) -> Path:
    if explicit:
        path = Path(explicit)
        if not path.is_file():
            raise SystemExit("指定的 exe 不存在: {}".format(path))
        return path
    dist = REPO / "ruoyi-vpn-client" / "dist"
    matches = sorted(dist.glob("GenlotVPN-win64-*/GenlotVPN-*.exe"))
    if not matches:
        raise SystemExit("未找到打包 exe，请先 gate 客户端。目录: {}".format(dist))
    return matches[-1]


def state_path(run_folder: Path) -> Path:
    return run_folder / STATE_NAME


def load_state(run_folder: Path) -> dict:
    path = state_path(run_folder)
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def save_state(run_folder: Path, data: dict) -> None:
    state_path(run_folder).write_text(
        json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def stamp() -> str:
    return datetime.now().strftime("%Y%m%d-%H%M%S")


def enable_dpi() -> None:
    try:
        USER32.SetProcessDPIAware()
    except Exception:
        pass


def enum_windows_by_title(prefix: str) -> list:
    found = []

    @ctypes.WINFUNCTYPE(ctypes.c_int, wintypes.HWND, wintypes.LPARAM)
    def cb(hwnd, _lp):
        if not USER32.IsWindowVisible(hwnd):
            return 1
        buf = (wintypes.WCHAR * 512)()
        USER32.GetWindowTextW(hwnd, buf, 512)
        title = buf.value or ""
        if title.startswith(prefix) or prefix in title:
            found.append((hwnd, title))
        return 1

    USER32.EnumWindows(cb, 0)
    return found


def wait_main_window(timeout: float = 25.0):
    deadline = time.time() + timeout
    while time.time() < deadline:
        hits = enum_windows_by_title("Genlot VPN")
        if hits:
            return hits[0]
        time.sleep(0.4)
    raise RuntimeError("未找到标题含 Genlot VPN 的窗口")


def client_rect(hwnd) -> tuple:
    rect = wintypes.RECT()
    USER32.GetClientRect(hwnd, ctypes.byref(rect))
    pt = wintypes.POINT(0, 0)
    USER32.ClientToScreen(hwnd, ctypes.byref(pt))
    return pt.x, pt.y, rect.right, rect.bottom


def click_frac(hwnd, fx: float, fy: float) -> None:
    USER32.SetForegroundWindow(hwnd)
    time.sleep(0.15)
    sx, sy, w, h = client_rect(hwnd)
    x = int(sx + w * fx)
    y = int(sy + h * fy)
    USER32.SetCursorPos(x, y)
    USER32.mouse_event(0x0002, 0, 0, 0, 0)
    USER32.mouse_event(0x0004, 0, 0, 0, 0)
    # 再向客户区发一次，避免 Qt 吃掉全局 mouse_event
    cx = int(w * fx)
    cy = int(h * fy)
    lp = (cy << 16) | (cx & 0xFFFF)
    USER32.PostMessageW(hwnd, 0x0201, 0x0001, lp)  # WM_LBUTTONDOWN
    USER32.PostMessageW(hwnd, 0x0202, 0, lp)  # WM_LBUTTONUP
    time.sleep(0.12)


def grab_window_image(hwnd):
    from PIL import Image

    sx, sy, w, h = client_rect(hwnd)
    if w <= 0 or h <= 0:
        raise RuntimeError("窗口客户区无效")
    hwnd_dc = USER32.GetDC(hwnd)
    mem_dc = GDI32.CreateCompatibleDC(hwnd_dc)
    bmp = GDI32.CreateCompatibleBitmap(hwnd_dc, w, h)
    GDI32.SelectObject(mem_dc, bmp)
    if not USER32.PrintWindow(hwnd, mem_dc, 2):
        GDI32.BitBlt(mem_dc, 0, 0, w, h, hwnd_dc, 0, 0, 0x00CC0020)
    hdr = BITMAPINFOHEADER(40, w, -h, 1, 32, 0, 0, 0, 0, 0, 0)
    raw = (ctypes.c_char * (w * h * 4))()
    GDI32.GetDIBits(mem_dc, bmp, 0, h, raw, ctypes.byref(hdr), 0)
    GDI32.DeleteObject(bmp)
    GDI32.DeleteDC(mem_dc)
    USER32.ReleaseDC(hwnd, hwnd_dc)
    return Image.frombytes("RGB", (w, h), bytes(raw), "raw", "BGRX")


def find_login_button_frac(hwnd) -> tuple:
    """在下半窗找海军蓝主按钮中心，找不到则回退 POS_LOGIN。"""
    try:
        img = grab_window_image(hwnd)
    except Exception:
        return POS_LOGIN
    w, h = img.size
    pix = img.load()
    ys, xs = [], []
    y0 = int(h * 0.52)
    y1 = int(h * 0.82)
    x0 = int(w * 0.18)
    x1 = int(w * 0.82)
    for y in range(y0, y1, 2):
        for x in range(x0, x1, 2):
            r, g, b = pix[x, y][:3]
            if abs(r - NAVY_RGB[0]) < 18 and abs(g - NAVY_RGB[1]) < 18 and abs(b - NAVY_RGB[2]) < 18:
                xs.append(x)
                ys.append(y)
    if len(xs) < 30:
        return POS_LOGIN
    fx = (min(xs) + max(xs)) / 2.0 / w
    fy = (min(ys) + max(ys)) / 2.0 / h
    print("[client-ref] 主按钮约 ({:.3f},{:.3f})".format(fx, fy), flush=True)
    return fx, fy


def send_unicode(text: str) -> None:
    for ch in text:
        down = INPUT(1, KEYBDINPUT(0, ord(ch), 4, 0, 0))
        up = INPUT(1, KEYBDINPUT(0, ord(ch), 4 | 2, 0, 0))
        USER32.SendInput(1, ctypes.byref(down), ctypes.sizeof(INPUT))
        USER32.SendInput(1, ctypes.byref(up), ctypes.sizeof(INPUT))
        time.sleep(0.01)


def send_vk(vk: int, ctrl: bool = False) -> None:
    if ctrl:
        USER32.keybd_event(0x11, 0, 0, 0)
    USER32.keybd_event(vk, 0, 0, 0)
    USER32.keybd_event(vk, 0, 2, 0)
    if ctrl:
        USER32.keybd_event(0x11, 0, 2, 0)
    time.sleep(0.08)


def type_into(hwnd, fx: float, fy: float, text: str, clear: bool = True) -> None:
    click_frac(hwnd, fx, fy)
    if clear:
        send_vk(0x41, ctrl=True)  # Ctrl+A
        send_vk(0x2E)  # Delete
    send_unicode(text)


def screenshot_hwnd(hwnd, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    try:
        grab_window_image(hwnd).save(dest)
    except ImportError:
        raise SystemExit("缺少 pillow，请 pip install pillow（已写入 requirements-admin-e2e.txt）")


def ensure_on_login(hwnd):
    kind = detect_result(hwnd)
    if not kind.startswith("left-login"):
        return hwnd
    print("[client-ref] 已登录，点退出登录", flush=True)
    click_frac(hwnd, 0.92, 0.055)
    deadline = time.time() + 15
    while time.time() < deadline:
        hits = enum_windows_by_title("Genlot VPN")
        if not hits:
            time.sleep(0.4)
            continue
        hwnd = hits[0][0]
        if detect_result(hwnd) == "still-login-or-unknown":
            return hwnd
        time.sleep(0.4)
    raise RuntimeError("退出登录后未回到登录页")


def launch_or_attach(exe: Path):
    hits = enum_windows_by_title("Genlot VPN")
    if hits:
        hwnd, title = hits[0]
        print("[client-ref] 复用已开窗口: {}".format(title), flush=True)
        hwnd = ensure_on_login(hwnd)
        return hwnd, title
    print("[client-ref] 启动 {}".format(exe), flush=True)
    subprocess.Popen([str(exe)], cwd=str(exe.parent))
    hwnd, title = wait_main_window()
    print("[client-ref] 窗口: {}".format(title), flush=True)
    return hwnd, title


def fill_user_pass(hwnd) -> None:
    user = env("CLIENT_E2E_USER")
    password = env("CLIENT_E2E_PASSWORD")
    if not user or not password:
        raise SystemExit("缺少 CLIENT_E2E_USER / CLIENT_E2E_PASSWORD，只放 envs.yaml，不要写进对话。")
    type_into(hwnd, *POS_USER, user)
    type_into(hwnd, *POS_PASS, password)


def write_ref_report(report_dir: Path, scene: str, step: str, result: str, shots: list, note: str) -> None:
    day = date.today().isoformat()
    path = report_dir / "验收报告-{}.md".format(day)
    rows = [
        "# 客户端参考走查 {}".format(day),
        "",
        "- 场景: {}".format(scene),
        "- 步骤: {}".format(step),
        "- 时间: {}".format(datetime.now().isoformat(timespec="seconds")),
        "- 结果: {}".format(result),
        "- 说明: 参考走查，不等于 PHASE2 通过",
        "",
    ]
    for shot in shots:
        rows.append("- 截图: `{}`".format(shot))
    if note:
        rows.extend(["", note])
    block = "\n".join(rows).rstrip() + "\n"
    report_dir.mkdir(parents=True, exist_ok=True)
    if path.is_file():
        path.write_text(path.read_text(encoding="utf-8").rstrip() + "\n\n---\n\n" + block, encoding="utf-8")
    else:
        path.write_text(block, encoding="utf-8")
    print("[client-ref] 报告: {}".format(path), flush=True)


def step_prepare_client(args, run_folder: Path, report_dir: Path) -> int:
    enable_dpi()
    exe = find_client_exe(args.exe)
    hwnd, title = launch_or_attach(exe)
    time.sleep(1.0)
    fill_user_pass(hwnd)
    captcha = report_dir / "client-ref-captcha-{}-{}.png".format(args.scene, stamp())
    screenshot_hwnd(hwnd, captcha)
    save_state(
        run_folder,
        {"hwnd": int(hwnd), "title": title, "scene": args.scene, "captcha": str(captcha)},
    )
    rel = captcha.relative_to(REPO).as_posix()
    print("CAPTCHA_PATH={}".format(captcha), flush=True)
    print("[client-ref] 已截验证码（整窗，含算式图）: {}".format(rel), flush=True)
    write_ref_report(report_dir, args.scene, "prepare", "参考进行中", [rel], "等待 Agent 认图后 submit")
    return 0


def step_refresh(args, run_folder: Path, report_dir: Path) -> int:
    enable_dpi()
    hwnd, _title = wait_main_window(8)
    # 点验证码图刷新（登录页右侧）
    click_frac(hwnd, 0.78, 0.50)
    time.sleep(0.8)
    captcha = report_dir / "client-ref-captcha-{}-{}.png".format(args.scene, stamp())
    screenshot_hwnd(hwnd, captcha)
    st = load_state(run_folder)
    st["captcha"] = str(captcha)
    st["hwnd"] = int(hwnd)
    save_state(run_folder, st)
    print("CAPTCHA_PATH={}".format(captcha), flush=True)
    return 0


def detect_result(hwnd) -> str:
    boxes = enum_windows_by_title("需要升级客户端")
    if boxes:
        return "upgrade-dialog"
    _sx, _sy, w, h = client_rect(hwnd)
    if w >= 800 or h >= 520:
        return "left-login-wide"
    if w <= 680 and h <= 420:
        return "left-login-choose"
    return "still-login-or-unknown"


def step_submit_client(args, run_folder: Path, report_dir: Path) -> int:
    if not args.code.strip():
        raise SystemExit("submit 需要 --code（由 Agent 读图后填入，不要问人）")
    enable_dpi()
    hwnd, _title = wait_main_window(8)
    fill_user_pass(hwnd)
    type_into(hwnd, *POS_CAPTCHA, args.code.strip())
    send_vk(0x0D)
    time.sleep(0.3)
    click_frac(hwnd, *find_login_button_frac(hwnd))
    kind = "still-login-or-unknown"
    deadline = time.time() + 10
    while time.time() < deadline:
        kind = detect_result(hwnd)
        if kind != "still-login-or-unknown":
            break
        time.sleep(0.4)
    # 升级弹窗可能是独立 HWND
    result_hwnd = hwnd
    boxes = enum_windows_by_title("需要升级客户端")
    if boxes:
        result_hwnd = boxes[0][0]
    shot = report_dir / "client-ref-result-{}-{}.png".format(args.scene, stamp())
    try:
        screenshot_hwnd(result_hwnd, shot)
    except Exception:
        screenshot_hwnd(hwnd, shot)
    rel = shot.relative_to(REPO).as_posix()
    print("RESULT_PATH={}".format(shot), flush=True)
    print("[client-ref] 探测: {}".format(kind), flush=True)
    expect_ok = False
    if args.scene == "on" and kind == "upgrade-dialog":
        expect_ok = True
    if args.scene == "off" and kind.startswith("left-login"):
        expect_ok = True
    result = "参考完成" if expect_ok else "参考失败"
    note = "探测={}; 仅供人验对照，不算 PHASE2 通过。未点打开下载页。".format(kind)
    write_ref_report(report_dir, args.scene, "submit", result, [rel], note)
    append_step(args.run, "client-ref", result, detail="{} {}".format(args.scene, kind), scope=args.scene)
    return 0 if expect_ok else 2


def admin_set(args, report_dir: Path, do_save: bool) -> int:
    user = env("ADMIN_E2E_USER")
    password = env("ADMIN_E2E_PASSWORD")
    if not user or not password:
        raise SystemExit("admin-set 需要 ADMIN_E2E_USER / ADMIN_E2E_PASSWORD")
    base = env("ADMIN_E2E_BASE_URL", "http://10.27.0.92").rstrip("/")
    from playwright.sync_api import sync_playwright

    shots = []
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        try:
            page.goto(base + "/login", wait_until="domcontentloaded", timeout=30000)
            page.locator("input[placeholder='账号']").wait_for(timeout=15000)
            captcha = page.locator("input[placeholder='验证码']")
            try:
                captcha.wait_for(state="detached", timeout=15000)
            except Exception:
                raise SystemExit("管理端登录页仍有验证码，只关 92 Nacos security.captcha.enabled")
            page.fill("input[placeholder='账号']", user)
            page.fill("input[placeholder='密码']", password)
            page.click("button.el-button--primary")
            page.wait_for_selector(".navbar, .sidebar-container", timeout=20000)
            page.goto(base + "/yianlian/clientVersion", wait_until="domcontentloaded", timeout=30000)
            page.get_by_text("客户端版本策略", exact=False).first.wait_for(timeout=8000)
            page.locator(".el-loading-mask").first.wait_for(state="hidden", timeout=15000)
            if do_save:
                sw = page.locator(".el-form .el-switch").first
                if "is-checked" not in (sw.get_attribute("class") or ""):
                    sw.locator(".el-switch__core").click()
                page.wait_for_function(
                    "() => document.querySelector('.el-form .el-switch').classList.contains('is-checked')",
                    timeout=5000,
                )
                inners = page.locator(".el-form-item .el-input__inner")
                if inners.count() >= 1:
                    inners.nth(0).fill("9.9.9")
                if inners.count() >= 2:
                    inners.nth(1).fill("https://example.invalid/win")
                if inners.count() >= 3:
                    inners.nth(2).fill("https://example.invalid/mac")
                page.get_by_role("button", name="保存").click()
                page.get_by_text("保存成功", exact=False).first.wait_for(timeout=8000)
                page.locator(".el-loading-mask").first.wait_for(state="hidden", timeout=15000)
                page.wait_for_function(
                    "() => document.querySelector('.el-form .el-switch').classList.contains('is-checked')",
                    timeout=8000,
                )
            shot = report_dir / "client-ref-{}-{}-{}.png".format(
                args.scene, "result" if do_save else "prepare", stamp()
            )
            page.screenshot(path=str(shot), full_page=True)
            shots.append(shot.relative_to(REPO).as_posix())
        finally:
            browser.close()
    result = "参考完成" if do_save else "参考进行中"
    write_ref_report(
        report_dir,
        args.scene,
        args.step,
        result,
        shots,
        "管理端填写最低版本；不测自动下载。" if do_save else "已打开策略页",
    )
    if do_save:
        append_step(args.run, "client-ref", result, detail="admin-set 保存成功", scope="admin-set")
    print("RESULT_PATH={}".format(report_dir / shots[-1].split("/")[-1]) if shots else "", flush=True)
    return 0


def main() -> int:
    if os.name != "nt" and sys.argv[1:2] != ["--help"]:
        # admin-set 可在任意 OS；客户端窗口自动化仅 Windows
        pass
    args = parse_args()
    run_folder = run_dir(args.run)
    if not run_folder.is_dir():
        raise SystemExit("找不到当次目录: {}".format(run_folder))
    report_dir = run_folder / "验收报告"
    report_dir.mkdir(parents=True, exist_ok=True)

    if args.scene == "admin-set":
        return admin_set(args, report_dir, do_save=(args.step == "submit"))

    if os.name != "nt":
        raise SystemExit("桌面端 client-ref 仅支持 Windows")
    if args.step == "prepare":
        return step_prepare_client(args, run_folder, report_dir)
    if args.step == "refresh":
        return step_refresh(args, run_folder, report_dir)
    return step_submit_client(args, run_folder, report_dir)


if __name__ == "__main__":
    raise SystemExit(main())
