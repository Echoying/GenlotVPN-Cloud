#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""兼容入口。正式命令：python pipeline/bin/notify.py"""
from __future__ import annotations

import runpy
from pathlib import Path

runpy.run_path(str(Path(__file__).resolve().parent.parent / "pipeline" / "bin" / "notify.py"), run_name="__main__")
