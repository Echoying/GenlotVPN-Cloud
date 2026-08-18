#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""读取本机凭证，写入当前进程环境。已存在的非空环境变量不覆盖。

优先 `pipeline/deploy/envs.yaml`，其次 `.deploy/envs.yaml`，再回退仓库根目录 `.env` / `.env.local`。
"""
from __future__ import annotations

import os
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from paths import pipeline_dir, repo_root

REPO = repo_root()
YAML_CANDIDATES = (
    pipeline_dir() / "deploy" / "envs.yaml",
    REPO / ".deploy" / "envs.yaml",
)
DOTENV_FILES = (REPO / ".env", REPO / ".env.local")


def _yaml_file() -> Path:
    for path in YAML_CANDIDATES:
        if path.is_file():
            return path
    return YAML_CANDIDATES[0]


YAML_FILE = _yaml_file()

SECTION_KEY_MAP = {
    "deploy": {
        "ssh_host": "DEPLOY_SSH_HOST",
        "ssh_host_91": "DEPLOY_SSH_HOST_91",
        "ssh_host_93": "DEPLOY_SSH_HOST_93",
        "ssh_user": "DEPLOY_SSH_USER",
        "ssh_port": "DEPLOY_SSH_PORT",
        "ssh_password": "DEPLOY_SSH_PASSWORD",
        "remote_dir": "DEPLOY_REMOTE_DIR",
        "remote_dir_93": "DEPLOY_REMOTE_DIR_93",
    },
    "accept": {
        "base_url": "ADMIN_E2E_BASE_URL",
        "user": "ADMIN_E2E_USER",
        "password": "ADMIN_E2E_PASSWORD",
        "client_user": "CLIENT_E2E_USER",
        "client_password": "CLIENT_E2E_PASSWORD",
        "path": "ADMIN_E2E_PATH",
        "expect_text": "ADMIN_E2E_EXPECT_TEXT",
        "title": "ADMIN_E2E_TITLE",
        "module": "ADMIN_E2E_MODULE",
        "round": "ADMIN_E2E_ROUND",
        "run": "ADMIN_E2E_RUN",
    },
    "dingtalk": {
        "access_token": "DINGTALK_ACCESS_TOKEN",
        "secret": "DINGTALK_SECRET",
        "webhook": "DINGTALK_WEBHOOK",
        "keywords": "DINGTALK_KEYWORDS",
    },
}


def _unquote(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        return value[1:-1]
    return value


def _parse_dotenv_line(raw: str) -> Optional[Tuple[str, str]]:
    line = raw.strip()
    if not line or line.startswith("#") or "=" not in line:
        return None
    key, _, value = line.partition("=")
    key = key.strip()
    if not key:
        return None
    return key, _unquote(value)


def _strip_comment(raw: str) -> str:
    in_quote = None
    for i, ch in enumerate(raw):
        if ch in "\"'" and (i == 0 or raw[i - 1] != "\\"):
            if in_quote is None:
                in_quote = ch
            elif in_quote == ch:
                in_quote = None
        elif ch == "#" and in_quote is None:
            return raw[:i].rstrip()
    return raw.rstrip()


def parse_envs_yaml(text: str) -> Dict[str, str]:
    """解析两层 YAML：section.key 或 env.KEY，以及顶层 ENV_VAR。不引入 PyYAML。"""
    result = {}
    section = None
    for raw in text.splitlines():
        line = _strip_comment(raw)
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip(" "))
        body = line.strip()
        if indent == 0 and body.endswith(":") and ":" == body[-1] and body.count(":") == 1:
            section = body[:-1].strip()
            continue
        if ":" not in body:
            continue
        key, _, value = body.partition(":")
        key = key.strip()
        value = _unquote(value)
        if indent == 0:
            section = None
            if key.isupper() or key.startswith("DEPLOY_") or key.startswith("ADMIN_") or key.startswith("DINGTALK_"):
                result[key] = value
            continue
        if section == "env":
            result[key] = value
            continue
        mapped = SECTION_KEY_MAP.get(section, {}).get(key)
        if mapped:
            result[mapped] = value
        elif key.isupper():
            result[key] = value
    return result


def iter_env_pairs() -> List[Tuple[str, str]]:
    pairs = []
    if YAML_FILE.is_file():
        pairs.extend(parse_envs_yaml(YAML_FILE.read_text(encoding="utf-8")).items())
    for path in DOTENV_FILES:
        if not path.is_file():
            continue
        for raw in path.read_text(encoding="utf-8").splitlines():
            parsed = _parse_dotenv_line(raw)
            if parsed:
                pairs.append(parsed)
    return pairs


def _normalize_dingtalk_env() -> None:
    """access_token 若误填完整 webhook URL，拆出真正的 token，并补 DINGTALK_WEBHOOK。"""
    token = os.environ.get("DINGTALK_ACCESS_TOKEN", "").strip()
    webhook = os.environ.get("DINGTALK_WEBHOOK", "").strip()
    if not token.startswith("http://") and not token.startswith("https://"):
        return
    if not webhook:
        os.environ["DINGTALK_WEBHOOK"] = token
    try:
        from urllib.parse import parse_qs, urlparse

        real = (parse_qs(urlparse(token).query).get("access_token") or [""])[0].strip()
        if real:
            os.environ["DINGTALK_ACCESS_TOKEN"] = real
    except Exception:
        pass


def load(overwrite: bool = False) -> Optional[Path]:
    loaded = None
    for key, value in iter_env_pairs():
        if overwrite or key not in os.environ or not os.environ.get(key, "").strip():
            os.environ[key] = str(value)
        loaded = YAML_FILE if YAML_FILE.is_file() else DOTENV_FILES[0]
    _normalize_dingtalk_env()
    if YAML_FILE.is_file():
        return YAML_FILE
    for path in DOTENV_FILES:
        if path.is_file():
            return path
    return loaded


def export_cmd() -> None:
    load()
    seen = set()
    for key, _ in iter_env_pairs():
        if key in seen:
            continue
        seen.add(key)
        value = os.environ.get(key, "").replace('"', '""')
        sys.stdout.write('set "{}={}"\n'.format(key, value))


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--export-cmd":
        export_cmd()
    else:
        path = load()
        print("loaded" if path else "missing", path or YAML_FILE)
