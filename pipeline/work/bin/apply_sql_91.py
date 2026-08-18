#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 sql/update 下的脚本打到 91 MySQL（ry-cloud）。密码不打印。

用法: python pipeline/work/bin/apply_sql_91.py sql/update/20260815_line_auth_query_menu.sql
"""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

_PIPE_BIN = Path(__file__).resolve().parents[2] / "bin"
sys.path.insert(0, str(_PIPE_BIN))
from paths import repo_root  # noqa: E402
from local_env import load as load_local_env  # noqa: E402

REPO = repo_root()

load_local_env()


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def run(ssh, command: str) -> tuple:
    _stdin, stdout, stderr = ssh.exec_command(command)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="ignore").strip()
    err = stderr.read().decode("utf-8", errors="ignore").strip()
    return code, out, err


def main() -> int:
    p = argparse.ArgumentParser(description="在 91 ruoyi-mysql 执行可重入 SQL")
    p.add_argument("sql", help="仓库内 SQL 文件，如 sql/update/xxx.sql")
    args = p.parse_args()
    local = Path(args.sql)
    if not local.is_file():
        local = REPO / args.sql
    if not local.is_file():
        raise SystemExit("找不到 SQL: {}".format(args.sql))

    password = env("DEPLOY_SSH_PASSWORD")
    if not password:
        raise SystemExit("缺少 DEPLOY_SSH_PASSWORD")
    host = env("DEPLOY_SSH_HOST_91") or "10.27.0.91"
    user = env("DEPLOY_SSH_USER") or "root"
    port = int(env("DEPLOY_SSH_PORT") or "22")
    remote = "/tmp/{}".format(local.name)

    try:
        import paramiko
    except ImportError:
        raise SystemExit("缺少 paramiko")

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    print("[apply-sql] 连接 {}@{}".format(user, host), flush=True)
    ssh.connect(hostname=host, port=port, username=user, password=password, timeout=30)
    try:
        sftp = ssh.open_sftp()
        try:
            sftp.put(str(local), remote)
        finally:
            sftp.close()
        code, out, err = run(ssh, "docker cp {} ruoyi-mysql:/tmp/apply.sql".format(remote))
        if code != 0:
            print(err or out, file=sys.stderr)
            return 1
        code, out, err = run(
            ssh,
            "docker exec ruoyi-mysql sh -c "
            "'mysql -uroot -p\"$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 "
            "ry-cloud < /tmp/apply.sql'",
        )
        if out:
            print(out, flush=True)
        if err and "Using a password" not in err and "World-writable" not in err:
            print(err, file=sys.stderr)
        if code != 0:
            return 1
        run(ssh, "rm -f {}".format(remote))
        print("[apply-sql] 完成 {}".format(local.name), flush=True)
        return 0
    finally:
        ssh.close()


if __name__ == "__main__":
    raise SystemExit(main())
