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

    node91 = base / "node-91"
    node92 = base / "node-92"
    node93 = base / "node-93"

    print("begin copy sql")
    copy_file(base.parent / "sql" / "ry-cloud.sql", node91 / "mysql" / "db")
    copy_file(base.parent / "sql" / "ry-config.sql", node91 / "mysql" / "db")
    copy_file(base.parent / "sql" / "quartz.sql", node91 / "mysql" / "db")

    print("begin copy html")
    copy_tree_contents(base.parent / "ruoyi-ui" / "dist", node92 / "nginx" / "html" / "dist")

    print("begin copy vpn html")
    copy_tree_contents(base.parent / "ruoyi-vpn-ui" / "dist", node93 / "ruoyi" / "vpn" / "nginx" / "html" / "dist")

    print("begin copy ruoyi-gateway")
    copy_file(base.parent / "ruoyi-gateway" / "target" / "ruoyi-gateway.jar", node92 / "ruoyi" / "gateway" / "jar")

    print("begin copy ruoyi-vpn-gateway")
    copy_file(base.parent / "ruoyi-vpn-gateway" / "target" / "ruoyi-vpn-gateway.jar", node93 / "ruoyi" / "vpn" / "gateway" / "jar")

    print("begin copy ruoyi-auth")
    copy_file(base.parent / "ruoyi-auth" / "target" / "ruoyi-auth.jar", node92 / "ruoyi" / "auth" / "jar")

    print("begin copy ruoyi-vpn-auth")
    copy_file(base.parent / "ruoyi-vpn-auth" / "target" / "ruoyi-vpn-auth.jar", node93 / "ruoyi" / "vpn" / "auth" / "jar")

    print("begin copy ruoyi-visual")
    copy_file(base.parent / "ruoyi-visual" / "ruoyi-monitor" / "target" / "ruoyi-visual-monitor.jar", node92 / "ruoyi" / "visual" / "monitor" / "jar")

    print("begin copy ruoyi-modules-system")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-system" / "target" / "ruoyi-modules-system.jar", node92 / "ruoyi" / "modules" / "system" / "jar")

    print("begin copy ruoyi-modules-file")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-file" / "target" / "ruoyi-modules-file.jar", node92 / "ruoyi" / "modules" / "file" / "jar")

    print("begin copy ruoyi-modules-job")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-job" / "target" / "ruoyi-modules-job.jar", node92 / "ruoyi" / "modules" / "job" / "jar")

    print("begin copy ruoyi-modules-gen")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-gen" / "target" / "ruoyi-modules-gen.jar", node92 / "ruoyi" / "modules" / "gen" / "jar")

    print("begin copy ruoyi-modules-yianlian")
    copy_file(base.parent / "ruoyi-modules" / "ruoyi-yianlian" / "target" / "ruoyi-modules-yianlian.jar", node92 / "ruoyi" / "modules" / "yianlian" / "jar")

    print("copy finished.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as e:
        print(f"copy failed: {e}", file=sys.stderr)
        raise SystemExit(1)
