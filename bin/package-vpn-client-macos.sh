#!/usr/bin/env bash
# GenlotVPN macOS 打包：macdeployqt + 拷贝到 dist/
# 签名/公证需设置 APPLE_DEVELOPER_ID / NOTARY_* 环境变量后另行执行（见 DEPLOY_MACOS.md）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLIENT="$ROOT/ruoyi-vpn-client"
cd "$CLIENT"

if [[ -z "${CMAKE_PREFIX_PATH:-}" ]]; then
  for hint in \
    "$HOME/Qt/6.11.1/macos" \
    "$HOME/Qt/6.8.3/macos" \
    "/opt/homebrew/opt/qt" \
    "/usr/local/opt/qt"; do
    if [[ -f "$hint/lib/cmake/Qt6/Qt6Config.cmake" ]]; then
      export CMAKE_PREFIX_PATH="$hint"
      break
    fi
  done
fi

MACDEPLOYQT="${CMAKE_PREFIX_PATH:-}/bin/macdeployqt"
if [[ ! -x "$MACDEPLOYQT" ]]; then
  echo "[X] macdeployqt 未找到，请设置 CMAKE_PREFIX_PATH"
  exit 1
fi

APP_PATH="${GENLOT_APP_PATH:-}"
if [[ -z "$APP_PATH" || ! -d "$APP_PATH" ]]; then
  for candidate in \
    build-macos/GenlotVPN.app \
    build/GenlotVPN.app; do
    if [[ -d "$candidate" ]]; then
      APP_PATH="$candidate"
      break
    fi
  done
fi
if [[ -z "$APP_PATH" || ! -d "$APP_PATH" ]]; then
  APP_PATH="$(find build-macos build -maxdepth 3 -type d -name 'GenlotVPN.app' 2>/dev/null | head -n1 || true)"
fi
if [[ -z "$APP_PATH" || ! -d "$APP_PATH" ]]; then
  echo "[X] 未找到 GenlotVPN.app，请先: bin/build-vpn-client-macos.sh"
  exit 1
fi

VER="$(sed -n 's/.*project(GenlotVPN VERSION \([0-9.]*\).*/\1/p' CMakeLists.txt | head -n1)"
if [[ -z "$VER" ]]; then
  echo "[X] 无法从 CMakeLists.txt 读取 VERSION"
  exit 1
fi

ARCH="$(uname -m)"
DIST="dist/GenlotVPN-macos-${ARCH}-${VER}"
echo "[OK] 源: $APP_PATH"
echo "[OK] 版本: $VER 架构: $ARCH"

rm -rf "$DIST"
mkdir -p "$DIST"
rm -rf "$DIST/GenlotVPN.app"
cp -R "$APP_PATH" "$DIST/GenlotVPN.app"

echo "[..] macdeployqt ..."
"$MACDEPLOYQT" "$DIST/GenlotVPN.app" -qmldir="$CLIENT/qml" -verbose=1

# Homebrew libprotobuf 若为动态库，尝试拷入 Frameworks
copy_protobuf_dylib() {
  local bin="$DIST/GenlotVPN.app/Contents/MacOS/GenlotVPN"
  [[ -x "$bin" ]] || return 0
  local deps
  deps="$(otool -L "$bin" | awk '/libprotobuf/{print $1}' || true)"
  [[ -n "$deps" ]] || return 0
  mkdir -p "$DIST/GenlotVPN.app/Contents/Frameworks"
  while IFS= read -r lib; do
    [[ -f "$lib" ]] || continue
    local base
    base="$(basename "$lib")"
    cp -f "$lib" "$DIST/GenlotVPN.app/Contents/Frameworks/$base"
    install_name_tool -change "$lib" "@executable_path/../Frameworks/$base" "$bin" || true
    echo "[OK] 已打包 $base"
  done <<< "$deps"
}
copy_protobuf_dylib

# 确保默认配置在 Resources
mkdir -p "$DIST/GenlotVPN.app/Contents/Resources"
cp -f resources/config.default.json "$DIST/GenlotVPN.app/Contents/Resources/config.default.json"

echo
echo "[OK] 可分发目录: ruoyi-vpn-client/$DIST"
echo "    应用: $DIST/GenlotVPN.app"
echo "    签名/公证/dmg 见 docs/DEPLOY_MACOS.md"
echo "    可写配置/日志: ~/Library/Application Support/Genlot/GenlotVPN/"
