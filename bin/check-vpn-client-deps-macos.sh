#!/usr/bin/env bash
# GenlotVPN macOS 依赖检查
set -euo pipefail

echo "========================================"
echo "  GenlotVPN Client - macOS Deps Check"
echo "========================================"
echo

MISSING=0

if command -v cmake >/dev/null 2>&1; then
  CMAKE_VER="$(cmake --version | head -n1)"
  echo "[OK] CMake: $CMAKE_VER"
  # 粗检：要求 3.21+（脚本构建依赖）
  if echo "$CMAKE_VER" | grep -Eq 'version 2\.|version 3\.(0|[1-9]|1[0-9]|20)\.'; then
    echo "[X] CMake 版本过低（需要 ≥ 3.21）。Monterey 推荐安装官方 cmake-*-macos-universal.dmg"
    MISSING=$((MISSING + 1))
  fi
else
  echo "[X] CMake 未找到 — 官方 dmg 或 brew install cmake（详见 docs/MACOS_SETUP.md）"
  MISSING=$((MISSING + 1))
fi

if command -v ninja >/dev/null 2>&1; then
  echo "[OK] Ninja: $(ninja --version)"
else
  echo "[X] ninja 未找到 — brew install ninja"
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
    "$HOME/Qt/6.7.3/macos" \
    "$HOME/Qt/6.6.3/macos" \
    "$HOME/Qt/6.8.3/macos" \
    "$HOME/Qt/6.11.1/macos" \
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
  case "$CMAKE_PREFIX_PATH" in
    */6.11.*|*/6.10.*|*/6.9.*|*/6.8.*)
      if [[ "$(sw_vers -productVersion 2>/dev/null | cut -d. -f1)" -lt 13 ]]; then
        echo "[!] 警告：当前 macOS < 13，Qt 6.8+ 可能无法运行 qmlimportscanner，建议改用 Qt 6.6/6.7"
      fi
      ;;
  esac
  if [[ -x "${CMAKE_PREFIX_PATH}/bin/macdeployqt" ]]; then
    echo "[OK] macdeployqt"
  else
    echo "[X] macdeployqt 缺失: ${CMAKE_PREFIX_PATH}/bin/macdeployqt"
    MISSING=$((MISSING + 1))
  fi
else
  echo "[X] 未找到 Qt6 — 安装 Qt 6.6/6.7 macOS，并 export CMAKE_PREFIX_PATH=~/Qt/6.7.x/macos"
  MISSING=$((MISSING + 1))
fi

echo "[..] 架构: uname=$(uname -m)  CMAKE_OSX_ARCHITECTURES=${CMAKE_OSX_ARCHITECTURES:-"(将默认用 uname -m)"}"

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
