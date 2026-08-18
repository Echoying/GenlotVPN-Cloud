#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""流水线路径。本文件在 pipeline/bin/，仓库根是上两级。"""
from __future__ import annotations

from pathlib import Path


def pipeline_dir() -> Path:
    return Path(__file__).resolve().parent.parent


def repo_root() -> Path:
    return pipeline_dir().parent


def work_dir() -> Path:
    return pipeline_dir() / "work"


def work_bin() -> Path:
    return work_dir() / "bin"


def shared_dir() -> Path:
    return work_dir() / "_shared"


def run_dir(name: str) -> Path:
    """当次流水线目录：pipeline/work/<YYYY-MM-DD-短标题>。"""
    name = (name or "").strip()
    if not name or name in (".", "..") or "/" in name or "\\" in name:
        raise ValueError("非法当次目录名: {!r}".format(name))
    return work_dir() / name
