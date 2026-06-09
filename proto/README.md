# GenlotVPN Protobuf 协议

桌面客户端与 `ruoyi-vpn-auth` TCP 服务（默认端口 9443）共用本目录 `.proto` 定义。

## 帧格式

```
Magic(4) = "GVPN"
Version(1) = 0x01
Length(4, big-endian)
Body     = Envelope protobuf
```

## 安全

- 传输：TLS 1.3（生产环境必须）
- 防重放：timestamp ±60s + nonce（Redis 5 分钟 TTL）
- 完整性：登录后 Envelope.mac = HMAC-SHA256(session_key, type+timestamp+nonce+payload)

## 代码生成

- Java：`mvn compile -pl ruoyi-vpn-protocol`
- C++：见 `ruoyi-vpn-client/CMakeLists.txt`
