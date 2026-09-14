# -*- coding: utf-8 -*-
"""扫 apps/ 生成下载站 manifest（字段名固定英文）。"""
from __future__ import annotations

import json
import re
from pathlib import Path

WIN_CLIENT = re.compile(r"^GenlotVPN-win64-(\d+\.\d+\.\d+)\.zip$", re.I)
MAC_CLIENT = re.compile(r"^GenlotVPN-macos-[^-]+-(\d+\.\d+\.\d+)\.zip$", re.I)
WIN_SDK = re.compile(r"^EnUES.*_SDK\.exe$", re.I)
MAC_SDK = re.compile(r"^EnUES.*_SDK\.pkg$", re.I)
SDK_VER = re.compile(r"v(\d+(?:\.\d+)+)", re.I)


def parse_xyz(text):
    m = re.match(r"^(\d+)\.(\d+)\.(\d+)$", text or "")
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3))


def pick_max_xyz(names, pattern):
    best = None
    best_key = None
    cre = re.compile(pattern, re.I) if isinstance(pattern, str) else pattern
    for name in names:
        m = cre.match(name)
        if not m:
            continue
        key = parse_xyz(m.group(1))
        if key is None:
            continue
        if best_key is None or key > best_key:
            best_key = key
            best = (name, m.group(1))
    return best


def _sdk_version(name):
    m = SDK_VER.search(name)
    return m.group(1) if m else ""


def _sdk_key(name):
    ver = _sdk_version(name)
    parts = []
    for p in ver.split("."):
        try:
            parts.append(int(p))
        except ValueError:
            parts.append(0)
    return tuple(parts)


def pick_max_sdk(names, pattern):
    best = None
    best_key = None
    for name in names:
        if not pattern.match(name):
            continue
        key = _sdk_key(name)
        if best_key is None or key > best_key:
            best_key = key
            best = name
    return best


def _entry(rel_dir, filename, version):
    if not filename:
        return None
    return {"version": version or "", "file": "{}/{}".format(rel_dir, filename)}


def build_manifest(apps_dir):
    apps_dir = Path(apps_dir)
    win_names = [p.name for p in (apps_dir / "windows").glob("*") if p.is_file()]
    mac_names = [p.name for p in (apps_dir / "macos").glob("*") if p.is_file()]
    win_c = pick_max_xyz(win_names, WIN_CLIENT)
    mac_c = pick_max_xyz(mac_names, MAC_CLIENT)
    win_s = pick_max_sdk(win_names, WIN_SDK)
    mac_s = pick_max_sdk(mac_names, MAC_SDK)
    return {
        "windows": {
            "sdk": _entry("windows", win_s, _sdk_version(win_s) if win_s else ""),
            "client": _entry("windows", win_c[0], win_c[1]) if win_c else None,
        },
        "macos": {
            "sdk": _entry("macos", mac_s, _sdk_version(mac_s) if mac_s else ""),
            "client": _entry("macos", mac_c[0], mac_c[1]) if mac_c else None,
        },
    }


def write_manifest(apps_dir, dest):
    data = build_manifest(apps_dir)
    dest = Path(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    return data
