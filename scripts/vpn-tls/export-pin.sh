#!/usr/bin/env bash
# 从 server.crt 导出 SPKI SHA-256 指纹（与 Qt CertificatePinner 算法一致）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CRT="${1:-$REPO_ROOT/docker/ruoyi/vpn/auth/certs/server.crt}"

if [[ ! -f "$CRT" ]]; then
  echo "错误: 证书不存在: $CRT" >&2
  exit 1
fi

PIN=$(openssl x509 -in "$CRT" -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 | awk '{print $2}')

echo "certPinSha256: $PIN"
echo ""
echo "粘贴到 ruoyi-vpn-client/resources/config.json 或设置页「证书指纹」字段"
