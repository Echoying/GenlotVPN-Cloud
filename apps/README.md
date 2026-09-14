# 可分发客户端产物

由打包脚本写入本目录，**安装包不进 Git**（体积大，GitHub 单文件上限 100MB）。同事从 93 下载页取包。

外网下载页（93 独立静态站，免登录）：

`https://<93公网IP>/genlotvpn/download/`

> 部署后页面从 `manifest.json` 读取 `apps/` 中各平台**当前最高版本**的安装包与 SDK，无需手改页内版本号。发版流程：确认包放入本目录 → 跑 `deploy.py vpn`（或 `download`）同步到 93。

## Windows

| 路径 | 说明 |
|------|------|
| `apps/windows/GenlotVPN-win64-{x.y.z}.zip` | **进 Git**：解压得到客户端目录 |
| `apps/windows/EnUES*_SDK.exe` | 易安联 Windows SDK（已有 `EnUES_win_v3.3.7.0132_SDK.exe`） |

当前本次：`GenlotVPN-win64-1.0.2`

### 同事使用

1. 打开上方下载页（或从管理端「客户端版本策略」链接进入）  
2. 先安装 **易安联 Windows SDK**，再下载 **GenlotVPN** zip 并解压  
3. 运行客户端 → 选线 → 登录 → 按需完成钉钉验证  

## macOS

| 路径 | 说明 |
|------|------|
| `apps/macos/GenlotVPN-macos-{arch}-{version}.zip` | **进 Git**：解压得到 `GenlotVPN.app` |
| `apps/macos/GenlotVPN-macos-{arch}-{version}/GenlotVPN.app` | 本机副本（已 gitignore，不提交） |
| `apps/macos/EnUES*_SDK.pkg` | 易安联 macOS SDK（已有 `EnUESBOX_mac_v3.3.7.0114_SDK.pkg`） |

当前本次：`GenlotVPN-macos-x86_64-1.0.1`

### 同事使用

1. 从下载页或仓库取出对应 zip 并解压  
2. 双击 `GenlotVPN.app`（若 Gatekeeper 拦截：右键 → 打开）  
3. 本机需已安装并启动 **易安联 macOS Agent**（`127.0.0.1:30303`）

### 维护者重新打包后提交示例

```bash
bash bin/build-vpn-client-macos.sh --package
git add apps/README.md apps/macos/*.zip
git commit -m "release: macOS 客户端 1.0.1 (x86_64)"
git push
```

> `ruoyi-vpn-client/dist/`、`build-macos/`、`apps/macos/*/`（未压缩 .app）已 gitignore。  
> zip 约数十～百余 MB；若远端单文件限制更严，请改用 Git LFS 或网盘。
