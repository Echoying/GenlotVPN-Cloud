#!/usr/bin/env bash
# GenlotVPN macOS Release 构建（产出 GenlotVPN.app）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PACKAGE=0
if [[ "${1:-}" == "--package" || "${1:-}" == "-p" ]]; then
  PACKAGE=1
fi

bash bin/check-vpn-client-deps-macos.sh

# 重新探测（check 脚本可能已设置）
if [[ -z "${CMAKE_PREFIX_PATH:-}" ]]; then
  for hint in \
    "$HOME/Qt/6.7.3/macos" \
    "$HOME/Qt/6.6.3/macos" \
    "$HOME/Qt/6.8.3/macos" \
    "$HOME/Qt/6.11.1/macos" \
    "/opt/homebrew/opt/qt" \
    "/usr/local/opt/qt"; do
    if [[ -f "$hint/lib/cmake/Qt6/Qt6Config.cmake" ]]; then
      export CMAKE_PREFIX_PATH="$hint"
      break
    fi
  done
fi

cd ruoyi-vpn-client
BUILD_DIR="${BUILD_DIR:-build-macos}"

if [[ -n "${CMAKE_PREFIX_PATH:-}" && -x "${CMAKE_PREFIX_PATH}/bin/lupdate" ]]; then
  echo "[..] lupdate ..."
  "${CMAKE_PREFIX_PATH}/bin/lupdate" src qml -ts i18n/genlotvpn_zh_CN.ts i18n/genlotvpn_en.ts >/dev/null 2>&1 || true
fi

# 默认跟随本机架构；Intel Monterey 勿再误用 arm64
ARCH="${CMAKE_OSX_ARCHITECTURES:-$(uname -m)}"
echo "[..] CMAKE_PREFIX_PATH=${CMAKE_PREFIX_PATH:-}"
echo "[..] CMAKE_OSX_ARCHITECTURES=${ARCH}"

CMAKE_ARGS=(
  -B "$BUILD_DIR"
  -G Ninja
  -DCMAKE_BUILD_TYPE=Release
  -DCMAKE_PREFIX_PATH="${CMAKE_PREFIX_PATH}"
  -DCMAKE_OSX_ARCHITECTURES="${ARCH}"
)
if [[ -n "${Protobuf_ROOT:-}" ]]; then
  CMAKE_ARGS+=(-DProtobuf_ROOT="${Protobuf_ROOT}")
fi

echo "[..] cmake configure -> $BUILD_DIR"
cmake "${CMAKE_ARGS[@]}"

echo "[..] cmake build Release"
cmake --build "$BUILD_DIR" --config Release

APP_PATH="$BUILD_DIR/GenlotVPN.app"
if [[ ! -d "$APP_PATH" ]]; then
  # 部分生成器把 .app 放在子目录
  APP_PATH="$(find "$BUILD_DIR" -maxdepth 3 -type d -name 'GenlotVPN.app' | head -n1 || true)"
fi
if [[ -z "$APP_PATH" || ! -d "$APP_PATH" ]]; then
  echo "[X] 未找到 GenlotVPN.app"
  exit 1
fi

echo "[OK] 构建完成: $APP_PATH"

if [[ "$PACKAGE" -eq 1 ]]; then
  cd "$ROOT"
  GENLOT_APP_PATH="$APP_PATH" bash bin/package-vpn-client-macos.sh
fi
