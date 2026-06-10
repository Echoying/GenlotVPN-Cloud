# 三机拆分部署说明

将原单机部署按机器物理拆分为三个子目录 `node-91` / `node-92` / `node-93`，分别对应三台生产服务器。采用 **host 网络模式** + `extra_hosts` 别名解析，**无需改动** 各服务 `bootstrap.yml` 和 Nacos 配置中心内容。

## 一、目录结构

```
docker/
  copy.sh / copy.py            # 构建机用：把 jar/前端 dist/sql 分发到各 node-9x 子目录
  README_DEPLOY_3NODES.md

  node-91/                     # 中间件机 10.27.0.91
    .env  deploy.sh  docker-compose.yml
    mysql/  redis/  nacos/

  node-92/                     # 管理端机 10.27.0.92
    .env  deploy.sh  docker-compose.yml
    nginx/
    ruoyi/{gateway, auth, visual/monitor,
           modules/{system, gen, job, file, yianlian}}/

  node-93/                     # VPN 机 10.27.0.93
    .env  deploy.sh  docker-compose.yml
    ruoyi/vpn/{gateway, auth/certs, nginx}/
```

## 二、机器与服务分配

| 机器 | IP | 子目录 | 服务 | 端口 |
|------|----|--------|------|------|
| 中间件机 | 10.27.0.91 | node-91 | ruoyi-mysql / ruoyi-redis / ruoyi-nacos | 3306 / 6379 / 8848,9848,9849 |
| 管理端机 | 10.27.0.92 | node-92 | gateway / auth / system / gen / job / file / monitor / yianlian / nginx | 8080 / 9200 / 9201 / 9202 / 9203 / 9300 / 9100 / 9205 / 80 |
| VPN 机 | 10.27.0.93 | node-93 | vpn-gateway / vpn-auth / vpn-nginx | 8090 / 9400,9443 / 8060 |

每个 `node-9x` 目录内含独立的 `.env`（三台 IP 变量）、`deploy.sh`（本机操作脚本）、`docker-compose.yml`。

## 三、核心机制

1. **别名解析**：node-92 / node-93 的 compose 通过 `extra_hosts` 把 `ruoyi-nacos`、`ruoyi-redis`、`ruoyi-mysql` 解析到中间件机 IP，因此从 Nacos 拉取的 `ry_config` 配置（里面写的是别名）无需修改即可跨机连通。
2. **服务注册 IP**：每个微服务通过 `SPRING_CLOUD_NACOS_DISCOVERY_IP=<本机IP>` 显式指定注册到 Nacos 的 IP，确保跨主机 `lb://` 与 Feign 调用可达（避免多网卡选错或注册成容器内网 IP）。
3. **host 网络**：所有容器使用 `network_mode: host`，端口直接占用宿主机，`extra_hosts` 仍写入容器 `/etc/hosts`。

## 四、构建与分发（构建机）

1. 在源码机编译后，在 `docker/` 目录执行 `sh copy.sh`（或 `python copy.py`），将 jar / 前端 dist / sql 拷贝到 `node-9x` 对应位置。
2. 将对应的 `node-9x` 目录分发到对应机器（也可整目录分发，各机只用自己的子目录）。

## 五、防火墙端口（跨机必开）

| 机器 | 入站放通 | 来源 |
|------|----------|------|
| 91 | 3306, 6379, 8848, 9848, 9849 | 92 / 93 |
| 92 | 80, 8080, 9100, 9200, 9201, 9202, 9203, 9205, 9300 | 93 / 客户端 |
| 93 | 8060, 8090, 9400, 9443 | 92 / 客户端 |

> 注意：Nacos 的 gRPC 端口 `9848/9849` 必须放通，否则 2.x 客户端注册/心跳失败。

各机进入自己的 `node-9x` 目录执行 `sh deploy.sh port` 即可开放本机端口。

## 六、部署步骤

各台机器进入自己的 `node-9x` 目录操作，确认 `.env` 中 IP 正确。

1. **先起 91**（等待 MySQL 初始化 `ry-cloud` / `ry-config`、Nacos 健康）：

```bash
cd node-91 && sh deploy.sh up
```

2. **再起 92**：

```bash
cd node-92 && sh deploy.sh up
```

3. **最后起 93**：

```bash
cd node-93 && sh deploy.sh up
```

## 七、验证

1. 打开 Nacos 控制台 `http://10.27.0.91:8848/nacos`，在「服务管理 → 服务列表」中确认各微服务实例 IP 为 `10.27.0.92` / `10.27.0.93`（**不能是 172.x 容器内网 IP**）。
2. 访问管理端 `http://10.27.0.92`、VPN 用户端 `http://10.27.0.93:8060` 联调。
3. VPN 桌面客户端连接 `10.27.0.93:9443`（TLS/TCP）。

## 八、停止 / 删除

```bash
# 在对应 node-9x 目录内
sh deploy.sh stop   # 停止本机服务
sh deploy.sh rm     # 删除本机容器
```

## 九、注意事项

- host 模式下 compose 中的 `ports:` 不生效，已全部移除；需保证同机端口无冲突（当前各服务端口唯一）。
- node-93 的 `ruoyi-vpn-nginx` 使用本目录 `ruoyi/vpn/nginx/dockerfile` 构建（`build.context: ./ruoyi/vpn/nginx`）。
- `ry_config` 中 `ruoyi-file-dev.yml` 的 `file.domain=http://127.0.0.1:9300` 等为既有配置；若跨机访问文件服务异常，在 Nacos 中改为 `http://10.27.0.92:9300`。
