# 客户端下载页 Implementation Plan

> **For agentic workers:** 按本计划逐任务实现。步骤用 checkbox（`- [ ]`）跟踪。门禁以 spec 第 7 节 `gate.py download` → `deploy.py` → `accept.py download` 为准。清单扫描用 `unittest`。**未批书面 spec 不写业务代码。** 不要自动 `git commit`，除非用户明确要求提交。跨模块可用 `_subagent-driven-development`，本会话内联实现也可以。

**Goal:** 在 93 用独立 nginx 提供免登录 HTTPS 下载站（`/genlotvpn/`）：按系统默认平台、中/EN、安装手册栏目；部署扫 `apps/` 写出当前包清单。

**Architecture:** 静态 HTML/JS 读 `manifest.json`。`download_manifest.py` 按文件名取最高版本。93 新容器 `ruoyi-download-nginx`（host 网络，80→443，只出静态，不反代 9400/9443）。`deploy.py vpn` 在现有 vpn-auth 之后同步下载站；另加 `deploy.py download` 以便本条不必重编 92。

**Tech Stack:** 静态 HTML/CSS/JS、nginx、Docker compose（node-93）、Python 3（unittest / paramiko / Playwright）。

**Spec:** [spec.md](./spec.md)

## Global Constraints

- 文案中文：`GenlotVPN 客户端下载` / `客户端下载` / `安装手册` / `下载` / `暂无安装包` / `暂时无法获取版本信息` / `Windows 使用说明` / `macOS 使用说明`
- 文案英文：`GenlotVPN Client Download` / `Download` / `Installation Guide` / `Download` / `Package not available` / `Unable to load version info` / `Windows Guide` / `macOS Guide`
- 路径：`/genlotvpn/download/`、`/genlotvpn/download/manifest.json`、`/genlotvpn/download/files/`、`/genlotvpn/manual/`
- 主色 `#123572`，辅色 `#379ada`，浅底 `#eaf1ff`；Logo 与字体本地，不请求 genlot.com / Google Fonts
- 平台：UA 含 `Mac|iPhone|iPad|iPod` → macos，其它 → windows；`sessionStorage` 键 `gv-platform`
- 语言：`navigator.language` 以 `zh` 开头 → zh，其它 → en；键 `gv-lang`
- 客户端 zip 版本：文件名 `x.y.z` 三段整数取最大；缺项 `null`；Windows 客户端目前可无
- 不改 9443 / Pin / vpn-auth 业务；nginx **禁止** `proxy_pass` 到 9400/9443
- 证书只在 93 本机生成，不进 Git；accept 忽略 HTTPS 校验
- 不跑 `client-ref`；不关桌面验证码
- 提交：仅当用户说「提交」时才 `git commit`

## 文件地图

| 文件 | 职责 |
|------|------|
| `pipeline/work/bin/download_manifest.py` | 扫 `apps/` → `manifest.json` 结构（纯函数，可单测） |
| `pipeline/work/bin/test_download_manifest.py` | 版本比较、文件名、缺包为 null |
| `docker/node-93/nginx/html/genlotvpn/index.html` | 壳：顶栏、下载区、手册区 |
| `docker/node-93/nginx/html/genlotvpn/css/app.css` | 官网近似风格 |
| `docker/node-93/nginx/html/genlotvpn/js/i18n.js` | zh/en 字典 |
| `docker/node-93/nginx/html/genlotvpn/js/app.js` | 平台/语言/路由/读清单 |
| `docker/node-93/nginx/html/genlotvpn/fonts/` | PT Sans woff2（拉丁）；中文走系统字体 |
| `docker/node-93/nginx/html/genlotvpn/img/logo.png` | 从官网一次下载后入库 |
| `docker/node-93/nginx/conf/nginx.conf` | 80 跳 443；`/genlotvpn/`；`autoindex off` |
| `docker/node-93/nginx/certs/README.md` | 说明私钥不入库 |
| `docker/node-93/docker-compose.yml` | 增加 `ruoyi-download-nginx` |
| `docker/node-93/deploy.sh` | `port` 增加 80/443 |
| `pipeline/work/bin/deploy_admin_92.py` | `vpn` 后同步下载站；新增 `--download` |
| `pipeline/bin/deploy.py` | 识别 `download` 子命令 |
| `pipeline/bin/gate.py` | `download` 范围：静态齐全 + 扫 `apps/` |
| `pipeline/work/bin/accept_download.py` | 免登录 Playwright |
| `pipeline/bin/accept.py` | 转发 `download` |
| `apps/README.md` | 补 Windows 约定与下载页地址形态 |
| `.gitignore` | `docker/node-93/nginx/certs/*.key`（及 crt 若本机生成） |
| `pipeline/deploy/envs.yaml.example` | `accept.download_base_url` |

---

### Task 1: 清单扫描（M3 核心，先单测）

**Files:**
- Create: `pipeline/work/bin/download_manifest.py`
- Create: `pipeline/work/bin/test_download_manifest.py`

**Interfaces:**
- Consumes: 仓库根下 `apps/windows/`、`apps/macos/`
- Produces: `build_manifest(apps_dir: Path) -> dict`，形状与 spec 4.2 一致；`write_manifest(apps_dir, dest: Path) -> dict`

- [ ] **Step 1: 写失败单测**

```python
# pipeline/work/bin/test_download_manifest.py
# -*- coding: utf-8 -*-
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from download_manifest import build_manifest, pick_max_xyz, parse_xyz


class ManifestTest(unittest.TestCase):
    def test_parse_xyz(self):
        self.assertEqual(parse_xyz("1.0.2"), (1, 0, 2))
        self.assertIsNone(parse_xyz("1.0"))

    def test_pick_max_xyz(self):
        names = [
            "GenlotVPN-win64-1.0.1.zip",
            "GenlotVPN-win64-1.0.2.zip",
            "readme.txt",
        ]
        got = pick_max_xyz(names, r"^GenlotVPN-win64-(\d+\.\d+\.\d+)\.zip$")
        self.assertEqual(got[0], "GenlotVPN-win64-1.0.2.zip")
        self.assertEqual(got[1], "1.0.2")

    def test_missing_windows_client_is_null(self):
        root = Path(tempfile.mkdtemp())
        (root / "windows").mkdir()
        (root / "macos").mkdir()
        (root / "windows" / "EnUES_win_v3.3.7.0132_SDK.exe").write_bytes(b"x")
        (root / "macos" / "EnUESBOX_mac_v3.3.7.0114_SDK.pkg").write_bytes(b"x")
        (root / "macos" / "GenlotVPN-macos-x86_64-1.0.1.zip").write_bytes(b"x")
        m = build_manifest(root)
        self.assertIsNone(m["windows"]["client"])
        self.assertEqual(m["windows"]["sdk"]["version"], "3.3.7.0132")
        self.assertEqual(m["windows"]["sdk"]["file"], "windows/EnUES_win_v3.3.7.0132_SDK.exe")
        self.assertEqual(m["macos"]["client"]["version"], "1.0.1")
        self.assertEqual(m["macos"]["client"]["file"], "macos/GenlotVPN-macos-x86_64-1.0.1.zip")

    def test_ignore_unpacked_app_dir(self):
        root = Path(tempfile.mkdtemp())
        (root / "macos").mkdir()
        unpacked = root / "macos" / "GenlotVPN-macos-x86_64-1.0.0"
        unpacked.mkdir()
        (unpacked / "GenlotVPN.app").mkdir()
        (root / "macos" / "GenlotVPN-macos-x86_64-1.0.1.zip").write_bytes(b"x")
        m = build_manifest(root)
        self.assertEqual(m["macos"]["client"]["version"], "1.0.1")


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 跑测，确认失败**

```bash
python -m unittest pipeline.work.bin.test_download_manifest
```

在仓库根执行若包路径不对，改为：

```bash
python pipeline/work/bin/test_download_manifest.py
```

Expected: `ModuleNotFoundError` 或 `ImportError`

- [ ] **Step 3: 实现 `download_manifest.py`**

```python
# -*- coding: utf-8 -*-
"""扫 apps/ 生成下载站 manifest（字段名固定英文）。"""
from __future__ import annotations

import json
import re
from pathlib import Path

WIN_CLIENT = re.compile(r"^GenlotVPN-win64-(\d+\.\d+\.\d+)\.zip$", re.I)
MAC_CLIENT = re.compile(r"^GenlotVPN-macos-[^-]+-(\d+\.\d+\.\d+)\.zip$", re.I)
WIN_SDK = re.compile(r"^EnUES.*_SDK\.exe$", re.I)
MAC_SDK = re.compile(r"^EnUES.*_SDK\.pkg$", re.I)
SDK_VER = re.compile(r"v(\d+(?:\.\d+)+)", re.I)


def parse_xyz(text):
    m = re.match(r"^(\d+)\.(\d+)\.(\d+)$", text or "")
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3))


def pick_max_xyz(names, pattern):
    best = None
    best_key = None
    cre = re.compile(pattern, re.I) if isinstance(pattern, str) else pattern
    for name in names:
        m = cre.match(name)
        if not m:
            continue
        key = parse_xyz(m.group(1))
        if key is None:
            continue
        if best_key is None or key > best_key:
            best_key = key
            best = (name, m.group(1))
    return best


def _sdk_version(name):
    m = SDK_VER.search(name)
    return m.group(1) if m else ""


def _sdk_key(name):
    ver = _sdk_version(name)
    parts = []
    for p in ver.split("."):
        try:
            parts.append(int(p))
        except ValueError:
            parts.append(0)
    return tuple(parts)


def pick_max_sdk(names, pattern):
    best = None
    best_key = None
    for name in names:
        if not pattern.match(name):
            continue
        key = _sdk_key(name)
        if best_key is None or key > best_key:
            best_key = key
            best = name
    return best


def _entry(rel_dir, filename, version):
    if not filename:
        return None
    return {"version": version or "", "file": "{}/{}".format(rel_dir, filename)}


def build_manifest(apps_dir):
    apps_dir = Path(apps_dir)
    win_names = [p.name for p in (apps_dir / "windows").glob("*") if p.is_file()]
    mac_names = [p.name for p in (apps_dir / "macos").glob("*") if p.is_file()]
    win_c = pick_max_xyz(win_names, WIN_CLIENT)
    mac_c = pick_max_xyz(mac_names, MAC_CLIENT)
    win_s = pick_max_sdk(win_names, WIN_SDK)
    mac_s = pick_max_sdk(mac_names, MAC_SDK)
    return {
        "windows": {
            "sdk": _entry("windows", win_s, _sdk_version(win_s) if win_s else ""),
            "client": _entry("windows", win_c[0], win_c[1]) if win_c else None,
        },
        "macos": {
            "sdk": _entry("macos", mac_s, _sdk_version(mac_s) if mac_s else ""),
            "client": _entry("macos", mac_c[0], mac_c[1]) if mac_c else None,
        },
    }


def write_manifest(apps_dir, dest):
    data = build_manifest(apps_dir)
    dest = Path(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    return data
```

- [ ] **Step 4: 再跑单测**

```bash
python pipeline/work/bin/test_download_manifest.py
```

Expected: `OK`

---

### Task 2: 静态站（M1）

**Files:**
- Create: `docker/node-93/nginx/html/genlotvpn/index.html`
- Create: `docker/node-93/nginx/html/genlotvpn/css/app.css`
- Create: `docker/node-93/nginx/html/genlotvpn/js/i18n.js`
- Create: `docker/node-93/nginx/html/genlotvpn/js/app.js`
- Create: `docker/node-93/nginx/html/genlotvpn/img/logo.png`（实现时用 `curl.exe` 从官网那张 Logo 下一次，入库）
- Create: `docker/node-93/nginx/html/genlotvpn/fonts/README.md`（说明 PT Sans 拉丁 woff2 放这里；中文用 `Microsoft YaHei` / `PingFang SC`）

**Interfaces:**
- Consumes: `GET /genlotvpn/download/manifest.json`
- Produces: 路由 `/genlotvpn/download/` 与 `/genlotvpn/manual/` 同一 `index.html`；下载链接 `/genlotvpn/download/files/` + `file`

- [ ] **Step 1: 写 `i18n.js`（文案必须与 spec 一致）**

```javascript
window.GV_I18N = {
  zh: {
    title: "GenlotVPN 客户端下载",
    navDownload: "客户端下载",
    navManual: "安装手册",
    download: "下载",
    unavailable: "暂无安装包",
    manifestFail: "暂时无法获取版本信息",
    winGuide: "Windows 使用说明",
    macGuide: "macOS 使用说明",
    currentClient: "当前客户端 {version}",
    sdkName: "易安联 SDK",
    clientName: "GenlotVPN 客户端",
    footer: "© Genlot",
    intranetHint: "",
    winSteps: [
      "下载并安装易安联 SDK，安装后启动 Agent。",
      "下载 GenlotVPN 压缩包，解压到本地目录（不要只拷贝单个 exe）。",
      "双击目录内 GenlotVPN-x.y.z.exe。",
      "选择线路 → 登录（验证码 + 账号密码）→ 按提示完成钉钉验证。"
    ],
    macSteps: [
      "下载并安装易安联 SDK（.pkg），安装后启动 Agent。",
      "下载 GenlotVPN zip 并解压，打开 GenlotVPN.app。若系统拦截：右键 → 打开。",
      "选择线路 → 登录 → 按提示完成钉钉验证。"
    ]
  },
  en: {
    title: "GenlotVPN Client Download",
    navDownload: "Download",
    navManual: "Installation Guide",
    download: "Download",
    unavailable: "Package not available",
    manifestFail: "Unable to load version info",
    winGuide: "Windows Guide",
    macGuide: "macOS Guide",
    currentClient: "Current client {version}",
    sdkName: "YiAnLian / EnUES SDK",
    clientName: "GenlotVPN Client",
    footer: "© Genlot",
    winSteps: [
      "Install the YiAnLian / EnUES SDK, then start the Agent.",
      "Download the GenlotVPN zip and extract it. Keep the exe next to the folder contents.",
      "Double-click GenlotVPN-x.y.z.exe.",
      "Choose a line → sign in → complete DingTalk verification when prompted."
    ],
    macSteps: [
      "Install the YiAnLian / EnUES SDK (.pkg), then start the Agent.",
      "Download and unzip GenlotVPN, then open GenlotVPN.app. If blocked: right-click → Open.",
      "Choose a line → sign in → complete DingTalk verification when prompted."
    ]
  }
};
```

禁止出现「仅内网」。手册步骤禁止出现打包命令、Git、TLS、明文调试。

- [ ] **Step 2: 写 `app.js`**

```javascript
(function () {
  var KEY_P = "gv-platform";
  var KEY_L = "gv-lang";

  function detectPlatform() {
    var s = sessionStorage.getItem(KEY_P);
    if (s === "windows" || s === "macos") return s;
    var ua = navigator.userAgent || "";
    return /Mac|iPhone|iPad|iPod/.test(ua) ? "macos" : "windows";
  }

  function detectLang() {
    var s = sessionStorage.getItem(KEY_L);
    if (s === "zh" || s === "en") return s;
    var lang = (navigator.language || "en").toLowerCase();
    return lang.indexOf("zh") === 0 ? "zh" : "en";
  }

  function isManual() {
    return /\/manual\/?$/.test(location.pathname) || location.pathname.indexOf("/manual/") !== -1;
  }

  var state = { platform: detectPlatform(), lang: detectLang(), manifest: null, fail: false };

  function t(key) {
    return window.GV_I18N[state.lang][key];
  }

  function setPlatform(p) {
    state.platform = p;
    sessionStorage.setItem(KEY_P, p);
    render();
  }

  function setLang(l) {
    state.lang = l;
    sessionStorage.setItem(KEY_L, l);
    render();
  }

  function fileName(rel) {
    if (!rel) return "";
    var i = rel.lastIndexOf("/");
    return i >= 0 ? rel.slice(i + 1) : rel;
  }

  function cardHtml(kind, entry) {
    var name = kind === "sdk" ? t("sdkName") : t("clientName");
    if (!entry) {
      return (
        '<article class="card"><h3>' + name + "</h3><p class=\"muted\">—</p>" +
        '<button type="button" class="btn" disabled>' + t("unavailable") + "</button></article>"
      );
    }
    var href = "/genlotvpn/download/files/" + entry.file;
    return (
      '<article class="card"><h3>' + name + "</h3>" +
      "<p>" + fileName(entry.file) + "</p>" +
      '<p class="ver">' + (entry.version || "") + "</p>" +
      '<a class="btn" href="' + href + '">' + t("download") + "</a></article>"
    );
  }

  function render() {
    document.title = t("title");
    document.documentElement.lang = state.lang === "zh" ? "zh-CN" : "en";
    document.getElementById("site-title").textContent = t("title");
    document.getElementById("nav-download").textContent = t("navDownload");
    document.getElementById("nav-manual").textContent = t("navManual");
    document.getElementById("btn-zh").className = state.lang === "zh" ? "on" : "";
    document.getElementById("btn-en").className = state.lang === "en" ? "on" : "";
    document.getElementById("btn-win").className = state.platform === "windows" ? "on" : "";
    document.getElementById("btn-mac").className = state.platform === "macos" ? "on" : "";
    document.getElementById("page-download").hidden = isManual();
    document.getElementById("page-manual").hidden = !isManual();
    document.getElementById("footer").textContent = t("footer");

    var box = document.getElementById("cards");
    if (state.fail) {
      box.innerHTML = "<p class=\"error\">" + t("manifestFail") + "</p>";
    } else if (state.manifest) {
      var plat = state.manifest[state.platform] || {};
      box.innerHTML = cardHtml("sdk", plat.sdk) + cardHtml("client", plat.client);
    }

    var platKey = state.platform === "macos" ? "macos" : "windows";
    var client = state.manifest && state.manifest[platKey] && state.manifest[platKey].client;
    var head = document.getElementById("manual-head");
    head.textContent = client && client.version
      ? t("currentClient").replace("{version}", client.version)
      : "";
    document.getElementById("guide-win").textContent = t("winGuide");
    document.getElementById("guide-mac").textContent = t("macGuide");
    document.getElementById("guide-win").className = state.platform === "windows" ? "on" : "";
    document.getElementById("guide-mac").className = state.platform === "macos" ? "on" : "";
    var steps = state.platform === "macos" ? t("macSteps") : t("winSteps");
    document.getElementById("steps").innerHTML = steps.map(function (s) {
      return "<li>" + s + "</li>";
    }).join("");
  }

  function bind() {
    document.getElementById("btn-zh").onclick = function () { setLang("zh"); };
    document.getElementById("btn-en").onclick = function () { setLang("en"); };
    document.getElementById("btn-win").onclick = function () { setPlatform("windows"); };
    document.getElementById("btn-mac").onclick = function () { setPlatform("macos"); };
    document.getElementById("guide-win").onclick = function () { setPlatform("windows"); };
    document.getElementById("guide-mac").onclick = function () { setPlatform("macos"); };
    document.getElementById("nav-download").setAttribute("href", "/genlotvpn/download/");
    document.getElementById("nav-manual").setAttribute("href", "/genlotvpn/manual/");
  }

  bind();
  render();
  fetch("/genlotvpn/download/manifest.json", { cache: "no-store" })
    .then(function (r) {
      if (!r.ok) throw new Error("bad");
      return r.json();
    })
    .then(function (j) {
      state.manifest = j;
      state.fail = false;
      render();
    })
    .catch(function () {
      state.fail = true;
      render();
    });
})();
```

- [ ] **Step 3: 写 `index.html` + `app.css`**

`index.html` 结构（id 必须与 `app.js` 一致）：`#site-title`、`#nav-download`、`#nav-manual`、`#btn-zh`（文本 `中`）、`#btn-en`（文本 `EN`）、`#btn-win`（`Windows`）、`#btn-mac`（`macOS`）、`#page-download` `#cards`、`#page-manual` `#manual-head` `#guide-win` `#guide-mac` `#steps`、`#footer`。

`app.css`：顶栏背景 `#123572` 白字；页面背景 `#eaf1ff`；按钮/链接 `#123572`；当前项/版本 `#379ada`；正文字 `#212121`；`font-family: "PT Sans", "PingFang SC", "Microsoft YaHei", sans-serif`。卡片两列（窄屏一列）。不要官网整站导航和联系表单。

- [ ] **Step 4: 拉 Logo（只一次，之后用仓库文件）**

```bash
curl.exe -L -o docker/node-93/nginx/html/genlotvpn/img/logo.png "https://www.genlot.com/uploads/202408/Genlot-logo-V_1722846882_WNo_114d114.png"
```

若失败，用现有 `ruoyi-ui/src/assets/logo/logo.png` 拷一份，不得运行时热链。

- [ ] **Step 5: 本机用任意静态服务器打开 `index.html`，改 UA / 点 中·EN / Windows·macOS，看标题与缺包按钮。** 清单可用一份手写 `manifest.json` 放在同级调试；进 93 后由部署生成。

---

### Task 3: 93 nginx（M2）

**Files:**
- Create: `docker/node-93/nginx/conf/nginx.conf`
- Create: `docker/node-93/nginx/certs/README.md`
- Create: `docker/node-93/nginx/html/genlotvpn/`（Task 2 已有）
- Modify: `docker/node-93/docker-compose.yml`（追加服务，**不要改** `ruoyi-vpn-auth` 端口）
- Modify: `docker/node-93/deploy.sh`（`port` 增加 80/443）
- Modify: `.gitignore`（证书私钥）

**Interfaces:**
- Consumes: 宿主机目录 `nginx/html/genlotvpn`、`nginx/files`、`nginx/certs`
- Produces: 容器名 `ruoyi-download-nginx`；听 80/443

- [ ] **Step 1: `nginx.conf` 全文如下（禁止出现 `proxy_pass`）**

```nginx
worker_processes  1;
events { worker_connections  1024; }
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    client_max_body_size 0;
    server {
        listen 80;
        server_name _;
        return 301 https://$host$request_uri;
    }
    server {
        listen 443 ssl;
        server_name _;
        ssl_certificate     /etc/nginx/certs/download.crt;
        ssl_certificate_key /etc/nginx/certs/download.key;
        autoindex off;
        location = /genlotvpn/download/manifest.json {
            alias /home/ruoyi/download-files/manifest.json;
            default_type application/json;
        }
        location /genlotvpn/download/files/ {
            alias /home/ruoyi/download-files/apps/;
            autoindex off;
            add_header Content-Disposition "attachment";
        }
        location /genlotvpn/ {
            alias /home/ruoyi/genlotvpn/;
            try_files $uri $uri/ /index.html;
        }
        location / { return 404; }
    }
}
```

注意：`alias .../apps/` 对应部署把仓库 `apps/windows` 同步到远端 `nginx/files/apps/windows`，则 `file: windows/xxx.zip` 能对上。若 `try_files` 配 `alias` 在所用 nginx 版本上异常，改为：

```nginx
location /genlotvpn/ {
    root /home/ruoyi;
    try_files $uri /genlotvpn/index.html;
}
```

并把站点挂到容器 `/home/ruoyi/genlotvpn`。实现时二选一，以「打开 `/genlotvpn/download/` 和 `/genlotvpn/manual/` 都出同一页」为准。

- [ ] **Step 2: compose 追加（与 vpn-auth 并列，host 网络）**

```yaml
  ruoyi-download-nginx:
    container_name: ruoyi-download-nginx
    image: nginx
    network_mode: host
    environment:
      - TZ=Asia/Shanghai
    volumes:
      - ./nginx/html/genlotvpn:/home/ruoyi/genlotvpn:ro
      - ./nginx/files:/home/ruoyi/download-files:ro
      - ./nginx/conf/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/certs:/etc/nginx/certs:ro
      - ./nginx/logs:/var/log/nginx
```

- [ ] **Step 3: `deploy.sh` 的 `port()` 增加**

```sh
firewall-cmd --add-port=80/tcp --permanent
firewall-cmd --add-port=443/tcp --permanent
```

不要删 9400/9443。

- [ ] **Step 4: `.gitignore` 增加**

```
docker/node-93/nginx/certs/*.key
docker/node-93/nginx/certs/*.crt
```

`certs/README.md` 写：在 93 用 openssl 生成 `download.crt` / `download.key`，不入库、不与 vpn-auth 的 9443 证书混用。

- [ ] **Step 5: 人审配置**（S1）：搜 `nginx.conf` 确认无 `proxy_pass`、有 `autoindex off`、80 仅 `return 301`。

---

### Task 4: 部署同步（M3）

**Files:**
- Modify: `pipeline/work/bin/deploy_admin_92.py`
- Modify: `pipeline/bin/deploy.py`
- Modify: `pipeline/deploy/envs.yaml.example`

**Interfaces:**
- Consumes: `download_manifest.write_manifest`；`REPO/apps`；`DOCKER/node-93/nginx`
- Produces: 远端 `/data/genlotvpn/nginx/...`；`docker compose up -d --build ruoyi-download-nginx`。缺 Windows zip **不得** 让 vpn-auth 失败。

- [ ] **Step 1: `deploy.py` 增加 `download` 作用域**

现有：

```python
if args and args[0].lower() in ("ui", "vpn", "all"):
    scope = args[0].lower()
    mapped = ["--" + args[0].lower()] + args[1:]
```

改为同时接受 `download`，并 `mapped = ["--download"] + args[1:]`。`scope` 写入门禁仍用 `download`。

- [ ] **Step 2: `parse_args` 增加 `--download`**

`do_download = args.download or args.vpn or args.all`。`--download` 时：`do_ui = do_admin_jars = do_vpn = False`，只连 93 同步下载站。`vpn`/`all` 在现有 vpn-auth 成功或跳过逻辑之后 **仍要** 调下载同步（vpn-auth 失败则整次失败；清单缺包不失败）。

- [ ] **Step 3: 实现 `sync_download_site(ssh, sftp, remote93)`**

```python
def sync_download_site(ssh, sftp, remote93):
    from download_manifest import write_manifest
    apps = REPO / "apps"
    files_root = DOCKER / "node-93" / "nginx" / "files"
    if files_root.exists():
        shutil.rmtree(files_root)
    (files_root / "apps").mkdir(parents=True)
    # 只拷文件，不拷 macos 下已解压目录
    for plat in ("windows", "macos"):
        src = apps / plat
        dst = files_root / "apps" / plat
        dst.mkdir(parents=True, exist_ok=True)
        if src.is_dir():
            for p in src.iterdir():
                if p.is_file():
                    shutil.copy2(p, dst / p.name)
    write_manifest(apps, files_root / "manifest.json")
    remote = remote93.rstrip("/")
    run_remote(
        ssh,
        "mkdir -p '{0}/nginx/html/genlotvpn' '{0}/nginx/files' '{0}/nginx/conf' '{0}/nginx/certs' '{0}/nginx/logs'".format(remote),
    )
    run_remote(
        ssh,
        "test -f '{0}/nginx/certs/download.crt' || ("
        "openssl req -x509 -nodes -days 3650 -newkey rsa:2048 "
        "-keyout '{0}/nginx/certs/download.key' "
        "-out '{0}/nginx/certs/download.crt' "
        "-subj '/CN=genlotvpn-download')".format(remote),
    )
    n1 = sftp_put_tree(sftp, DOCKER / "node-93" / "nginx" / "html" / "genlotvpn", remote + "/nginx/html/genlotvpn")
    n2 = sftp_put_tree(sftp, files_root, remote + "/nginx/files")
    sftp.put(str(DOCKER / "node-93" / "nginx" / "conf" / "nginx.conf"), remote + "/nginx/conf/nginx.conf")
    sftp.put(str(DOCKER / "node-93" / "docker-compose.yml"), remote + "/docker-compose.yml")
    print("[deploy] 下载站页面 {} 个文件，包+清单 {} 个".format(n1, n2), flush=True)
    compose_up(ssh, remote, ["ruoyi-download-nginx"])
```

`sys.path` 已有 `work_bin()`，可 `from download_manifest import write_manifest`。

大 zip（数十～百 MB）SFTP 可能较慢，属预期，不要改走 Java 文件服务。

- [ ] **Step 4: `envs.yaml.example` 增加（不要把真实公网 IP 写进仓库文档正文）**

```yaml
accept:
  download_base_url: https://10.27.0.93
```

`local_env` 若已映射 `accept.base_url` → `ADMIN_E2E_BASE_URL`，同样映射 `accept.download_base_url` → `DOWNLOAD_E2E_BASE_URL`。实现时对照 `pipeline/bin/local_env.py` 现有键。

---

### Task 5: gate + accept（第 7 节命令）

**Files:**
- Modify: `pipeline/bin/gate.py`
- Modify: `pipeline/bin/accept.py`
- Create: `pipeline/work/bin/accept_download.py`

**Interfaces:**
- Consumes: Task 1–4 产物
- Produces: `gate.py download` 退出 0；`accept.py download` 免登录断言文案

- [ ] **Step 1: `gate.py` 增加 `download`**

在 `main()` 里：`scope == "download"` 时 `do_java = do_ui = do_client = False`，改为：

```python
def gate_download() -> None:
    site = REPO / "docker" / "node-93" / "nginx" / "html" / "genlotvpn"
    for rel in ("index.html", "css/app.css", "js/app.js", "js/i18n.js"):
        if not (site / rel).is_file():
            raise SystemExit("[gate] 失败: 缺少 " + rel)
    conf = (REPO / "docker" / "node-93" / "nginx" / "conf" / "nginx.conf").read_text(encoding="utf-8")
    if "proxy_pass" in conf:
        raise SystemExit("[gate] 失败: 下载 nginx 不得 proxy_pass")
    if "autoindex off" not in conf:
        raise SystemExit("[gate] 失败: 需要 autoindex off")
    sys.path.insert(0, str(REPO / "pipeline" / "work" / "bin"))
    from download_manifest import build_manifest
    m = build_manifest(REPO / "apps")
    if "windows" not in m or "macos" not in m:
        raise SystemExit("[gate] 失败: manifest 结构不对")
    print("[gate] manifest windows.client=", m["windows"]["client"], flush=True)
```

- [ ] **Step 2: `accept.py` 增加**

```python
if args and args[0] == "download":
    sys.argv = [sys.argv[0]] + args[1:]
    from accept_download import main as download_main
    return download_main()
```

- [ ] **Step 3: `accept_download.py` 对照 `accept_admin.py` 删登录**

要点（实现时抄 `accept_admin.py` 的 `--run` / 截图 / 写报告，不要抄登录）：

```python
p.add_argument("--base-url", default=env("DOWNLOAD_E2E_BASE_URL", "https://10.27.0.93"))
p.add_argument("--path", default="/genlotvpn/download/")
p.add_argument("--expect-text", required=True)
p.add_argument("--click", action="append", default=[])
# browser = chromium.launch(...)
# context = browser.new_context(ignore_https_errors=True)
# page.goto(base + path)
# for label in clicks: page.get_by_text(label, exact=True).first.click()
# assert expect_text in page.content()
```

`--click EN` / `中` / `Windows` 对应顶栏按钮可见文本。报告写入当次 `验收报告/`，门禁 `append_step(..., "accept", ...)` 与现网管理端 accept 一样带 `--run`。

- [ ] **Step 4: 本地先跑 gate（不声称完成）**

```bash
python -u pipeline/bin/gate.py download --run 2026-08-19-客户端下载页
```

Expected: 打印 manifest（Windows client 可为 `None`），退出 0。

---

### Task 6: 文档与人验清单

**Files:**
- Modify: `apps/README.md`
- Modify: `pipeline/work/2026-08-19-客户端下载页/README.md`（计划已写）
- Modify: `docker/README_DEPLOY_3NODES.md`（93 端口表增加 80/443 下载站一句）

**Interfaces:**
- Consumes: spec 3.5 / 3.6
- Produces: 发版说明；不写真实公网 IP

- [ ] **Step 1: `apps/README.md` 增加 Windows 行与「部署后页从清单读当前最高版本」**。下载 URL 写成 `https://<93公网IP>/genlotvpn/download/`。

- [ ] **Step 2: 三机文档 93 行改为：vpn-auth 9400/9443 + 下载 nginx 80/443。** 写明 443 是这张 IP 自己的 HTTPS，不是官网 443。

- [ ] **Step 3: spec 已批准且代码就绪后，按第 7 节 `deploy` + `accept`。** 公网映射未做时，只验 `https://10.27.0.93`，报告写「内网 93 已通，公网 443 待网络」。不得写「外网已完成」。

- [ ] **Step 4: 人在管理端把版本策略两个链接填成同一下载页 URL（envs 里的公网 IP，对话用 `***`）。**

---

## 自检（对照 spec）

| spec | 任务 |
|------|------|
| 3.1 路径与 80→443 | Task 3 |
| 3.2 顶栏/UA/语言 | Task 2 |
| 3.3 下载区文案与缺包 | Task 2 + Task 1 |
| 3.4 手册栏目与步骤 | Task 2 `i18n.js` |
| 3.5 扫 `apps/` 部署 | Task 1 + 4 |
| 3.6 版本策略人手填 | Task 6 |
| 4.1–4.2 文件名/清单 | Task 1 |
| 7.2 命令 | Task 5 |
| 不反代 9400/9443 | Task 3 + gate 搜 `proxy_pass` |
| 不改桌面端 / 不跑 client-ref | 全计划无 Qt 文件 |

无 TBD。`deploy.py download` 是对第 7 节 `deploy.py vpn` 的补充：本条可不重部 92；`vpn` 仍会带上下载站。
