# 可分发客户端产物

由打包脚本写入本目录，**安装包不进 Git**。发版后跑 `python pipeline/bin/deploy.py download` 同步到 93 下载页。

## macOS

| 路径 | 说明 |
|------|------|
| `apps/macos/GenlotVPN-macos-{arch}-{version}.zip` | **进 Git**：解压得到 `GenlotVPN.app` |
| `apps/macos/GenlotVPN-macos-{arch}-{version}/GenlotVPN.app` | 本机副本（已 gitignore，不提交） |

当前本次：`GenlotVPN-macos-x86_64-1.0.2`

### 同事使用

1. 从仓库取出对应 zip 并解压  
2. 双击 `GenlotVPN.app`（若 Gatekeeper 拦截：右键 → 打开）  
3. 本机需已安装并启动 **易安联 macOS Agent**（`127.0.0.1:30303`）

### 维护者重新打包后提交示例

```bash
bash bin/build-vpn-client-macos.sh --package
git add apps/README.md apps/macos/*.zip
git commit -m "release: macOS 客户端 1.0.2 (x86_64)"
git push
```

> `ruoyi-vpn-client/dist/`、`build-macos/`、`apps/macos/*/`（未压缩 .app）已 gitignore。  
> zip 约数十～百余 MB；若远端单文件限制更严，请改用 Git LFS 或网盘。
