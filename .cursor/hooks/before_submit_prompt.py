#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""beforeSubmitPrompt：对话里像密码/密钥则拦截，不把原文回显。"""
from __future__ import annotations

import json
import re
import sys

PLACEHOLDERS = {
    "",
    "*",
    "**",
    "***",
    "xxx",
    "xxxx",
    "todo",
    "none",
    "null",
    "true",
    "false",
    "changeme",
    "your_password",
    "password",
    "secret",
}

ASSIGN_RE = re.compile(
    r"(?ix)"
    r"(?:password|passwd|pwd|secret|token|api[_-]?key|access[_-]?token|"
    r"session[_-]?key|ssh_password|DEPLOY_SSH_PASSWORD|ADMIN_E2E_PASSWORD|"
    r"DINGTALK_ACCESS_TOKEN|DINGTALK_SECRET|MYSQL_ROOT_PASSWORD)"
    r"\s*[:=]\s*"
    r"(?:['\"]([^'\"]+)['\"]|(\S+))"
)

CN_SECRET_RE = re.compile(r"(?:密码|口令|密钥)\s*[是为：:=]\s*(\S{4,})")
PEM_RE = re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")
DING_URL_RE = re.compile(r"oapi\.dingtalk\.com/robot/send\?access_token=", re.I)
JWT_RE = re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b")

SENSITIVE_ATTACH = re.compile(
    r"(?:^|[\\/])(?:\.env(?:\..+)?|envs\.yaml|secrets\.env|id_rsa|.*\.pem|.*\.key)$",
    re.I,
)

USER_MSG = (
    "检测到对话或附件里可能含密码/密钥/token。"
    "请改成 *** 或只写变量名后重发。凭证放 pipeline/deploy/envs.yaml 或 .deploy/envs.yaml，不要贴进对话。"
)


def _value_looks_secret(value: str) -> bool:
    v = (value or "").strip().strip("'\"")
    if v.lower() in PLACEHOLDERS:
        return False
    if v.endswith(".yaml") or v.endswith(".yml") or v.endswith(".env"):
        return False
    return len(v) >= 4


def find_reason(prompt: str, attachments: list) -> str:
    text = prompt or ""
    if PEM_RE.search(text):
        return "private-key"
    if DING_URL_RE.search(text):
        return "dingtalk-webhook"
    if JWT_RE.search(text):
        return "jwt"
    for m in ASSIGN_RE.finditer(text):
        if _value_looks_secret(m.group(1) or m.group(2) or ""):
            return "assignment"
    for m in CN_SECRET_RE.finditer(text):
        if _value_looks_secret(m.group(1)):
            return "cn-secret"
    for att in attachments or []:
        path = str(att.get("file_path") or "")
        if SENSITIVE_ATTACH.search(path.replace("\\", "/")):
            return "attachment"
    return ""


def decide(payload: dict) -> dict:
    reason = find_reason(payload.get("prompt") or "", payload.get("attachments") or [])
    if reason:
        return {"continue": False, "user_message": USER_MSG}
    return {"continue": True}


def _self_test() -> int:
    cases = [
        ({"prompt": "密码只走环境变量，不要写进对话"}, True),
        ({"prompt": "DEPLOY_SSH_PASSWORD="}, True),
        ({"prompt": "ssh_password: \"\""}, True),
        ({"prompt": "ssh_password: \"abcd1234\""}, False),
        ({"prompt": "ADMIN_E2E_PASSWORD=123456"}, False),
        ({"prompt": "密码是 abcd"}, False),
        ({"prompt": "-----BEGIN RSA PRIVATE KEY-----\nMII"}, False),
        (
            {
                "prompt": "看下配置",
                "attachments": [{"type": "file", "file_path": r"D:\proj\.deploy\envs.yaml"}],
            },
            False,
        ),
        ({"prompt": "打开 login.vue 看验证码"}, True),
    ]
    failed = 0
    for payload, allow in cases:
        got = decide(payload)["continue"]
        if got != allow:
            failed += 1
            sys.stderr.write("FAIL {} expected continue={} got={}\n".format(payload, allow, got))
    if failed:
        sys.stderr.write("self-test failed: {}\n".format(failed))
        return 1
    sys.stderr.write("self-test ok\n")
    return 0


def main() -> int:
    if len(sys.argv) > 1 and sys.argv[1] == "--self-test":
        return _self_test()
    raw = sys.stdin.read()
    if not raw.strip():
        sys.stdout.write(json.dumps({"continue": True}))
        return 0
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        sys.stdout.write(json.dumps({"continue": True}))
        return 0
    sys.stdout.write(json.dumps(decide(payload), ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
