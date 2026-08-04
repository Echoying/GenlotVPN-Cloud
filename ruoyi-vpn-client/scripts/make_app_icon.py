#!/usr/bin/env python3
"""生成 macOS 应用图标（与 Windows genlot-app.ico 对齐）。

优先使用 assets/images/genlot-app-icon-official.png（与 exe / 托盘同源），
铺到 Big Sur 风格黑底圆角方块上。

输出：
  - assets/images/genlot-app-1024.png
  - assets/images/genlot-app.icns

macOS 推荐：swift scripts/make_app_icon.swift
跨平台：python3 scripts/make_app_icon.py（需 Pillow + numpy）
"""

from __future__ import annotations

import re
import struct
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw

try:
    import numpy as np
except ImportError as exc:  # pragma: no cover
    raise SystemExit("需要 numpy：pip install numpy") from exc

# ---------------------------------------------------------------- 设计参数

CANVAS = 1024
# Big Sur 规范：1024 画布内图标本体 824×824，四周留透明边
BODY = 824
BODY_RADIUS = 184
MARK_BOX = 470  # 标志在本体内的最大边长

SS = 4  # 超采样倍数

NAVY_TOP = (11, 45, 91)      # Theme.navy  #0B2D5B
NAVY_BOTTOM = (8, 28, 58)    # 更深，保证 #1B4F9C 标志可读
MARK_COLOR = (27, 79, 156)   # 与 genlot-logo.svg 一致 #1B4F9C
ORB_STOPS = [
    (0.00, (255, 255, 255)),
    (0.55, (142, 200, 248)),  # #8EC8F8
    (1.00, (61, 143, 212)),   # #3D8FD4
]

# 取自 genlot-logo.svg（viewBox 0 0 360 88）的标志部分
MARK_PATHS = [
    "M44 44c0-18 12-30 30-30 8 0 15 3 20 8-6-2-12-3-18-3-14 0-24 10-24 24 0 10 "
    "6 18 16 22-12-4-24-12-24-21z",
    "M58 18c14 0 24 10 24 24 0 8-4 15-10 19 7-5 12-13 12-22 0-16-13-28-30-28-9 "
    "0-17 3-23 9 6-2 13-2 17-2z",
]
ORB = (52.0, 50.0, 11.0)  # cx, cy, r

_TOKEN = re.compile(r"[MmLlCcZzHhVv]|-?\d*\.?\d+(?:[eE][-+]?\d+)?")
_BEZIER_STEPS = 64


def flatten_path(d: str) -> list[list[tuple[float, float]]]:
    """把 SVG path 展开为多边形点列（仅支持本文件用到的 M/L/C/H/V/Z）。"""
    tokens = _TOKEN.findall(d)
    subpaths: list[list[tuple[float, float]]] = []
    current: list[tuple[float, float]] = []
    x = y = 0.0
    start_x = start_y = 0.0
    cmd = ""
    i = 0

    def num() -> float:
        nonlocal i
        value = float(tokens[i])
        i += 1
        return value

    while i < len(tokens):
        token = tokens[i]
        if token.isalpha():
            cmd = token
            i += 1
            if cmd in "Zz":
                if current:
                    subpaths.append(current)
                    current = []
                x, y = start_x, start_y
                continue
        if not cmd:
            raise ValueError(f"path 缺少命令: {d[:40]}")

        if cmd in "Mm":
            dx, dy = num(), num()
            x, y = (x + dx, y + dy) if cmd == "m" else (dx, dy)
            if current:
                subpaths.append(current)
            current = [(x, y)]
            start_x, start_y = x, y
            cmd = "l" if cmd == "m" else "L"
        elif cmd in "Ll":
            dx, dy = num(), num()
            x, y = (x + dx, y + dy) if cmd == "l" else (dx, dy)
            current.append((x, y))
        elif cmd in "Hh":
            dx = num()
            x = x + dx if cmd == "h" else dx
            current.append((x, y))
        elif cmd in "Vv":
            dy = num()
            y = y + dy if cmd == "v" else dy
            current.append((x, y))
        elif cmd in "Cc":
            x1, y1, x2, y2, x3, y3 = (num() for _ in range(6))
            if cmd == "c":
                x1, y1 = x + x1, y + y1
                x2, y2 = x + x2, y + y2
                x3, y3 = x + x3, y + y3
            p0 = (x, y)
            for step in range(1, _BEZIER_STEPS + 1):
                t = step / _BEZIER_STEPS
                mt = 1 - t
                bx = (mt ** 3 * p0[0] + 3 * mt * mt * t * x1
                      + 3 * mt * t * t * x2 + t ** 3 * x3)
                by = (mt ** 3 * p0[1] + 3 * mt * mt * t * y1
                      + 3 * mt * t * t * y2 + t ** 3 * y3)
                current.append((bx, by))
            x, y = x3, y3
        else:
            raise ValueError(f"不支持的 path 命令: {cmd}")

    if current:
        subpaths.append(current)
    return subpaths


def mark_geometry() -> tuple[list[list[tuple[float, float]]], tuple[float, float, float, float]]:
    """返回标志的多边形点列与整体包围盒（含光球）。"""
    polys: list[list[tuple[float, float]]] = []
    for d in MARK_PATHS:
        polys.extend(flatten_path(d))

    xs = [p[0] for poly in polys for p in poly]
    ys = [p[1] for poly in polys for p in poly]
    cx, cy, r = ORB
    xs += [cx - r, cx + r]
    ys += [cy - r, cy + r]
    return polys, (min(xs), min(ys), max(xs), max(ys))


def vertical_gradient(size: int, top: tuple[int, int, int],
                      bottom: tuple[int, int, int]) -> Image.Image:
    ramp = np.linspace(0.0, 1.0, size, dtype=np.float32)[:, None]
    top_arr = np.array(top, dtype=np.float32)
    bottom_arr = np.array(bottom, dtype=np.float32)
    rows = top_arr * (1 - ramp) + bottom_arr * ramp
    data = np.repeat(rows[:, None, :], size, axis=1).astype(np.uint8)
    return Image.fromarray(data, mode="RGB")


def diagonal_gradient(size: int, stops) -> Image.Image:
    """左上 → 右下的线性渐变，对应 SVG 中 orb 的 0%/55%/100% 三个色标。"""
    axis = np.linspace(0.0, 1.0, size, dtype=np.float32)
    t = (axis[:, None] + axis[None, :]) / 2.0
    offsets = np.array([s[0] for s in stops], dtype=np.float32)
    colors = np.array([s[1] for s in stops], dtype=np.float32)
    channels = [np.interp(t, offsets, colors[:, c]) for c in range(3)]
    data = np.stack(channels, axis=-1).astype(np.uint8)
    return Image.fromarray(data, mode="RGB")


def render_icon() -> Image.Image:
    scale = SS
    canvas = CANVAS * scale
    body = BODY * scale
    radius = BODY_RADIUS * scale
    offset = (canvas - body) // 2

    image = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))

    body_mask = Image.new("L", (body, body), 0)
    ImageDraw.Draw(body_mask).rounded_rectangle(
        (0, 0, body - 1, body - 1), radius=radius, fill=255)
    image.paste(vertical_gradient(body, NAVY_TOP, NAVY_BOTTOM),
                (offset, offset), body_mask)

    polys, (min_x, min_y, max_x, max_y) = mark_geometry()
    mark_w, mark_h = max_x - min_x, max_y - min_y
    factor = (MARK_BOX * scale) / max(mark_w, mark_h)
    tx = (canvas - mark_w * factor) / 2 - min_x * factor
    ty = (canvas - mark_h * factor) / 2 - min_y * factor

    def project(point: tuple[float, float]) -> tuple[float, float]:
        return point[0] * factor + tx, point[1] * factor + ty

    mark_layer = Image.new("L", (canvas, canvas), 0)
    mark_draw = ImageDraw.Draw(mark_layer)
    for poly in polys:
        mark_draw.polygon([project(p) for p in poly], fill=255)
    image.paste(Image.new("RGBA", (canvas, canvas), MARK_COLOR + (255,)),
                (0, 0), mark_layer)

    cx, cy, r = ORB
    ocx, ocy = project((cx, cy))
    orb_r = r * factor
    orb_size = max(2, int(round(orb_r * 2)))
    orb_mask = Image.new("L", (orb_size, orb_size), 0)
    ImageDraw.Draw(orb_mask).ellipse((0, 0, orb_size - 1, orb_size - 1), fill=255)
    orb = diagonal_gradient(orb_size, ORB_STOPS)
    image.paste(orb, (int(round(ocx - orb_r)), int(round(ocy - orb_r))), orb_mask)

    return image.resize((CANVAS, CANVAS), Image.LANCZOS)


def build_svg() -> str:
    _, (min_x, min_y, max_x, max_y) = mark_geometry()
    mark_w, mark_h = max_x - min_x, max_y - min_y
    factor = MARK_BOX / max(mark_w, mark_h)
    tx = (CANVAS - mark_w * factor) / 2 - min_x * factor
    ty = (CANVAS - mark_h * factor) / 2 - min_y * factor
    offset = (CANVAS - BODY) / 2

    def rgb(color: tuple[int, int, int]) -> str:
        return "#%02X%02X%02X" % color

    paths = "\n".join(
        f'      <path fill="{rgb(MARK_COLOR)}" d="{d}"/>' for d in MARK_PATHS)
    cx, cy, r = ORB
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<!-- 由 scripts/make_app_icon.py 生成，勿手工编辑 -->
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {CANVAS} {CANVAS}"
     width="{CANVAS}" height="{CANVAS}" role="img" aria-label="Genlot VPN">
  <defs>
    <linearGradient id="body" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{rgb(NAVY_TOP)}"/>
      <stop offset="100%" stop-color="{rgb(NAVY_BOTTOM)}"/>
    </linearGradient>
    <linearGradient id="orb" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="{rgb(ORB_STOPS[0][1])}"/>
      <stop offset="55%" stop-color="{rgb(ORB_STOPS[1][1])}"/>
      <stop offset="100%" stop-color="{rgb(ORB_STOPS[2][1])}"/>
    </linearGradient>
  </defs>
  <rect x="{offset:g}" y="{offset:g}" width="{BODY}" height="{BODY}"
        rx="{BODY_RADIUS}" ry="{BODY_RADIUS}" fill="url(#body)"/>
  <g transform="translate({tx:.3f} {ty:.3f}) scale({factor:.5f})">
{paths}
      <circle cx="{cx:g}" cy="{cy:g}" r="{r:g}" fill="url(#orb)"/>
  </g>
</svg>
"""


# iconutil 生成的现代 ICNS 类型表：OSType -> 像素尺寸
ICNS_ENTRIES = [
    ("icp4", 16), ("icp5", 32), ("icp6", 64),
    ("ic07", 128), ("ic08", 256), ("ic09", 512), ("ic10", 1024),
    ("ic11", 32), ("ic12", 64), ("ic13", 256), ("ic14", 512),
]


def build_icns(master: Image.Image) -> bytes:
    """优先走 macOS iconutil（系统可识别）；其他平台回退为 PNG 块 ICNS。"""
    import platform
    import shutil
    import subprocess
    import tempfile

    if platform.system() == "Darwin" and shutil.which("iconutil"):
        with tempfile.TemporaryDirectory(prefix="genlot-iconset-") as tmp:
            iconset = Path(tmp) / "genlot-app.iconset"
            iconset.mkdir()
            mapping = [
                ("icon_16x16.png", 16),
                ("icon_16x16@2x.png", 32),
                ("icon_32x32.png", 32),
                ("icon_32x32@2x.png", 64),
                ("icon_128x128.png", 128),
                ("icon_128x128@2x.png", 256),
                ("icon_256x256.png", 256),
                ("icon_256x256@2x.png", 512),
                ("icon_512x512.png", 512),
                ("icon_512x512@2x.png", 1024),
            ]
            for name, size in mapping:
                resized = master if size == master.width else master.resize(
                    (size, size), Image.LANCZOS)
                resized.save(iconset / name, format="PNG")
            out = Path(tmp) / "genlot-app.icns"
            subprocess.check_call(
                ["iconutil", "-c", "icns", str(iconset), "-o", str(out)])
            return out.read_bytes()

    chunks = bytearray()
    for ostype, size in ICNS_ENTRIES:
        buffer = BytesIO()
        resized = master if size == master.width else master.resize(
            (size, size), Image.LANCZOS)
        resized.save(buffer, format="PNG", optimize=True)
        payload = buffer.getvalue()
        chunks += ostype.encode("ascii")
        chunks += struct.pack(">I", len(payload) + 8)
        chunks += payload
    return b"icns" + struct.pack(">I", len(chunks) + 8) + bytes(chunks)


def render_from_official(images_dir: Path) -> Image.Image | None:
    """与 Windows genlot-app.ico 同源：铺 official PNG 到黑底圆角方块。"""
    official_path = images_dir / "genlot-app-icon-official.png"
    if not official_path.is_file():
        return None
    official = Image.open(official_path).convert("RGBA")
    image = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    offset = (CANVAS - BODY) // 2
    body_mask = Image.new("L", (BODY, BODY), 0)
    ImageDraw.Draw(body_mask).rounded_rectangle(
        (0, 0, BODY - 1, BODY - 1), radius=BODY_RADIUS, fill=255)
    body = Image.new("RGBA", (BODY, BODY), (0, 0, 0, 255))
    pad = 28
    dest = BODY - pad * 2
    scaled = official.resize((dest, dest), Image.LANCZOS)
    body.paste(scaled, (pad, pad), scaled)
    image.paste(body, (offset, offset), body_mask)
    return image


def main() -> None:
    images_dir = Path(__file__).resolve().parent.parent / "assets" / "images"
    images_dir.mkdir(parents=True, exist_ok=True)

    master = render_from_official(images_dir)
    if master is None:
        master = render_icon()
        (images_dir / "genlot-app.svg").write_text(build_svg(), encoding="utf-8")
        print("[..] 未找到 genlot-app-icon-official.png，回退 SVG 重绘")
    else:
        print("[..] 使用 genlot-app-icon-official.png（与 Windows 对齐）")

    png_path = images_dir / "genlot-app-1024.png"
    master.save(png_path, format="PNG", optimize=True)

    icns_path = images_dir / "genlot-app.icns"
    icns_path.write_bytes(build_icns(master))

    for path in (png_path, icns_path):
        print(f"[OK] {path.name}  {path.stat().st_size / 1024:.1f} KB")


if __name__ == "__main__":
    main()
