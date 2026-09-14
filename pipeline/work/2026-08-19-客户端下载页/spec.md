# SPEC：客户端下载页

| 项 | 内容 |
|----|------|
| 日期 | 2026-08-19 |
| 状态 | **spec 已批准**（2026-08-19 选子代理按 plan 实现） |
| 安全级 | M1=S2；M3=S2；M2=S1（人看 nginx/证书；**不改** 9443 TLS/Pin） |
| 模块 | M1 / M2 / M3 |
| 熟悉度 | 熟悉（静态站 + 现有 93 部署 + `apps/` 约定） |

人批准本文件后才写业务代码与脚手架。实现后按第 7 节跑 `gate` → `deploy` → `accept`。本条**不改**桌面端源码，不跑 `client-ref`，不得声称客户端已验收。

对照：[模块拆分清单.md](./模块拆分清单.md)、[ADR-006](../_shared/ADR-006-93下载站.md)。

---

## 1. 背景与目标

同事和出差用户需要下载：易安联 SDK（Win/Mac）+ 当前 GenlotVPN 客户端。包已按约定放在仓库 `apps/`（Windows 客户端 zip 确认发布后再放入）。

现状：没有下载页；管理端「客户端版本策略」只有手填 URL；92 nginx 只托管管理端且不对公网。

目标：在 **93**（与桌面端同一张公网 IP，机房地址池之一，**不是**官网 IP）提供免登录静态站。按操作系统默认打开对应平台；中/EN；安装手册为独立栏目。发版只丢包到 `apps/` 再部署。

---

## 2. 范围

**做：**

- 静态站：下载区 + 安装手册栏目（二级：Windows / macOS 使用说明），中/EN。
- 按 User-Agent 默认平台（Windows / macOS；其它当 Windows）；顶栏可切换，记入 `sessionStorage`。
- 按浏览器语言默认中/EN（`zh*` → 中文，其它 → English）；顶栏 **中 / EN**，记入 `sessionStorage`。
- 93 独立 nginx：443 HTTPS、80 仅 301 到 HTTPS；前缀 `/genlotvpn/`；关 `autoindex`；**不反代** 9400/9443/92。
- 部署扫 `apps/` 按版本号取最大，写 `manifest.json`，同步页面与包到 93。
- 风格接近 [genlot.com](https://www.genlot.com/)：主色 `#123572`、辅色 `#379ada`、浅底 `#eaf1ff`、字体 PT Sans **本地预置**；Logo **拷进仓库**，不热链官网。
- 管理端版本策略的 Win/Mac 链接：部署后由人填同一条下载页 URL（见 3.6），本条不改策略表结构。

**不做：**

- 登录、IP 白名单、下载审计、自动更新安装器。
- 历史版本列表；按 `1.0.x` 各写一份手册。
- 第三种语言；完整运维手册（打包、Git、TLS 轮换、明文调试）上页。
- 下载走 Java / `ruoyi-file`；挂进 `ruoyi-ui` 登录后页面。
- 改 9443 / Pin / vpn-auth 业务；共用 9443 或 9400。
- 正规域名证书（本期可用自签；浏览器告警可点继续）。
- 脚本里假装公网 443 已由网络开通。

---

## 3. 行为

### 3.1 地址与入口

对外（证书告警可忽略）：

`https://<93公网IP>/genlotvpn/download/`

`<93公网IP>` 与桌面端 `serverHost` 为同一张 IP，写在本机 `envs.yaml`（如 `download.public_host`），**不写进本 spec 正文、不进对话**。内网验收可用 `https://10.27.0.93/genlotvpn/download/`。

| 路径 | 用途 |
|------|------|
| `/genlotvpn/download/` | 下载页（默认） |
| `/genlotvpn/download/manifest.json` | 当前包清单 |
| `/genlotvpn/download/files/...` | 安装包（与 `apps/` 相对路径对应） |
| `/genlotvpn/manual/` | 安装手册（二级选平台） |

`http://<同一主机>/...`（80）必须 301 到对应 `https://` URL。

### 3.2 顶栏与默认

顶栏：**Logo** + 站点名 + 栏目 **客户端下载** / **安装手册** + 平台 **Windows** / **macOS** + 语言 **中** / **EN**。

页脚：`© Genlot`（中英相同）。不要「仅内网」，不要官网联系表单。

打开规则（平台与语言独立）：

| 条件 | 默认 |
|------|------|
| UA 含 Mac / iPhone / iPad 等 | macOS |
| 其它（含 Windows、Linux、未知） | Windows |
| `sessionStorage` 已有平台 | 用已选，不再按 UA 覆盖 |
| 语言 `zh` 开头 | 中文 |
| 其它语言 | English |
| `sessionStorage` 已有语言 | 用已选 |

### 3.3 下载区（M1）

只展示**当前平台**两张卡：易安联 SDK、GenlotVPN 客户端。每张卡：产品名、文件名、版本、按钮。

| 状态 | 中文 | English |
|------|------|---------|
| 页标题 / 站点名 | 客户端下载 | Client Download |
| 栏目 | 客户端下载 | Download |
| 栏目 | 安装手册 | Installation Guide |
| 按钮有包 | 下载 | Download |
| 缺包 | 暂无安装包 | Package not available |
| 清单失败 | 暂时无法获取版本信息 | Unable to load version info |

缺包：按钮 **disabled**，文案用上表「缺包」。清单失败：下载区只出清单失败文案，手册仍可进。点下载即浏览器 GET 对应 `files/`，不另做进度条。

多个客户端 zip：按文件名里的 `x.y.z` **三段整数取最大**。SDK 按文件名中版本串取最大；每平台通常一个。旧 zip 可留在 `apps/`，页上不列历史。

### 3.4 安装手册（M1）

点 **安装手册** 出二级：**Windows 使用说明** / **macOS 使用说明**（英：**Windows Guide** / **macOS Guide**）。默认打开与当前平台相同的一篇。手册页头展示「当前客户端 {version}」/ `Current client {version}`（来自清单；无客户端则省略版本号）。

只写装机步骤，不写打包、Git、TLS 轮换、明文调试、防火墙脚本。

**Windows 使用说明（中文）**

1. 下载并安装 **易安联 SDK**，安装后启动 Agent。
2. 下载 **GenlotVPN** 压缩包，解压到本地目录（不要只拷贝单个 exe）。
3. 双击目录内 `GenlotVPN-x.y.z.exe`。
4. 选择线路 → 登录（验证码 + 账号密码）→ 按提示完成钉钉验证。

**macOS 使用说明（中文）**

1. 下载并安装 **易安联 SDK**（`.pkg`），安装后启动 Agent。
2. 下载 **GenlotVPN** zip 并解压，打开 `GenlotVPN.app`。若系统拦截：右键 → 打开。
3. 选择线路 → 登录 → 按提示完成钉钉验证。

英文对应（验收可抽查标题与「Download」；手册正文实现时按此意译，不得塞进构建步骤）：

1. Install the **YiAnLian / EnUES SDK**, then start the Agent.
2. Download the **GenlotVPN** package and extract it (Windows: keep exe next to the folder contents; macOS: open `GenlotVPN.app`, use Open if Gatekeeper blocks).
3. Choose a line → sign in → complete DingTalk verification when prompted.

### 3.5 发布与部署（M2 / M3）

人：确认发布后把 Windows 包放入 `apps/windows/GenlotVPN-win64-{x.y.z}.zip`。macOS 仍由现有打包脚本写入 `apps/macos/`。SDK 沿用现有 `EnUES*` 文件名。

部署（纳入现有 `deploy.py vpn`，多同步下载站，不另做流水线）：

1. 扫 `apps/`，按 3.3 规则写 `manifest.json`。
2. 同步静态页、字体、Logo、清单、安装包到 93 nginx 目录。
3. 重载下载 nginx。清单或缺包 **不得** 导致 `ruoyi-vpn-auth` 部署失败。

证书与私钥只在 93（对照 `node-93/.../certs/`），**不进 Git**。本期自签即可。

网络（人）：把 **93 这张公网 IP** 的 **443、80** 映到 93 本机同端口。不动官网 443，不动 9443 映射。

### 3.6 版本策略链接

部署可用后，超管在「客户端版本策略」把 Windows / macOS 下载链接都填：

`https://<93公网IP>/genlotvpn/download/`

发版不用改这条。本条不自动改库。

---

## 4. 数据与约定

无新表、无新 Java API。

### 4.1 `apps/` 文件名

| 类型 | 约定 | 现状（2026-08-19） |
|------|------|-------------------|
| Windows 客户端 | `apps/windows/GenlotVPN-win64-{x.y.z}.zip` | 尚无 → 页上暂无安装包 |
| macOS 客户端 | `apps/macos/GenlotVPN-macos-{arch}-{x.y.z}.zip` | 已有 `GenlotVPN-macos-x86_64-1.0.1.zip` |
| Windows SDK | `apps/windows/EnUES*_SDK.exe` | 已有 |
| macOS SDK | `apps/macos/EnUES*_SDK.pkg` | 已有 |

忽略：`apps/macos/*/` 未压缩 `.app`（gitignore）、`README.md`。

### 4.2 `manifest.json`（部署生成，人不用手改）

字段名固定英文。缺项为 `null`。

```json
{
  "windows": {
    "sdk": { "version": "3.3.7.0132", "file": "windows/EnUES_win_v3.3.7.0132_SDK.exe" },
    "client": { "version": "1.0.2", "file": "windows/GenlotVPN-win64-1.0.2.zip" }
  },
  "macos": {
    "sdk": { "version": "3.3.7.0114", "file": "macos/EnUESBOX_mac_v3.3.7.0114_SDK.pkg" },
    "client": { "version": "1.0.1", "file": "macos/GenlotVPN-macos-x86_64-1.0.1.zip" }
  }
}
```

页面把 `file` 拼到 `/genlotvpn/download/files/`。

### 4.3 仓库静态目录（实现时落地，名称可同义微调）

建议 `docker/node-93/nginx/`：`html/genlotvpn/`（页、手册、i18n、字体、Logo）、`conf/nginx.conf`、`certs/`（gitignore 私钥）。

---

## 5. 前端

不进 `ruoyi-ui`。单页静态 + 少量 JS 读清单、切平台/语言。PT Sans 与 Logo 本地文件。不请求 Google Fonts、不请求 genlot.com。

---

## 6. 菜单

无新管理端菜单。版本策略页已存在，只人手改链接。

---

## 7. 验收标准（命令可直接复制）

本条不改 Java / 管理端 Vue / 桌面端，**不要**默认全量 `gate.py`（会编客户端）。实现时扩展：

- `gate.py download`：静态文件齐全；对当前 `apps/` 跑一遍扫目录，能产出合法 JSON（允许 Windows client 为 null）。
- `deploy.py vpn`：在现有 93 vpn-auth 之外同步下载站并重载下载 nginx。
- `accept.py download`：免登录；打开下载站；自签证书 **忽略校验**；断言文案。`--base-url` 默认 `https://10.27.0.93`（可用 `DOWNLOAD_E2E_BASE_URL` 覆盖）。

### 7.1 下载页（M1）

| 场景 | `--path` | 操作 | `--expect-text` |
|------|----------|------|-----------------|
| 中文下载页 | `/genlotvpn/download/` | 打开（默认中文或点 **中**） | `客户端下载` |
| 英文 | `/genlotvpn/download/` | 点 **EN** | `Client Download` |
| 手册 | `/genlotvpn/manual/` | 打开或点 **安装手册** | `安装手册` 或 `Installation Guide`（随当前语言） |
| 缺 Windows 客户端 | `/genlotvpn/download/` | 切到 Windows | `暂无安装包` 或 `Package not available`（在放入 Win zip **之前**） |

不得用 92 `/index` 或管理端登录冒烟代替。

### 7.2 构建与部署

```bash
python -u pipeline/bin/gate.py download --run 2026-08-19-客户端下载页
python -u pipeline/bin/deploy.py vpn --run 2026-08-19-客户端下载页
python -u pipeline/bin/accept.py download --run 2026-08-19-客户端下载页 \
  --path /genlotvpn/download/ \
  --expect-text 客户端下载 --title 下载页中文 --module M1 --round 0
python -u pipeline/bin/accept.py download --run 2026-08-19-客户端下载页 \
  --path /genlotvpn/download/ --click EN \
  --expect-text "Client Download" --title 下载页英文 --module M1 --round 0
python -u pipeline/bin/accept.py download --run 2026-08-19-客户端下载页 \
  --path /genlotvpn/manual/ --click 中 \
  --expect-text 安装手册 --title 安装手册 --module M1 --round 0
```

Windows zip 尚未入库时加一条（有包后这条改为断言版本号，不再断言暂无）：

```bash
python -u pipeline/bin/accept.py download --run 2026-08-19-客户端下载页 \
  --path /genlotvpn/download/ --click Windows \
  --expect-text 暂无安装包 --title Windows缺包 --module M1 --round 0
```

有包后改为核对本页「下载」链接能取到文件（HEAD，不全量拉 zip）：

```bash
python -u pipeline/bin/accept.py download --run 2026-08-19-客户端下载页 \
  --path /genlotvpn/download/ --click Windows --expect-text 下载 \
  --check-files --title Windows下载 --module M1 --round 0
python -u pipeline/bin/accept.py download --run 2026-08-19-客户端下载页 \
  --path /genlotvpn/download/ --click macOS --expect-text 下载 \
  --check-files --title macOS下载 --module M1 --round 0
```

### 7.3 客户端 AI 走查

本条不改客户端，**不跑** `client-ref`。

### 7.4 人验

| 模块 | 方式 |
|------|------|
| M2 | 人看：nginx 无 9400/9443 反代；`autoindex off`；80→443；证书在 93 不在 Git。公网 443 是否已映由网络工程师确认，未映不得声称外网已通 |
| M3 | 人：放入或确认 `apps/` 后部署，清单版本与最高 zip 一致；macOS 现有 zip 可下载 |

---

## 8. 安全与合规

- M1/M3=S2；M2=S1。不得把 M2 降成「不用看 nginx」。不得改 9443 证书/Pin 充作本页证书。
- 下载 nginx 只出 `/genlotvpn/` 静态文件。关目录浏览。只提供清单中的当前包路径。
- 私钥、SSH、钉钉 token 不入库、不进对话。公网 IP 用 envs 占位。
- 免登录是产品选择：安装包本来就要分发。风险见设计讨论（隔离做好则低；危险的是反代 vpn-auth）。
- 不关桌面/选线验证码。

---

## 9. 非目标（防止膨胀）

登录墙、IP 白名单、下载次数统计、历史版本架、按小版本多份手册、第三语言、运维手册上页、Java 列目录、挂 92、共用 9443/9400、自动改版本策略表、正式域名证书、自动更新安装器、`client-ref`。
