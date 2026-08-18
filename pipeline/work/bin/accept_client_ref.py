#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""桌面端 AI 基础走查（参考，≠ PHASE2 通过）。不靠 objectName。

  python -u pipeline/bin/accept.py client-ref --run <当次> --scene off --step prepare
  python -u pipeline/bin/accept.py client-ref --run <当次> --scene off --step submit --code 12
  python -u pipeline/bin/accept.py client-ref --run <当次> --scene feedback-guest --step prepare
  python -u pipeline/bin/accept.py client-ref --run <当次> --scene feedback-guest --step submit
  python -u pipeline/bin/accept.py client-ref --run <当次> --scene feedback-login --step prepare
  python -u pipeline/bin/accept.py client-ref --run <当次> --scene feedback-login --step submit --code 12

账号只读 CLIENT_E2E_USER / CLIENT_E2E_PASSWORD。验证码由执行走查的 Agent 读图后回填。
当次额外 scene 以 spec 第 7.3 为准。流程与踩坑：pipeline/work/_shared/client-ref-走查流程.md

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
POS_LOGIN_SETTINGS = (0.94, 0.055)  # 登录页右上「设置」（emphasized 高 38、顶距 10）
POS_TOPBAR_SETTINGS = (0.07, 0.055)  # 已登录顶栏最左「设置」
NAVY_RGB = (11, 45, 91)  # Theme.navy #0B2D5B
NAVY_SOFT_RGB = (46, 95, 143)  # Theme.navySoft #2E5F8F
DANGER_RGB = (214, 69, 69)  # Theme.danger #D64545
SIDEBAR_RGB = (245, 247, 250)  # Theme.settingsSidebarBg #F5F7FA
INPUT_BG_RGB = (236, 239, 244)  # Theme.inputBg #ECEFF4
SETTINGS_NAV_W = 168
SETTINGS_HEADER_H = 44
ATTACH_MAX_BYTES = 400 * 1024

STATE_NAME = "client-ref-state.json"


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="客户端 AI 基础走查（参考）")
    p.add_argument("--run", required=True, help="当次目录名")
    p.add_argument(
        "--scene",
        required=True,
        choices=("off", "admin-set", "on", "feedback-guest", "feedback-login"),
    )
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
    if matches:
        return matches[-1]
    fallback = REPO / "ruoyi-vpn-client" / "build-msvc2022" / "GenlotVPN.exe"
    if fallback.is_file():
        print("[client-ref] dist 无包，回退 {}".format(fallback), flush=True)
        return fallback
    raise SystemExit("未找到打包 exe，请先 gate 客户端。目录: {}；也无 {}".format(dist, fallback))


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
    # PrintWindow 截不到 Qt Quick Image（登录验证码），改截屏幕客户区
    USER32.SetForegroundWindow(hwnd)
    time.sleep(0.08)
    screen_dc = USER32.GetDC(0)
    mem_dc = GDI32.CreateCompatibleDC(screen_dc)
    bmp = GDI32.CreateCompatibleBitmap(screen_dc, w, h)
    GDI32.SelectObject(mem_dc, bmp)
    GDI32.BitBlt(mem_dc, 0, 0, w, h, screen_dc, sx, sy, 0x00CC0020)
    hdr = BITMAPINFOHEADER(40, w, -h, 1, 32, 0, 0, 0, 0, 0, 0)
    raw = (ctypes.c_char * (w * h * 4))()
    GDI32.GetDIBits(mem_dc, bmp, 0, h, raw, ctypes.byref(hdr), 0)
    GDI32.DeleteObject(bmp)
    GDI32.DeleteDC(mem_dc)
    USER32.ReleaseDC(0, screen_dc)
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


def window_title(hwnd) -> str:
    buf = (wintypes.WCHAR * 512)()
    USER32.GetWindowTextW(hwnd, buf, 512)
    return buf.value or ""


def window_class(hwnd) -> str:
    buf = (wintypes.WCHAR * 256)()
    USER32.GetClassNameW(hwnd, buf, 256)
    return buf.value or ""


def settings_nav_frac(w: int, h: int, index: int = 4) -> tuple:
    """侧栏第 index+1 项中心（项高 40、间距 4、顶距 12）。"""
    cx = SETTINGS_NAV_W / 2.0
    cy = SETTINGS_HEADER_H + 12 + index * (40 + 4) + 20
    return cx / max(w, 1), cy / max(h, 1)


def settings_form_fracs(w: int, h: int, has_account: bool) -> dict:
    """设置-问题反馈内容区坐标（680x560 设计稿，按实测客户区换算）。"""
    margin = 24.0
    content_x0 = SETTINGS_NAV_W + margin
    content_w = max(w - SETTINGS_NAV_W - 2 * margin, 80)
    field_cx = content_x0 + content_w / 2.0
    y = SETTINGS_HEADER_H + margin + 20 + 20
    label_h, label_gap, field_h, area_h, group_gap = 12.0, 6.0, 44.0, 88.0, 16.0

    def consume(box_h: float) -> tuple:
        nonlocal y
        cy = y + label_h + label_gap + box_h / 2.0
        y += label_h + label_gap + box_h + group_gap
        return field_cx / w, cy / h

    account = consume(field_h) if has_account else None
    title = consume(field_h)
    desc = consume(area_h)
    consume(field_h)  # 分类
    y += 16  # 空 Flow 后的 spacing
    add_cx = content_x0 + 50.0
    submit_cx = (w - margin) - 44.0
    btn_cy = y + 20.0
    return {
        "account": account,
        "title": title,
        "desc": desc,
        "add": (add_cx / w, btn_cy / h),
        "submit": (submit_cx / w, btn_cy / h),
    }


def save_attach_jpeg(hwnd, dest: Path, max_bytes: int = ATTACH_MAX_BYTES) -> Path:
    dest.parent.mkdir(parents=True, exist_ok=True)
    img = grab_window_image(hwnd)
    if img.mode != "RGB":
        img = img.convert("RGB")
    quality = 85
    while quality >= 40:
        img.save(dest, "JPEG", quality=quality, optimize=True)
        if dest.stat().st_size <= max_bytes:
            print(
                "[client-ref] 附件 {} ({} KB, q={})".format(
                    dest, dest.stat().st_size // 1024, quality
                ),
                flush=True,
            )
            return dest
        quality -= 5
    w, h = img.size
    while dest.stat().st_size > max_bytes and w > 200:
        w, h = int(w * 0.75), int(h * 0.75)
        img = img.resize((max(w, 1), max(h, 1)))
        img.save(dest, "JPEG", quality=40, optimize=True)
    return dest


def find_file_dialog():
    found = []

    @ctypes.WINFUNCTYPE(ctypes.c_int, wintypes.HWND, wintypes.LPARAM)
    def cb(hwnd, _lp):
        if not USER32.IsWindowVisible(hwnd):
            return 1
        title = window_title(hwnd)
        cls = window_class(hwnd)
        low = title.lower()
        if (
            "添加截图" in title
            or title in ("打开", "Open", "选择文件")
            or (cls == "#32770" and ("打开" in title or "截图" in title or "选择" in title or "open" in low))
        ):
            found.append(hwnd)
        return 1

    USER32.EnumWindows(cb, 0)
    return found[0] if found else None


def close_file_dialog() -> None:
    dlg = find_file_dialog()
    if not dlg:
        return
    USER32.SetForegroundWindow(dlg)
    time.sleep(0.1)
    send_vk(0x1B)  # Esc
    time.sleep(0.3)


def set_dialog_filename(dlg, path: Path) -> None:
    WM_SETTEXT = 0x000C
    edits = []

    @ctypes.WINFUNCTYPE(ctypes.c_int, wintypes.HWND, wintypes.LPARAM)
    def cb(hwnd, _lp):
        cls = window_class(hwnd)
        if cls == "Edit":
            edits.append(hwnd)
        return 1

    USER32.EnumChildWindows(dlg, cb, 0)
    if not edits:
        raise RuntimeError("文件框没有文件名输入框")
    USER32.SendMessageW(edits[-1], WM_SETTEXT, 0, str(path))


def click_dialog_open(dlg) -> None:
    buttons = []

    @ctypes.WINFUNCTYPE(ctypes.c_int, wintypes.HWND, wintypes.LPARAM)
    def cb(hwnd, _lp):
        if window_class(hwnd) != "Button":
            return 1
        text = window_title(hwnd)
        if "打开" in text or text.lower().startswith("open") or text in ("OK", "确定"):
            buttons.append(hwnd)
        return 1

    USER32.EnumChildWindows(dlg, cb, 0)
    if buttons:
        USER32.SendMessageW(buttons[0], 0x00F5, 0, 0)  # BM_CLICK
        return
    USER32.PostMessageW(dlg, 0x0111, 1, 0)  # WM_COMMAND IDOK


def ascii_attach_copy(src: Path) -> Path:
    dest = Path(os.environ.get("TEMP") or "C:/Temp") / "client-ref-attach.jpg"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(src.read_bytes())
    return dest


def type_file_dialog_path(path: Path) -> None:
    time.sleep(0.8)
    dlg = None
    deadline = time.time() + 6
    while time.time() < deadline:
        dlg = find_file_dialog()
        if dlg:
            break
        time.sleep(0.2)
    if not dlg:
        titles = []
        @ctypes.WINFUNCTYPE(ctypes.c_int, wintypes.HWND, wintypes.LPARAM)
        def cb(hwnd, _lp):
            if USER32.IsWindowVisible(hwnd):
                t = window_title(hwnd)
                if t:
                    titles.append("{}:{}".format(window_class(hwnd), t))
            return 1
        USER32.EnumWindows(cb, 0)
        print("[client-ref] 可见窗: {}".format("; ".join(titles[:20])), flush=True)
        raise RuntimeError("未找到系统文件框（标题应为打开/添加截图）")
    USER32.SetForegroundWindow(dlg)
    time.sleep(0.15)
    set_dialog_filename(dlg, path)
    time.sleep(0.15)
    click_dialog_open(dlg)
    time.sleep(0.8)
    if not find_file_dialog():
        return
    dlg2 = find_file_dialog()
    USER32.SetForegroundWindow(dlg2)
    time.sleep(0.1)
    set_dialog_filename(dlg2, path)
    click_dialog_open(dlg2)
    time.sleep(0.5)


def wait_kind(hwnd, predicate, timeout: float = 10.0):
    deadline = time.time() + timeout
    while time.time() < deadline:
        hits = enum_windows_by_title("Genlot VPN")
        if hits:
            hwnd = hits[0][0]
            if predicate(detect_result(hwnd)):
                return hwnd
        time.sleep(0.35)
    return hwnd


def click_settings_back(hwnd) -> None:
    _sx, _sy, w, h = client_rect(hwnd)
    click_frac(hwnd, 36.0 / max(w, 1), 22.0 / max(h, 1))


def open_settings_from_login(hwnd):
    candidates = [find_login_settings_frac(hwnd), POS_LOGIN_SETTINGS, (0.91, 0.06)]
    seen = []
    for fx, fy in candidates:
        key = (round(fx, 3), round(fy, 3))
        if key in seen:
            continue
        seen.append(key)
        click_frac(hwnd, fx, fy)
        hwnd = wait_kind(hwnd, lambda k: k == "settings-page", 4)
        if detect_result(hwnd) == "settings-page":
            return hwnd
    return require_settings(hwnd, "登录页点设置")


def open_settings_from_topbar(hwnd):
    click_frac(hwnd, *POS_TOPBAR_SETTINGS)
    return wait_kind(hwnd, lambda k: k == "settings-page", 8)


def open_feedback_section(hwnd, nav_index=3):
    _sx, _sy, w, h = client_rect(hwnd)
    click_frac(hwnd, *settings_nav_frac(w, h, nav_index))
    time.sleep(0.4)
    return hwnd


def fill_feedback_form(hwnd, has_account: bool, title: str, content: str) -> None:
    _sx, _sy, w, h = client_rect(hwnd)
    pos = settings_form_fracs(w, h, has_account)
    if has_account:
        user = env("CLIENT_E2E_USER")
        if not user:
            raise SystemExit("缺少 CLIENT_E2E_USER，只放 envs.yaml，不要写进对话。")
        type_into(hwnd, *pos["account"], user)
    type_into(hwnd, *pos["title"], title)
    type_into(hwnd, *pos["desc"], content)


def click_add_and_choose(hwnd, attach: Path, has_account: bool) -> None:
    close_file_dialog()
    attach = ascii_attach_copy(attach)
    hwnd = require_settings(hwnd, "选图前")
    _sx, _sy, w, h = client_rect(hwnd)
    pos = settings_form_fracs(w, h, has_account)
    print("[client-ref] 表单坐标 add={} submit={}".format(pos["add"], pos["submit"]), flush=True)
    tries = [pos["add"], (0.30, 0.86), (0.28, 0.90), (0.32, 0.82)]
    found = find_navy_soft_frac(hwnd, 0.20, 0.62, 0.62, 0.98)
    if found:
        tries.insert(0, found)
        print("[client-ref] 添加截图约 ({:.3f},{:.3f})".format(*found), flush=True)
    for fx, fy in tries:
        click_frac(hwnd, fx, fy)
        deadline = time.time() + 2.5
        while time.time() < deadline:
            if find_file_dialog():
                type_file_dialog_path(attach)
                return
            time.sleep(0.2)
    type_file_dialog_path(attach)
    if find_file_dialog():
        raise RuntimeError("文件框仍在，未选中附件")


def click_feedback_submit(hwnd, has_account: bool) -> None:
    _sx, _sy, w, h = client_rect(hwnd)
    pos = settings_form_fracs(w, h, has_account)
    click_frac(hwnd, *pos["submit"])


def _color_close(rgb, target, tol: int) -> bool:
    return (
        abs(rgb[0] - target[0]) < tol
        and abs(rgb[1] - target[1]) < tol
        and abs(rgb[2] - target[2]) < tol
    )


def is_settings_window(hwnd) -> bool:
    _sx, _sy, w, h = client_rect(hwnd)
    if not (620 <= w <= 760 and 530 <= h <= 620):
        return False
    try:
        img = grab_window_image(hwnd)
    except Exception:
        return True
    pix = img.load()
    iw, ih = img.size
    hits = 0
    total = 0
    x1 = min(SETTINGS_NAV_W, iw)
    for y in range(min(44, ih), min(ih, 200), 3):
        for x in range(8, max(x1 - 8, 9), 4):
            total += 1
            if _color_close(pix[x, y][:3], SIDEBAR_RGB, 22):
                hits += 1
    return total > 0 and hits / total >= 0.18


def find_navy_soft_frac(hwnd, x0: float, y0: float, x1: float, y1: float):
    try:
        img = grab_window_image(hwnd)
    except Exception:
        return None
    w, h = img.size
    pix = img.load()
    xs, ys = [], []
    for y in range(int(h * y0), min(int(h * y1), h), 1):
        for x in range(int(w * x0), min(int(w * x1), w), 1):
            if _color_close(pix[x, y][:3], NAVY_SOFT_RGB, 28):
                xs.append(x)
                ys.append(y)
    if len(xs) < 10:
        return None
    return (min(xs) + max(xs)) / 2.0 / w, (min(ys) + max(ys)) / 2.0 / h


def find_login_settings_frac(hwnd) -> tuple:
    """在登录页右上找「设置」海军蓝字，找不到则回退 POS_LOGIN_SETTINGS。"""
    try:
        img = grab_window_image(hwnd)
    except Exception:
        return POS_LOGIN_SETTINGS
    w, h = img.size
    pix = img.load()
    xs, ys = [], []
    for y in range(4, min(int(h * 0.16), h), 1):
        for x in range(max(int(w * 0.72), 0), w - 4, 1):
            if _color_close(pix[x, y][:3], NAVY_SOFT_RGB, 28):
                xs.append(x)
                ys.append(y)
    if len(xs) < 12:
        return POS_LOGIN_SETTINGS
    fx = (min(xs) + max(xs)) / 2.0 / w
    fy = (min(ys) + max(ys)) / 2.0 / h
    print("[client-ref] 登录设置约 ({:.3f},{:.3f})".format(fx, fy), flush=True)
    return fx, fy


def require_settings(hwnd, where: str):
    if detect_result(hwnd) == "settings-page":
        return hwnd
    raise RuntimeError("未进入设置页（{}），当前探测={}".format(where, detect_result(hwnd)))


def detect_toast_kind(hwnd) -> str:
    try:
        img = grab_window_image(hwnd)
    except Exception:
        return ""
    w, h = img.size
    pix = img.load()
    navy_n = 0
    red_n = 0
    y0 = int(h * 0.82)
    x0 = int(w * 0.12)
    x1 = int(w * 0.88)
    for y in range(y0, h, 2):
        for x in range(x0, x1, 2):
            rgb = pix[x, y][:3]
            if _color_close(rgb, NAVY_RGB, 18):
                navy_n += 1
            if _color_close(rgb, DANGER_RGB, 28):
                red_n += 1
    if navy_n >= 80:
        return "feedback-ok"
    if red_n >= 80:
        return "feedback-missing-user"
    return ""


def form_has_account_field(hwnd) -> bool:
    """内容区约 44px 高的 inputBg 横条：游客 3 条（账号+标题+分类），已登录 2 条。"""
    try:
        img = grab_window_image(hwnd)
    except Exception:
        return False
    w, h = img.size
    pix = img.load()
    x0 = SETTINGS_NAV_W + 24
    x1 = w - 24
    if x1 <= x0:
        return False
    band_ys = []
    span = max((x1 - x0) / 4.0, 1)
    for y in range(int(h * 0.12), int(h * 0.92)):
        hits = 0
        for x in range(x0, x1, 4):
            if _color_close(pix[x, y][:3], INPUT_BG_RGB, 12):
                hits += 1
        if hits >= span * 0.45:
            band_ys.append(y)
    bands = []
    if band_ys:
        start = prev = band_ys[0]
        for y in band_ys[1:]:
            if y <= prev + 2:
                prev = y
            else:
                bands.append((start, prev))
                start = prev = y
        bands.append((start, prev))
    short = [b for b in bands if 32 <= (b[1] - b[0] + 1) <= 56]
    return len(short) >= 3


def screenshot_hwnd(hwnd, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    try:
        grab_window_image(hwnd).save(dest)
    except ImportError:
        raise SystemExit("缺少 pillow，请 pip install pillow（已写入 requirements-admin-e2e.txt）")


def ensure_on_login(hwnd):
    kind = detect_result(hwnd)
    if kind == "settings-page":
        print("[client-ref] 在设置页，点返回", flush=True)
        click_settings_back(hwnd)
        deadline = time.time() + 8
        while time.time() < deadline:
            hits = enum_windows_by_title("Genlot VPN")
            if hits:
                hwnd = hits[0][0]
                kind = detect_result(hwnd)
                if kind != "settings-page":
                    break
            time.sleep(0.3)
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


def detect_result(hwnd, feedback: bool = False) -> str:
    boxes = enum_windows_by_title("需要升级客户端")
    if boxes:
        return "upgrade-dialog"
    if feedback:
        toast = detect_toast_kind(hwnd)
        if toast:
            return toast
    if is_settings_window(hwnd):
        return "settings-page"
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


def _feedback_result(args, hwnd, report_dir: Path, expect_account: bool) -> int:
    time.sleep(2.0)
    kind = detect_result(hwnd, feedback=True)
    if kind not in ("feedback-ok", "feedback-missing-user"):
        kind = "still-login-or-unknown"
    shot = report_dir / "client-ref-result-{}-{}.png".format(args.scene, stamp())
    screenshot_hwnd(hwnd, shot)
    rel = shot.relative_to(REPO).as_posix()
    has_user = form_has_account_field(hwnd)
    print("RESULT_PATH={}".format(shot), flush=True)
    print("[client-ref] 探测: {} 账号框={}".format(kind, has_user), flush=True)
    if expect_account:
        expect_ok = kind == "feedback-ok" and has_user
        extra = "期望提交成功且图里仍有账号框"
    else:
        expect_ok = kind == "feedback-ok" and not has_user
        extra = "期望提交成功且图里没有账号框/请输入账号"
    result = "参考完成" if expect_ok else "参考失败"
    note = "探测={}; 账号框={}; {}。仅供人验对照，不算 PHASE2 通过。".format(kind, has_user, extra)
    write_ref_report(report_dir, args.scene, "submit", result, [rel], note)
    append_step(args.run, "client-ref", result, detail="{} {}".format(args.scene, kind), scope=args.scene)
    return 0 if expect_ok else 2


def step_prepare_feedback_guest(args, run_folder: Path, report_dir: Path) -> int:
    enable_dpi()
    exe = find_client_exe(args.exe)
    hwnd, title = launch_or_attach(exe)
    time.sleep(0.8)
    hwnd = open_settings_from_login(hwnd)
    hwnd = open_feedback_section(hwnd)
    shot = report_dir / "client-ref-prepare-{}-{}.png".format(args.scene, stamp())
    screenshot_hwnd(hwnd, shot)
    attach = save_attach_jpeg(
        hwnd, report_dir / "client-ref-attach-{}-{}.jpg".format(args.scene, stamp())
    )
    save_state(
        run_folder,
        {
            "hwnd": int(hwnd),
            "title": title,
            "scene": args.scene,
            "prepare": str(shot),
            "attach": str(attach),
        },
    )
    rel = shot.relative_to(REPO).as_posix()
    rel_att = attach.relative_to(REPO).as_posix()
    print("PREPARE_PATH={}".format(shot), flush=True)
    print("ATTACH_PATH={}".format(attach), flush=True)
    write_ref_report(
        report_dir,
        args.scene,
        "prepare",
        "参考进行中",
        [rel, rel_att],
        "未登录：设置 → 问题反馈。submit 可不带 --code。",
    )
    return 0


def step_submit_feedback_guest(args, run_folder: Path, report_dir: Path) -> int:
    enable_dpi()
    hwnd, _title = wait_main_window(8)
    if detect_result(hwnd) != "settings-page":
        hwnd = ensure_on_login(hwnd)
        hwnd = open_settings_from_login(hwnd)
    hwnd = require_settings(hwnd, "feedback-guest submit")
    hwnd = open_feedback_section(hwnd)
    fill_feedback_form(hwnd, True, "client-ref未登录", "走查")
    attach = save_attach_jpeg(
        hwnd, report_dir / "client-ref-attach-{}-{}.jpg".format(args.scene, stamp())
    )
    print("ATTACH_PATH={}".format(attach), flush=True)
    click_add_and_choose(hwnd, attach, True)
    time.sleep(0.5)
    click_feedback_submit(hwnd, True)
    return _feedback_result(args, hwnd, report_dir, expect_account=True)


def step_prepare_feedback_login(args, run_folder: Path, report_dir: Path) -> int:
    rc = step_prepare_client(args, run_folder, report_dir)
    hwnd, _title = wait_main_window(8)
    attach = save_attach_jpeg(
        hwnd, report_dir / "client-ref-attach-{}-{}.jpg".format(args.scene, stamp())
    )
    print("ATTACH_PATH={}".format(attach), flush=True)
    return rc


def step_submit_feedback_login(args, run_folder: Path, report_dir: Path) -> int:
    if not args.code.strip():
        raise SystemExit("feedback-login 的 submit 需要 --code（由 Agent 读图后填入，不要问人）")
    enable_dpi()
    hwnd, _title = wait_main_window(8)
    fill_user_pass(hwnd)
    type_into(hwnd, *POS_CAPTCHA, args.code.strip())
    send_vk(0x0D)
    time.sleep(0.3)
    click_frac(hwnd, *find_login_button_frac(hwnd))
    hwnd = wait_kind(hwnd, lambda k: k.startswith("left-login"), 12)
    if not detect_result(hwnd).startswith("left-login"):
        shot = report_dir / "client-ref-result-{}-{}.png".format(args.scene, stamp())
        screenshot_hwnd(hwnd, shot)
        rel = shot.relative_to(REPO).as_posix()
        print("RESULT_PATH={}".format(shot), flush=True)
        print("[client-ref] 探测: still-login-or-unknown（登录未离开）", flush=True)
        write_ref_report(
            report_dir,
            args.scene,
            "submit",
            "参考失败",
            [rel],
            "登录未离开登录页，未继续反馈。仅供人验对照，不算 PHASE2 通过。",
        )
        append_step(
            args.run, "client-ref", "参考失败", detail="{} still-login-or-unknown".format(args.scene), scope=args.scene
        )
        return 2
    hwnd = open_settings_from_topbar(hwnd)
    hwnd = open_feedback_section(hwnd, 0)
    fill_feedback_form(hwnd, False, "client-ref已登录", "走查")
    attach = save_attach_jpeg(
        hwnd, report_dir / "client-ref-attach-{}-{}.jpg".format(args.scene, stamp())
    )
    print("ATTACH_PATH={}".format(attach), flush=True)
    click_add_and_choose(hwnd, attach, False)
    time.sleep(0.5)
    click_feedback_submit(hwnd, False)
    return _feedback_result(args, hwnd, report_dir, expect_account=False)


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

    if args.scene == "feedback-guest":
        if args.step == "prepare":
            return step_prepare_feedback_guest(args, run_folder, report_dir)
        if args.step == "refresh":
            return step_refresh(args, run_folder, report_dir)
        return step_submit_feedback_guest(args, run_folder, report_dir)

    if args.scene == "feedback-login":
        if args.step == "prepare":
            return step_prepare_feedback_login(args, run_folder, report_dir)
        if args.step == "refresh":
            return step_refresh(args, run_folder, report_dir)
        return step_submit_feedback_login(args, run_folder, report_dir)

    if args.step == "prepare":
        return step_prepare_client(args, run_folder, report_dir)
    if args.step == "refresh":
        return step_refresh(args, run_folder, report_dir)
    return step_submit_client(args, run_folder, report_dir)


if __name__ == "__main__":
    raise SystemExit(main())
