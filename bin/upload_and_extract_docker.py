#!/usr/bin/env python3
"""
打包本地 docker 目录，上传到远端 /root/GenlotVPN-Cloud，并在服务器解压。

依赖:
    pip install paramiko

示例:
    python docker/upload_and_extract_docker.py --password "genlot#20240709"
    python docker/upload_and_extract_docker.py --host 10.9.2.177 --user root --password "xxx"
"""

from __future__ import annotations

import argparse
import sys
import sysconfig
import importlib.util
from pathlib import Path
import getpass

import paramiko

# 避免同目录 copy.py 覆盖标准库 copy 模块（tarfile 内部依赖）
SCRIPT_DIR = Path(__file__).resolve().parent
sanitized = []
for p in sys.path:
    try:
        resolved = Path(p or ".").resolve()
    except Exception:
        sanitized.append(p)
        continue
    if resolved != SCRIPT_DIR:
        sanitized.append(p)
sys.path = sanitized

stdlib_copy = Path(sysconfig.get_path("stdlib")) / "copy.py"
spec = importlib.util.spec_from_file_location("copy", str(stdlib_copy))
copy_module = importlib.util.module_from_spec(spec)
assert spec and spec.loader
spec.loader.exec_module(copy_module)
sys.modules["copy"] = copy_module

import tarfile


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Pack, upload and extract docker directory on remote server")
    parser.add_argument("--host", default="10.9.2.177", help="SSH host")
    parser.add_argument("--port", type=int, default=22, help="SSH port")
    parser.add_argument("--user", default="root", help="SSH username")
    parser.add_argument("--password", default=None, help="SSH password")
    parser.add_argument(
        "--remote-base",
        default="/root/GenlotVPN-Cloud",
        help="Remote target base directory",
    )
    parser.add_argument(
        "--archive-name",
        default="docker.tar.gz",
        help="Archive file name used locally and remotely",
    )
    parser.add_argument(
        "--keep-remote-archive",
        action="store_true",
        help="Keep remote tar.gz after extraction",
    )
    return parser.parse_args()


def create_archive(repo_root: Path, archive_name: str) -> Path:
    docker_dir = repo_root / "docker"
    if not docker_dir.is_dir():
        raise FileNotFoundError(f"未找到目录: {docker_dir}")

    archive_path = repo_root / archive_name
    print(f"[1/4] 打包目录: {docker_dir} -> {archive_path}")
    with tarfile.open(archive_path, "w:gz") as tf:
        tf.add(docker_dir, arcname="docker")
    return archive_path


def run_remote(ssh: paramiko.SSHClient, command: str) -> None:
    stdin, stdout, stderr = ssh.exec_command(command)
    exit_code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="ignore").strip()
    err = stderr.read().decode("utf-8", errors="ignore").strip()
    if exit_code != 0:
        raise RuntimeError(f"远端命令执行失败({exit_code}): {command}\n{err or out}")


def main() -> int:
    args = parse_args()
    password = args.password or getpass.getpass("SSH password: ")

    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent

    archive_path = create_archive(repo_root, args.archive_name)
    remote_archive = f"{args.remote_base.rstrip('/')}/{args.archive_name}"

    print(f"[2/4] 连接服务器: {args.user}@{args.host}:{args.port}")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(
        hostname=args.host,
        port=args.port,
        username=args.user,
        password=password,
        timeout=30,
    )

    try:
        # Ensure target directory exists before SFTP upload.
        run_remote(ssh, f"mkdir -p '{args.remote_base}'")

        print(f"[3/4] 上传压缩包: {archive_path} -> {remote_archive}")
        sftp = ssh.open_sftp()
        try:
            sftp.put(str(archive_path), remote_archive)
        finally:
            sftp.close()

        print("[4/4] 远端解压并完成替换")
        run_remote(ssh, f"rm -rf '{args.remote_base}/docker'")
        run_remote(ssh, f"tar -xzf '{remote_archive}' -C '{args.remote_base}'")
        if not args.keep_remote_archive:
            run_remote(ssh, f"rm -f '{remote_archive}'")

        print("完成: docker 目录已上传并在远端解压")
        return 0
    finally:
        ssh.close()


if __name__ == "__main__":
    raise SystemExit(main())
