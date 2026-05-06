#!/usr/bin/env python3
from pathlib import Path
import shutil
import sys


def copy_file(src: Path, dst_dir: Path) -> None:
    if not src.exists():
        raise FileNotFoundError(f"源文件不存在: {src}")
    dst_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst_dir / src.name)


def copy_tree_contents(src_dir: Path, dst_dir: Path) -> None:
    if not src_dir.exists():
        raise FileNotFoundError(f"源目录不存在: {src_dir}")
    dst_dir.mkdir(parents=True, exist_ok=True)
    for item in src_dir.iterdir():
        target = dst_dir / item.name
        if item.is_dir():
            shutil.copytree(item, target, dirs_exist_ok=True)
        else:
            shutil.copy2(item, target)


def main() -> int:
    base = Path(__file__).resolve().parent

    print("begin copy sql")
    copy_file(base.parent / "sql" / "ry_20260402.sql", base / "mysql" / "db")
    copy_file(base.parent / "sql" / "ry_config_20250902.sql", base / "mysql" / "db")

    print("begin copy html")
    copy_tree_contents(base.parent / "ruoyi-ui" / "dist", base / "nginx" / "html" / "dist")

    print("begin copy ruoyi-gateway")
    copy_file(base.parent / "ruoyi-gateway" / "target" / "ruoyi-gateway.jar", base / "ruoyi" / "gateway" / "jar")

    print("begin copy ruoyi-auth")
    copy_file(base.parent / "ruoyi-auth" / "target" / "ruoyi-auth.jar", base / "ruoyi" / "auth" / "jar")

    print("begin copy ruoyi-visual")
    copy_file(base.parent / "ruoyi-visual" / "ruoyi-monitor" / "target" / "ruoyi-visual-monitor.jar", base / "ruoyi" / "visual" / "monitor" / "jar")

    print("begin copy ruoyi-modules-system")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-system" / "target" / "ruoyi-modules-system.jar", base / "ruoyi" / "modules" / "system" / "jar")

    print("begin copy ruoyi-modules-file")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-file" / "target" / "ruoyi-modules-file.jar", base / "ruoyi" / "modules" / "file" / "jar")

    print("begin copy ruoyi-modules-job")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-job" / "target" / "ruoyi-modules-job.jar", base / "ruoyi" / "modules" / "job" / "jar")

    print("begin copy ruoyi-modules-gen")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-gen" / "target" / "ruoyi-modules-gen.jar", base / "ruoyi" / "modules" / "gen" / "jar")

    print("begin copy ruoyi-modules-yianlian")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-yianlian" / "target" / "ruoyi-modules-yianlian.jar", base / "ruoyi" / "modules" / "yianlian" / "jar")

    print("copy finished.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as e:
        print(f"copy failed: {e}", file=sys.stderr)
        raise SystemExit(1)
