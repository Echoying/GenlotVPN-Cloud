# GenlotVPN-Proxy

服务端驱动的 VPN 同步代理桌面客户端。启动后显示 UI 并监听 HTTP；由服务端 `POST /api/v1/login` 与 `POST /api/v1/logout` 驱动 30303 连接与 OpenAPI 同步代理，**不走云端 VPN 认证**。

与 [`ruoyi-vpn-client`](../ruoyi-vpn-client/) **零代码共享**，视觉风格对齐 GenlotVPN 桌面端。

## 功能

- 启动即显示主窗口（会话日志 / 代理日志双 Tab）
- 管理 API：默认 `http://0.0.0.0:18080`
- 同步代理：默认 `http://0.0.0.0:18001`（未就绪返回 503）
- 30303 本地控制器连接链（detect → select → login → gateway）
- 日志落盘至 exe 同目录 `logs/`

## 构建

要求：**Java 无关**；**Qt 6 MSVC 64bit** + **Visual Studio C++**。

```bat
set CMAKE_PREFIX_PATH=D:\apps\Qt\6.11.1\msvc2022_64
bin\build-vpn-proxy.bat
```

打包：

```bat
bin\package-vpn-proxy.bat
```

或一步构建并打包：

```bat
bin\build-vpn-proxy.bat --package
```

## 配置

首次运行从 `config.default.json` 复制 `config.json`（与 exe 同目录）：

```json
{
  "controllerBaseUrl": "http://127.0.0.1:30303",
  "controllerAesEnabled": true,
  "proxyListenHost": "0.0.0.0",
  "proxyListenPort": 18001,
  "proxyAllowedSourceIps": ["10.27.0.92"],
  "adminListenHost": "0.0.0.0",
  "adminListenPort": 18080
}
```

| 配置项 | 说明 |
|--------|------|
| `controllerAesEnabled` | 访问本机 Agent `30303` 是否全包 AES 传输，**默认 true**（与 VPN 客户端相同 key/IV）；旧版 Agent 可设 `false` |

## API 文档

详见 [docs/PROXY_CLIENT.md](docs/PROXY_CLIENT.md)。

## 与 vpn-client 的区别

| 项 | vpn-client | vpn-proxy |
|----|------------|-----------|
| 登录方式 | 用户 UI 登录 + 云端认证 | 服务端 HTTP login |
| TCP/Protobuf | 有 | 无 |
| 代理 | 连接后可选 | 启动即监听，login 后转发 |
| Protobuf 依赖 | 有 | 无 |
