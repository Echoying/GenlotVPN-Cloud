#!/usr/bin/env bash
# GenlotVPN macOS 依赖检查
set -euo pipefail

echo "========================================"
echo "  GenlotVPN Client - macOS Deps Check"
echo "========================================"
echo

MISSING=0

if command -v cmake >/dev/null 2>&1; then
  echo "[OK] CMake: $(cmake --version | head -n1)"
else
  echo "[X] CMake 未找到 — brew install cmake"
  MISSING=$((MISSING + 1))
fi

if command -v protoc >/dev/null 2>&1; then
  echo "[OK] Protobuf: $(protoc --version)"
else
  echo "[X] protoc 未找到 — brew install protobuf"
  MISSING=$((MISSING + 1))
fi

if [[ -z "${CMAKE_PREFIX_PATH:-}" ]]; then
  for hint in \
    "$HOME/Qt/6.11.1/macos" \
    "$HOME/Qt/6.8.3/macos" \
    "/opt/homebrew/opt/qt" \
    "/usr/local/opt/qt"; do
    if [[ -f "$hint/lib/cmake/Qt6/Qt6Config.cmake" ]]; then
      export CMAKE_PREFIX_PATH="$hint"
      echo "[..] 自动探测 CMAKE_PREFIX_PATH=$CMAKE_PREFIX_PATH"
      break
    fi
  done
fi

if [[ -n "${CMAKE_PREFIX_PATH:-}" && -f "${CMAKE_PREFIX_PATH}/lib/cmake/Qt6/Qt6Config.cmake" ]]; then
  echo "[OK] Qt6 at $CMAKE_PREFIX_PATH"
  if [[ -x "${CMAKE_PREFIX_PATH}/bin/macdeployqt" ]]; then
    echo "[OK] macdeployqt"
  else
    echo "[X] macdeployqt 缺失: ${CMAKE_PREFIX_PATH}/bin/macdeployqt"
    MISSING=$((MISSING + 1))
  fi
else
  echo "[X] 未找到 Qt6 — 安装 Qt 6 macOS，并 export CMAKE_PREFIX_PATH=~/Qt/6.x.x/macos"
  MISSING=$((MISSING + 1))
fi

if xcode-select -p >/dev/null 2>&1; then
  echo "[OK] Xcode CLT: $(xcode-select -p)"
else
  echo "[X] 未找到 Xcode Command Line Tools — xcode-select --install"
  MISSING=$((MISSING + 1))
fi

echo
if [[ "$MISSING" -gt 0 ]]; then
  echo "缺少 $MISSING 项依赖，详见 ruoyi-vpn-client/docs/MACOS_SETUP.md"
  exit 1
fi
echo "依赖检查通过。"
exit 0
