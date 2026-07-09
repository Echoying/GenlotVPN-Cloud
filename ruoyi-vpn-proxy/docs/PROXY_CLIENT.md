# GenlotVPN-Proxy 使用说明

## 概述

`GenlotVPN-Proxy` 是内网常驻桌面程序，由**服务端 HTTP 请求**驱动一次完整的 VPN 同步代理周期：

1. 程序启动 → UI 显示「待命」，同步代理端口已监听（返回 503）
2. 服务端 `POST /api/v1/login` → 客户端走 30303 连接链 → 代理开始转发
3. 服务端经代理端口访问易安联 OpenAPI
4. 服务端 `POST /api/v1/logout` → 30303 登出 → 代理恢复 503

## 默认端口

| 服务 | 默认地址 | 说明 |
|------|----------|------|
| 管理 API | `http://0.0.0.0:18080` | login / logout / probe |
| 同步代理 | `http://0.0.0.0:18001` | 转发 `/enadmin/api/open/v1/*` |
| 30303 控制器 | `http://127.0.0.1:30303` | 本地易安联 Agent |

可在 `config.json` 中修改。

### 30303 传输加密

与 [`ruoyi-vpn-client`](../ruoyi-vpn-client/) 一致：`controllerAesEnabled=true`（默认）时，对 `127.0.0.1:30303` 的请求/响应 JSON 做全包 AES-CBC 加密（Base64 传输，`Content-Type: text/plain`）。`loginWithAccount` 的 `password` 字段仍为服务端下发的字段级 AES 密文，外层再整包加密。旧版 Agent 可在 `config.json` 设 `controllerAesEnabled: false` 回退明文 JSON。

## 管理 API

### POST `/api/v1/login`

**请求体：**

```json
{
  "username": "yianlian_user",
  "password": "yianlian_password",
  "line": {
    "host": "vpn.example.com",
    "srvPort": "443",
    "spaPort": "",
    "spaKey": ""
  },
  "proxy": {
    "upstreamUrl": "https://vpn.example.com:443",
    "allowedSourceIps": ["10.27.0.92"]
  }
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `username` / `password` | 是 | 易安联平台账号 |
| `line` | 是 | 30303 选线参数（与 Web 选线页一致） |
| `proxy.upstreamUrl` | 是 | 代理转发目标根 URL |
| `proxy.allowedSourceIps` | 否 | 覆盖配置中的来源 IP 白名单；空则使用 `config.json` 默认值 |

**成功响应（200）：**

```json
{
  "code": 200,
  "msg": "连接成功",
  "data": {
    "proxyEndpoint": "http://0.0.0.0:18001",
    "tunnelStatus": 1,
    "upstreamUrl": "https://vpn.example.com:443"
  }
}
```

**常见错误：**

| HTTP | code | 说明 |
|------|------|------|
| 400 | 400 | 参数不完整 |
| 409 | 409 | 已有活跃会话或正在处理 |
| 500 | 500 | 30303 连接失败 |

### POST `/api/v1/logout`

无请求体。成功后代理清除上游并恢复 503，UI 回到「待命」。

```json
{
  "code": 200,
  "msg": "已退出，等待下次登录"
}
```

### POST `/api/v1/probe`

仅执行 30303 `detect`，不触发 login/logout，不开启同步代理转发。

**请求体：**

```json
{
  "line": {
    "host": "vpn.example.com",
    "srvPort": "443",
    "spaPort": "62201",
    "spaKey": "md5小写32位"
  }
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `line.host` | 是 | 服务器域名或 IP |
| `line.srvPort` | 是 | 服务器端口 |
| `line.spaPort` | 是 | 敲门端口 |
| `line.spaKey` | 是 | 预共享密钥 MD5（32 位小写） |

**成功响应（200）：**

```json
{
  "code": 200,
  "msg": "探测成功",
  "data": {
    "available": true
  }
}
```

**线路不可达或 30303 调用失败（仍 HTTP 200）：**

```json
{
  "code": 200,
  "msg": "无法连接本地控制器 http://127.0.0.1:30303: ...",
  "data": {
    "available": false
  }
}
```

| 场景 | HTTP | `data.available` | `msg` 示例 |
|------|------|------------------|------------|
| 探测成功 | 200 | `true` | 探测成功 |
| 线路不可达 | 200 | `false` | 线路不可用 |
| 30303 未启动/超时/解密失败 | 200 | `false` | 无法连接本地控制器… |
| 参数不完整 | 400 | — | 缺少 line.host/… |

业务探测失败**不**返回 HTTP 500，便于服务端解析 JSON 并写入具体 `probe_msg`。仅参数错误返回 400。

## 同步代理行为

- 程序启动后即监听 `proxyListenHost:proxyListenPort`
- **未 login 或 logout 后**：对任意请求返回 **503 Service Unavailable**
- **login 成功且隧道就绪后**：将请求转发到 `upstreamUrl`，路径需为 `/enadmin/api/open/v1/...`
- 来源 IP 不在白名单时拒绝（行为与 vpn-client `OpenApiProxyService` 一致）

服务端调用示例：

```
POST http://<proxy-host>:18001/enadmin/api/open/v1/user/list
```

## 日志文件

日志目录：`<exe 目录>/logs/`，按日滚动，UTF-8 编码。

| 文件 | 内容 |
|------|------|
| `genlot-vpn-proxy-YYYY-MM-DD.log` | 通用运行日志 |
| `genlot-vpn-proxy-error-YYYY-MM-DD.log` | ERROR 级别双写 |
| `genlot-vpn-proxy-session-YYYY-MM-DD.log` | 会话日志（login/logout、30303 步骤） |
| `genlot-vpn-proxy-access-YYYY-MM-DD.log` | 代理访问日志（含请求/响应摘要） |

敏感字段（密码、Authorization 等）写入前脱敏为 `***`。

UI「清空日志」仅清除内存列表，**不影响已落盘文件**。

## 单实例

同一机器仅允许运行一个实例；重复启动会提示「已在运行中」并退出。

## 部署建议

1. 在 VPN 用户机器或跳板机上安装并常驻运行
2. 确保本地 30303 Agent 已启动
3. 服务端通过内网 HTTP 调用管理 API 与代理端口
4. 防火墙放行 `adminListenPort` 与 `proxyListenPort`（仅内网）

## 故障排查

| 现象 | 可能原因 |
|------|----------|
| 管理 API 启动失败 | 端口被占用，修改 `adminListenPort` |
| login 返回 500 | 30303 未运行、线路参数错误或账号无效 |
| 代理 503 | 未 login 或已 logout |
| 代理连接被拒 | 来源 IP 不在 `allowedSourceIps` 白名单 |

查看 `logs/` 下 session 与 access 日志获取详细步骤与 HTTP 原文。
