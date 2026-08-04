#!/usr/bin/env bash
# GenlotVPN macOS 打包：macdeployqt + protobuf + 自检 → dist/
# 签名/公证见 docs/DEPLOY_MACOS.md
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLIENT="$ROOT/ruoyi-vpn-client"
cd "$CLIENT"

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

MACDEPLOYQT="${CMAKE_PREFIX_PATH:-}/bin/macdeployqt"
if [[ ! -x "$MACDEPLOYQT" ]]; then
  echo "[X] macdeployqt 未找到，请设置 CMAKE_PREFIX_PATH（建议 Qt 6.6/6.7）"
  exit 1
fi
echo "[OK] macdeployqt: $MACDEPLOYQT"

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
cp -R "$APP_PATH" "$DIST/GenlotVPN.app"
APP="$DIST/GenlotVPN.app"
MACOS_DIR="$APP/Contents/MacOS"
FW_DIR="$APP/Contents/Frameworks"

# VERSION 属性会产出 GenlotVPN-x.y.z + 符号链接 GenlotVPN；打包后确保符号链接存在
resolve_bin() {
  local link="$MACOS_DIR/GenlotVPN"
  local verbin
  verbin="$(find "$MACOS_DIR" -maxdepth 1 -type f -name 'GenlotVPN-*' | head -n1 || true)"
  if [[ -n "$verbin" ]]; then
    local base
    base="$(basename "$verbin")"
    ln -sfn "$base" "$link"
    echo "$verbin"
    return 0
  fi
  if [[ -x "$link" ]]; then
    echo "$link"
    return 0
  fi
  echo ""
}

BIN="$(resolve_bin)"
if [[ -z "$BIN" || ! -x "$BIN" ]]; then
  echo "[X] 未找到可执行文件 Contents/MacOS/GenlotVPN*"
  exit 1
fi
echo "[OK] 可执行文件: $BIN"

echo "[..] macdeployqt（嵌入 Qt 框架 / 插件 / QML）..."
# -qmldir：按项目 QML 导入收集 QtQuick 等模块
# SQL 驱动本客户端不用；macdeployqt 仍可能尝试拷贝并打印 ERROR，稍后删除
set +e
"$MACDEPLOYQT" "$APP" -qmldir="$CLIENT/qml" -verbose=1
MDQ_RC=$?
set -e
if [[ "$MDQ_RC" -ne 0 ]]; then
  echo "[!] macdeployqt 退出码=$MDQ_RC（若仅为 sql 插件缺库警告，将继续并清理）"
fi

# 删除无用且常带绝对路径的 SQL 驱动（本应用不使用 QtSql）
if [[ -d "$APP/Contents/PlugIns/sqldrivers" ]]; then
  rm -rf "$APP/Contents/PlugIns/sqldrivers"
  echo "[OK] 已移除 PlugIns/sqldrivers（应用不依赖）"
fi

# Homebrew libprotobuf：拷入 Frameworks 并改写 install name
copy_protobuf_dylib() {
  mkdir -p "$FW_DIR"
  local deps
  deps="$(otool -L "$BIN" | awk '/libprotobuf/{print $1}' || true)"
  if [[ -z "$deps" ]]; then
    echo "[..] 可执行文件未链接动态 libprotobuf（可能已静态链接），跳过"
    return 0
  fi
  while IFS= read -r lib; do
    [[ -z "$lib" ]] && continue
    # 已是 @executable_path 则确认文件在 Frameworks
    local base
    base="$(basename "$lib")"
    if [[ "$lib" == @* ]]; then
      if [[ -f "$FW_DIR/$base" ]]; then
        echo "[OK] 已存在 $base"
      else
        echo "[X] 引用 $lib 但 Frameworks 中无 $base"
        return 1
      fi
      continue
    fi
    if [[ ! -f "$lib" ]]; then
      echo "[X] 找不到 protobuf 库: $lib"
      return 1
    fi
    cp -f "$lib" "$FW_DIR/$base"
    chmod u+w "$FW_DIR/$base"
    install_name_tool -id "@executable_path/../Frameworks/$base" "$FW_DIR/$base"
    install_name_tool -change "$lib" "@executable_path/../Frameworks/$base" "$BIN"
    # 若符号链接 GenlotVPN 指向该文件，install_name_tool 已改实体；无需再改
    echo "[OK] 已打包 $base ← $lib"
  done <<< "$deps"
}
copy_protobuf_dylib

# 默认配置与图标、翻译（构建期一般已有，再确保一遍）
mkdir -p "$APP/Contents/Resources/i18n"
cp -f resources/config.default.json "$APP/Contents/Resources/config.default.json"
if [[ -f assets/images/genlot-app.icns ]]; then
  cp -f assets/images/genlot-app.icns "$APP/Contents/Resources/genlot-app.icns"
fi
if [[ -d build-macos ]]; then
  for qm in build-macos/genlotvpn_zh_CN.qm build-macos/genlotvpn_en.qm; do
    [[ -f "$qm" ]] && cp -f "$qm" "$APP/Contents/Resources/i18n/"
  done
fi

# ---------- 自检 ----------
echo
echo "[..] 打包自检..."
FAIL=0

# 1) CFBundleExecutable 对应文件
EXE_NAME="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$APP/Contents/Info.plist" 2>/dev/null || true)"
if [[ -z "$EXE_NAME" || ! -e "$MACOS_DIR/$EXE_NAME" ]]; then
  echo "[X] Info.plist CFBundleExecutable=$EXE_NAME 在 MacOS 下不存在"
  FAIL=1
else
  echo "[OK] CFBundleExecutable=$EXE_NAME"
fi

# 2) 关键 Qt 框架
for fw in QtCore QtGui QtWidgets QtNetwork QtQml QtQuick QtQuickControls2; do
  if [[ ! -d "$FW_DIR/$fw.framework" ]]; then
    echo "[X] 缺少 Frameworks/$fw.framework"
    FAIL=1
  fi
done
echo "[OK] 核心 Qt 框架已嵌入"

# 3) 平台 / TLS 插件（云端 TLS 需要）
if [[ ! -f "$APP/Contents/PlugIns/platforms/libqcocoa.dylib" ]]; then
  echo "[X] 缺少 platforms/libqcocoa.dylib"
  FAIL=1
else
  echo "[OK] platforms/libqcocoa.dylib"
fi
if [[ ! -d "$APP/Contents/PlugIns/tls" ]]; then
  echo "[X] 缺少 PlugIns/tls（Qt TLS 后端）"
  FAIL=1
else
  echo "[OK] PlugIns/tls: $(ls "$APP/Contents/PlugIns/tls" | tr '\n' ' ')"
fi

# 4) QML 导入（Qt 官方模块 + 业务 GenlotVPN）
if [[ ! -d "$APP/Contents/Resources/qml/QtQuick" ]]; then
  echo "[X] 缺少 Resources/qml/QtQuick（macdeployqt -qmldir 未生效？）"
  FAIL=1
else
  echo "[OK] Resources/qml/QtQuick"
fi
if [[ ! -f "$APP/Contents/Resources/GenlotVPN/qmldir" ]]; then
  echo "[X] 缺少 Resources/GenlotVPN/qmldir（业务 QML 模块）"
  FAIL=1
else
  echo "[OK] Resources/GenlotVPN/qmldir"
fi

# 5) 图标与配置
if [[ ! -f "$APP/Contents/Resources/genlot-app.icns" ]]; then
  echo "[!] 警告: 缺少 genlot-app.icns"
else
  echo "[OK] genlot-app.icns"
fi
if [[ ! -f "$APP/Contents/Resources/config.default.json" ]]; then
  echo "[X] 缺少 config.default.json"
  FAIL=1
else
  echo "[OK] config.default.json"
fi

# 6) 不得残留指向本机构建机绝对路径的依赖（干净机器会打不开）
ABS_HITS="$(
  find "$APP" \( -name '*.dylib' -o -name 'GenlotVPN-*' -o -path '*/Versions/A/Qt*' \) -type f 2>/dev/null \
    | while read -r f; do
        otool -L "$f" 2>/dev/null | awk 'NR>1{print $1}' \
          | grep -E '^(/usr/local|/opt/homebrew|/Users/)' \
          | while read -r dep; do echo "$f => $dep"; done || true
      done
)"
if [[ -n "$ABS_HITS" ]]; then
  echo "[X] 仍有指向本机绝对路径的动态库依赖（目标机将无法加载）："
  echo "$ABS_HITS"
  FAIL=1
else
  echo "[OK] 无 /usr/local|/opt/homebrew|/Users 绝对路径依赖"
fi

# 7) 体积摘要
echo
echo "[..] 体积: $(du -sh "$APP" | awk '{print $1}')  (Frameworks $(du -sh "$FW_DIR" | awk '{print $1}'), PlugIns $(du -sh "$APP/Contents/PlugIns" | awk '{print $1}'))"

if [[ "$FAIL" -ne 0 ]]; then
  echo
  echo "[X] 打包自检失败，请勿分发"
  exit 1
fi

# ---------- 发布到仓库 apps/（便于 git 提交分发）----------
# 进 Git 只提交 zip（单文件）；另拷一份未压缩 .app 方便本机打开（gitignore）
APPS_DIR="$ROOT/apps/macos"
BUNDLE_STEM="GenlotVPN-macos-${ARCH}-${VER}"
ZIP_PATH="${APPS_DIR}/${BUNDLE_STEM}.zip"
mkdir -p "$APPS_DIR"
rm -f "$ZIP_PATH"
# --keepParent：解压后得到 GenlotVPN.app 目录
ditto -c -k --keepParent "$APP" "$ZIP_PATH"
# 本机便捷副本（apps/macos/*/ 已 gitignore，不会进仓库）
APP_COPY_DIR="${APPS_DIR}/${BUNDLE_STEM}"
rm -rf "$APP_COPY_DIR"
mkdir -p "$APP_COPY_DIR"
cp -R "$APP" "$APP_COPY_DIR/GenlotVPN.app"

# 写/更新目录说明
cat > "$ROOT/apps/README.md" <<EOF
# 可分发客户端产物

由打包脚本自动写入，**请将 zip 随版本提交到 Git**，供同事下载使用。

## macOS

| 路径 | 说明 |
|------|------|
| \`apps/macos/GenlotVPN-macos-{arch}-{version}.zip\` | **进 Git**：解压得到 \`GenlotVPN.app\` |
| \`apps/macos/GenlotVPN-macos-{arch}-{version}/GenlotVPN.app\` | 本机副本（已 gitignore，不提交） |

当前本次：\`${BUNDLE_STEM}\`

### 同事使用

1. 从仓库取出对应 zip 并解压  
2. 双击 \`GenlotVPN.app\`（若 Gatekeeper 拦截：右键 → 打开）  
3. 本机需已安装并启动 **易安联 macOS Agent**（\`127.0.0.1:30303\`）

### 维护者重新打包后提交示例

\`\`\`bash
bash bin/build-vpn-client-macos.sh --package
git add apps/README.md apps/macos/*.zip
git commit -m "release: macOS 客户端 ${VER} (${ARCH})"
git push
\`\`\`

> \`ruoyi-vpn-client/dist/\`、\`build-macos/\`、\`apps/macos/*/\`（未压缩 .app）已 gitignore。  
> zip 约数十～百余 MB；若远端单文件限制更严，请改用 Git LFS 或网盘。
EOF

echo
echo "[OK] 可分发目录: ruoyi-vpn-client/$DIST"
echo "    应用: $DIST/GenlotVPN.app"
echo "[OK] 已同步到仓库 apps/:"
echo "    $ZIP_PATH  ($(du -sh "$ZIP_PATH" | awk '{print $1}'))  ← 请 git add 此 zip"
echo "    $APP_COPY_DIR/GenlotVPN.app  （本机用，不进 Git）"
echo "    内含: Qt 框架 + platforms/tls/qml 插件 + libprotobuf + 业务 QML/i18n/图标"
echo "    签名/公证/dmg 见 docs/DEPLOY_MACOS.md"
echo "    提交示例: git add apps/README.md apps/macos/*.zip && git commit -m \"release: macOS 客户端 ${VER} (${ARCH})\""
