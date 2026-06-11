#!/usr/bin/env python3
"""构建产物分发到 docker/node-9x。注意：本文件不可命名为 copy.py 运行时遮蔽标准库 copy 模块。"""
import sys
from pathlib import Path

# 避免 docker/copy.py 被当作标准库 copy 模块导入（tarfile 等依赖会触发）
_script_dir = str(Path(__file__).resolve().parent)
if sys.path and Path(sys.path[0]).resolve() == Path(_script_dir).resolve():
    sys.path.pop(0)

import shutil
import struct
import tarfile
import zipfile

JAVA8_CLASS_MAJOR = 52


def check_jar_java8(jar_path: Path) -> None:
    """校验 Spring Boot JAR 主类是否为 Java 8 编译（class major 52）。"""
    if not jar_path.exists():
        raise FileNotFoundError(f"JAR 不存在: {jar_path}")
    with zipfile.ZipFile(jar_path) as zf:
        names = [
            n for n in zf.namelist()
            if n.endswith("Application.class") and "BOOT-INF/classes" in n
        ]
        if not names:
            raise ValueError(f"{jar_path} 中未找到 Application.class")
        major = struct.unpack(">H", zf.read(names[0])[6:8])[0]
        if major != JAVA8_CLASS_MAJOR:
            raise ValueError(
                f"{jar_path} 为 Java class {major}，需要 Java 8（{JAVA8_CLASS_MAJOR}）。"
                "请用 JDK 8 执行 mvn package。"
            )
    print(f"OK Java8: {jar_path}")


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


def copy_html_dist(src_dir: Path, html_dir: Path) -> None:
    """复制前端 dist，并打包为 html_dir/dist.tar。"""
    dist_dir = html_dir / "dist"
    tar_path = html_dir / "dist.tar"
    if not src_dir.exists():
        raise FileNotFoundError(f"源目录不存在: {src_dir}")
    if tar_path.exists():
        print(f"remove old {tar_path}")
        tar_path.unlink()
    if dist_dir.exists():
        shutil.rmtree(dist_dir)
    copy_tree_contents(src_dir, dist_dir)
    print(f"pack {tar_path}")
    with tarfile.open(tar_path, "w") as tar:
        tar.add(dist_dir, arcname="dist")


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
    copy_html_dist(base.parent / "ruoyi-ui" / "dist", node92 / "nginx" / "html")

    print("begin copy vpn html")
    copy_html_dist(base.parent / "ruoyi-vpn-ui" / "dist", node93 / "ruoyi" / "vpn" / "nginx" / "html")

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

    print("begin check jar java version (must be 52 / Java 8)")
    jar_paths = [
        node92 / "ruoyi" / "gateway" / "jar" / "ruoyi-gateway.jar",
        node92 / "ruoyi" / "auth" / "jar" / "ruoyi-auth.jar",
        node92 / "ruoyi" / "visual" / "monitor" / "jar" / "ruoyi-visual-monitor.jar",
        node92 / "ruoyi" / "modules" / "system" / "jar" / "ruoyi-modules-system.jar",
        node92 / "ruoyi" / "modules" / "file" / "jar" / "ruoyi-modules-file.jar",
        node92 / "ruoyi" / "modules" / "job" / "jar" / "ruoyi-modules-job.jar",
        node92 / "ruoyi" / "modules" / "gen" / "jar" / "ruoyi-modules-gen.jar",
        node92 / "ruoyi" / "modules" / "yianlian" / "jar" / "ruoyi-modules-yianlian.jar",
        node93 / "ruoyi" / "vpn" / "gateway" / "jar" / "ruoyi-vpn-gateway.jar",
        node93 / "ruoyi" / "vpn" / "auth" / "jar" / "ruoyi-vpn-auth.jar",
    ]
    for jar_path in jar_paths:
        check_jar_java8(jar_path)

    print("copy finished.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as e:
        print(f"copy failed: {e}", file=sys.stderr)
        raise SystemExit(1)
