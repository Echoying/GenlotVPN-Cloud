#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""上传管理端（默认 92 全部服务）并重建容器。

默认：gateway / auth / system / gen / job / file / monitor / yianlian / nginx。
--vpn / --all：再上传 93 的 vpn-auth。
--ui：只更新前端。

密码只读 DEPLOY_SSH_PASSWORD，不入库、不打印。
客户端不走本脚本。
"""
from __future__ import annotations

import argparse
import os
import sys
import time
from pathlib import Path

try:
    sys.stdout.reconfigure(line_buffering=True)
    sys.stderr.reconfigure(line_buffering=True)
except Exception:
    pass

_PIPE_BIN = Path(__file__).resolve().parents[2] / "bin"
sys.path.insert(0, str(_PIPE_BIN))
from paths import repo_root  # noqa: E402
from local_env import load as load_local_env  # noqa: E402

REPO = repo_root()

load_local_env()

DOCKER = REPO / "docker"
sys.path.insert(0, str(DOCKER))
from copy_artifacts import check_jar_java8, copy_file, copy_html_dist  # noqa: E402

# 92 上全部可自动部署的服务（与 node-92/docker-compose.yml 对齐）
ADMIN_JARS = [
    {
        "src": REPO / "ruoyi-gateway" / "target" / "ruoyi-gateway.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "gateway" / "jar",
        "remote": "ruoyi/gateway/jar/ruoyi-gateway.jar",
        "service": "ruoyi-gateway",
    },
    {
        "src": REPO / "ruoyi-auth" / "target" / "ruoyi-auth.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "auth" / "jar",
        "remote": "ruoyi/auth/jar/ruoyi-auth.jar",
        "service": "ruoyi-auth",
    },
    {
        "src": REPO / "ruoyi-modules" / "ruoyi-system" / "target" / "ruoyi-modules-system.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "modules" / "system" / "jar",
        "remote": "ruoyi/modules/system/jar/ruoyi-modules-system.jar",
        "service": "ruoyi-modules-system",
    },
    {
        "src": REPO / "ruoyi-modules" / "ruoyi-gen" / "target" / "ruoyi-modules-gen.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "modules" / "gen" / "jar",
        "remote": "ruoyi/modules/gen/jar/ruoyi-modules-gen.jar",
        "service": "ruoyi-modules-gen",
    },
    {
        "src": REPO / "ruoyi-modules" / "ruoyi-job" / "target" / "ruoyi-modules-job.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "modules" / "job" / "jar",
        "remote": "ruoyi/modules/job/jar/ruoyi-modules-job.jar",
        "service": "ruoyi-modules-job",
    },
    {
        "src": REPO / "ruoyi-modules" / "ruoyi-file" / "target" / "ruoyi-modules-file.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "modules" / "file" / "jar",
        "remote": "ruoyi/modules/file/jar/ruoyi-modules-file.jar",
        "service": "ruoyi-modules-file",
    },
    {
        "src": REPO / "ruoyi-visual" / "ruoyi-monitor" / "target" / "ruoyi-visual-monitor.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "visual" / "monitor" / "jar",
        "remote": "ruoyi/visual/monitor/jar/ruoyi-visual-monitor.jar",
        "service": "ruoyi-visual-monitor",
    },
    {
        "src": REPO / "ruoyi-modules" / "ruoyi-yianlian" / "target" / "ruoyi-modules-yianlian.jar",
        "dst": DOCKER / "node-92" / "ruoyi" / "modules" / "yianlian" / "jar",
        "remote": "ruoyi/modules/yianlian/jar/ruoyi-modules-yianlian.jar",
        "service": "ruoyi-modules-yianlian",
    },
]

VPN_JAR = {
    "src": REPO / "ruoyi-vpn-auth" / "target" / "ruoyi-vpn-auth.jar",
    "dst": DOCKER / "node-93" / "ruoyi" / "vpn" / "auth" / "jar",
    "remote": "ruoyi/vpn/auth/jar/ruoyi-vpn-auth.jar",
    "service": "ruoyi-vpn-auth",
}


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="上传并重建 92 全部管理端服务（可选 93 vpn-auth）")
    p.add_argument("--ui", action="store_true", help="只更新前端 nginx")
    p.add_argument("--vpn", action="store_true", help="额外部署 93 vpn-auth")
    p.add_argument("--all", action="store_true", help="92 全部 + 93 vpn-auth")
    p.add_argument("--host", default=env("DEPLOY_SSH_HOST", "10.27.0.92"))
    p.add_argument("--host-93", default=env("DEPLOY_SSH_HOST_93", "10.27.0.93"))
    p.add_argument("--port", type=int, default=int(env("DEPLOY_SSH_PORT") or "22"))
    p.add_argument("--user", default=env("DEPLOY_SSH_USER", "root"))
    p.add_argument("--remote-dir", default=env("DEPLOY_REMOTE_DIR", "/data/genlotvpn"))
    p.add_argument("--remote-dir-93", default=env("DEPLOY_REMOTE_DIR_93", "/data/genlotvpn"))
    return p.parse_args()


def require_password() -> str:
    password = env("DEPLOY_SSH_PASSWORD")
    if not password:
        raise SystemExit(
            "缺少 DEPLOY_SSH_PASSWORD。在本机环境变量里设置，不要写进仓库或对话。"
        )
    return password


def sftp_put_tree(sftp, local: Path, remote: str) -> int:
    count = 0
    for root, _dirs, files in os.walk(local):
        rel = Path(root).relative_to(local).as_posix()
        rdir = remote if rel == "." else f"{remote.rstrip('/')}/{rel}"
        try:
            sftp.stat(rdir)
        except IOError:
            sftp.mkdir(rdir)
        for name in files:
            sftp.put(str(Path(root) / name), f"{rdir}/{name}")
            count += 1
    return count


def run_remote(ssh, command: str, timeout: int = 600) -> str:
    print(f"[deploy] 远端: {command}", flush=True)
    _stdin, stdout, stderr = ssh.exec_command(command, timeout=timeout)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="ignore").strip()
    err = stderr.read().decode("utf-8", errors="ignore").strip()
    if out:
        print(out, flush=True)
    if code != 0:
        raise RuntimeError(f"远端失败({code}): {command}\n{err or out}")
    return out


def connect(host: str, port: int, user: str, password: str):
    import paramiko

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    print(f"[deploy] 连接 {user}@{host}:{port}")
    ssh.connect(hostname=host, port=port, username=user, password=password, timeout=30)
    return ssh


def compose_up(ssh, remote: str, services: list) -> None:
    svc = " ".join(services)
    run_remote(
        ssh,
        f"cd '{remote}' && (docker compose up -d --build {svc} || docker-compose up -d --build {svc})",
    )


def dockerfile_paths(item: dict, node_dir: Path) -> tuple:
    """(本地 dockerfile, 远端相对路径)。jar 在 .../jar/xxx.jar，dockerfile 在上一级。"""
    local = item["dst"].parent / "dockerfile"
    remote = str(Path(item["remote"]).parent.parent / "dockerfile").replace("\\", "/")
    return local, remote


def upload_compose_and_dockerfiles(sftp, ssh, remote_base: str, node_dir: Path, items: list) -> None:
    remote_base = remote_base.rstrip("/")
    compose_local = node_dir / "docker-compose.yml"
    if compose_local.is_file():
        sftp.put(str(compose_local), f"{remote_base}/docker-compose.yml")
        print("[deploy] 已上传 docker-compose.yml", flush=True)
    for item in items:
        local, rel = dockerfile_paths(item, node_dir)
        if not local.is_file():
            continue
        remote = f"{remote_base}/{rel}"
        parent = remote.rsplit("/", 1)[0]
        run_remote(ssh, f"mkdir -p '{parent}'")
        sftp.put(str(local), remote)
        print(f"[deploy] 已上传 {rel}", flush=True)


def wait_started(ssh, container: str, timeout: int = 120) -> None:
    print(f"[deploy] 等待 {container} 启动", flush=True)
    deadline = time.time() + timeout
    while time.time() < deadline:
        # 网关等服务启动后 DEBUG 刷屏会把 Started 挤出 tail 40，改为看重启后一段时间
        _stdin, stdout, stderr = ssh.exec_command(
            f"docker logs --since 5m {container} 2>&1 | grep -F 'Started ' || true"
        )
        stdout.channel.recv_exit_status()
        out = stdout.read().decode("utf-8", errors="ignore")
        if "Started " in out:
            print(f"[deploy] {container} 已启动", flush=True)
            return
        time.sleep(3)
    raise RuntimeError(f"{container} 在 {timeout}s 内未出现 Started")


def inject_jars(ssh, remote_base: str, items: list) -> None:
    """旧匿名卷可能仍挡住镜像 jar：先 docker cp，再一次性重启并核对 MD5。"""
    remote_base = remote_base.rstrip("/")
    for item in items:
        jar_name = item["src"].name
        remote_jar = f"{remote_base}/{item['remote']}"
        container = item["service"]
        try:
            run_remote(ssh, f"docker cp '{remote_jar}' {container}:/home/ruoyi/{jar_name}")
        except RuntimeError:
            print(f"[deploy] docker cp 跳过 {container}（可能已是只读绑定）", flush=True)
    names = " ".join(item["service"] for item in items)
    run_remote(ssh, f"docker restart {names}")
    for item in items:
        jar_name = item["src"].name
        remote_jar = f"{remote_base}/{item['remote']}"
        container = item["service"]
        host_md5 = run_remote(ssh, f"md5sum '{remote_jar}'").split()[0]
        box_md5 = run_remote(ssh, f"docker exec {container} md5sum /home/ruoyi/{jar_name}").split()[0]
        if host_md5 != box_md5:
            raise RuntimeError(f"{container} jar MD5 不一致，匿名卷仍可能挡住新包")
        print(f"[deploy] {container} jar MD5 一致", flush=True)
    for item in items:
        wait_started(ssh, item["service"])


def stage_jars(items: list) -> None:
    for item in items:
        if not item["src"].is_file():
            raise SystemExit(f"未找到 {item['src']}，请先跑 python pipeline/bin/gate.py")
        copy_file(item["src"], item["dst"])
        check_jar_java8(item["dst"] / item["src"].name)


def upload_jars(sftp, remote_base: str, items: list) -> None:
    for item in items:
        local = item["dst"] / item["src"].name
        remote = f"{remote_base.rstrip('/')}/{item['remote']}"
        parent = remote.rsplit("/", 1)[0]
        try:
            sftp.stat(parent)
        except IOError:
            # 远端目录应已由 mkdir -p 建好
            pass
        sftp.put(str(local), remote)
        print(f"[deploy] 已上传 {item['src'].name}")


def main() -> int:
    args = parse_args()
    do_ui = True
    do_admin_jars = not args.ui
    do_vpn = args.vpn or args.all
    password = require_password()

    try:
        import paramiko  # noqa: F401
    except ImportError:
        raise SystemExit("缺少 paramiko，请执行: pip install -r bin/requirements-admin-e2e.txt")

    if do_ui:
        ui_dist = REPO / "ruoyi-ui" / "dist"
        if not (ui_dist / "index.html").is_file():
            raise SystemExit("未找到 ruoyi-ui/dist/index.html，请先跑 python pipeline/bin/gate.py")
        print("[deploy] 分发前端 dist")
        copy_html_dist(ui_dist, DOCKER / "node-92" / "nginx" / "html")

    if do_admin_jars:
        print("[deploy] 分发 92 全部 jar")
        stage_jars(ADMIN_JARS)
    if do_vpn:
        print("[deploy] 分发 93 vpn-auth jar")
        stage_jars([VPN_JAR])

    ssh92 = connect(args.host, args.port, args.user, password)
    try:
        remote = args.remote_dir.rstrip("/")
        dirs = [f"{remote}/nginx/html/dist"] + [
            f"{remote}/{item['remote'].rsplit('/', 1)[0]}" for item in ADMIN_JARS
        ]
        run_remote(ssh92, "mkdir -p " + " ".join(f"'{d}'" for d in dirs))
        sftp = ssh92.open_sftp()
        try:
            if do_ui:
                n = sftp_put_tree(
                    sftp,
                    DOCKER / "node-92" / "nginx" / "html" / "dist",
                    f"{remote}/nginx/html/dist",
                )
                print(f"[deploy] 已上传前端 {n} 个文件")
            if do_admin_jars:
                upload_jars(sftp, remote, ADMIN_JARS)
                upload_compose_and_dockerfiles(
                    sftp, ssh92, remote, DOCKER / "node-92", ADMIN_JARS
                )
        finally:
            sftp.close()

        services = ["ruoyi-nginx"] if args.ui else [j["service"] for j in ADMIN_JARS] + ["ruoyi-nginx"]
        compose_up(ssh92, remote, services)
        if do_admin_jars:
            inject_jars(ssh92, remote, ADMIN_JARS)
    finally:
        ssh92.close()

    if do_vpn:
        ssh93 = connect(args.host_93, args.port, args.user, password)
        try:
            remote93 = args.remote_dir_93.rstrip("/")
            parent = f"{remote93}/{VPN_JAR['remote'].rsplit('/', 1)[0]}"
            run_remote(ssh93, f"mkdir -p '{parent}'")
            sftp = ssh93.open_sftp()
            try:
                upload_jars(sftp, remote93, [VPN_JAR])
                upload_compose_and_dockerfiles(
                    sftp, ssh93, remote93, DOCKER / "node-93", [VPN_JAR]
                )
            finally:
                sftp.close()
            compose_up(ssh93, remote93, [VPN_JAR["service"]])
            inject_jars(ssh93, remote93, [VPN_JAR])
        finally:
            ssh93.close()

    print("[deploy] 完成")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except SystemExit:
        raise
    except Exception as exc:
        print(f"[deploy] 失败: {exc}", file=sys.stderr)
        raise SystemExit(1)
