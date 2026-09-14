# 2026-08-19 客户端下载页

93 上独立静态站：外网 HTTPS 下载易安联 SDK 与当前 GenlotVPN 客户端，附中/英安装手册。不进管理端、不改 9443。

| 工件 | 文件 |
|------|------|
| 模块清单 | [模块拆分清单.md](./模块拆分清单.md) |
| spec | [spec.md](./spec.md)（已批准） |
| 计划 | [plan.md](./plan.md) |
| 门禁 | [质量门禁.md](./质量门禁.md) |
| 验收 | [验收报告/验收报告-2026-08-19.md](./验收报告/验收报告-2026-08-19.md)（内网 accept 通过；2026-08-19 人确认功能完成） |

## 阶段五命令（spec 第 7 节）

```bash
python pipeline/bin/gate.py download --run 2026-08-19-客户端下载页
python pipeline/bin/deploy.py vpn --run 2026-08-19-客户端下载页
python pipeline/bin/accept.py download --run 2026-08-19-客户端下载页
```

`gate` / `deploy download` / `accept download` **已跑过**（见 `质量门禁.md`）。内网地址 `https://10.27.0.93/genlotvpn/download/`。公网 443 未映时不得声称外网已通。

## 人验清单（spec 7.4 + 3.6）

| 项 | 负责人 | 做法 |
|----|--------|------|
| M2 nginx | 运维 / 开发 | 确认下载 nginx **无** 9400/9443 反代；`autoindex off`；80→443；证书只在 93、私钥不进 Git。公网 443 是否已映由网络工程师确认——**未映不得声称外网已通** |
| M3 清单与包 | 发版人 | 放入或确认 `apps/` 后部署；`manifest.json` 版本与最高 zip 一致；macOS 现有 zip 可下载 |
| 版本策略链接 | 超管 | 部署可用后，在管理端「客户端版本策略」把 Windows / macOS 下载链接都填 `https://<93公网IP>/genlotvpn/download/`（envs 公网 IP，对话脱敏 `***`）。发版不用改这条 |
