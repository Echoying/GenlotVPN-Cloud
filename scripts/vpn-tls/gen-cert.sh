#!/usr/bin/env bash
# 生成 VPN TCP 自签 TLS 证书（RSA 2048，默认 SAN IP:10.9.2.177）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_DIR="${1:-$REPO_ROOT/docker/ruoyi/vpn/auth/certs}"
SAN_IP="${2:-10.9.2.177}"
DAYS=3650

mkdir -p "$OUT_DIR"

CRT="$OUT_DIR/server.crt"
KEY="$OUT_DIR/server.key"

if [[ -f "$KEY" || -f "$CRT" ]]; then
  echo "错误: $OUT_DIR 已存在 server.crt 或 server.key，请先备份后删除" >&2
  exit 1
fi

ALT_CONF="$(mktemp)"
cat > "$ALT_CONF" <<EOF
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no

[req_distinguished_name]

[v3_req]
subjectAltName = IP:${SAN_IP}
EOF

openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout "$KEY" -out "$CRT" -days "$DAYS" \
  -subj "/CN=GenlotVPN/O=Genlot/C=CN" \
  -config "$ALT_CONF"
rm -f "$ALT_CONF"

chmod 600 "$KEY"
echo "证书已生成:"
echo "  $CRT"
echo "  $KEY"
echo ""
echo "导出 Pin 指纹:"
"$SCRIPT_DIR/export-pin.sh" "$CRT"
