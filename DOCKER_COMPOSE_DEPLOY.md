# Docker Compose 部署指南（springboot2）

本文档基于当前仓库实际配置整理，适用于 `springboot2` 分支。

## 1. 前置条件

- 已安装 Docker 与 Docker Compose
- 服务器端口开放（按需）：
  - `80, 8080, 8848, 9848, 9849, 3306, 6379, 9100, 9200, 9201, 9202, 9203, 9300`
- 在项目根目录执行命令

---

## 2. 准备部署产物（本地）

`docker-compose` 中 Java 服务镜像依赖本地 jar，Nginx 依赖前端 dist 文件。建议固定按“前端 → 后端 → copy”顺序执行。

### 2.1 前端打包（先执行）

```bash
cd ruoyi-ui
npm install
npm run build:prod
cd ..
```

### 2.2 后端打包

在项目根目录执行：

```bash
mvn clean package -DskipTests
```

### 2.3 复制产物到 docker 目录

```bash
cd docker
sh copy.sh
```

`copy.sh` 会执行：
- 复制 SQL 到 `docker/mysql/db`
- 复制前端静态资源到 `docker/nginx/html/dist`
- 复制各微服务 jar 到 `docker/ruoyi/**/jar`

---

## 3. 上传到 10.9.2.177（自动打包 + 上传 + 解压）

在项目根目录执行：

```bash
python bin/upload_and_extract_docker.py --password "<服务器密码>"
```

该脚本会自动完成：
- 打包本地 `docker` 目录
- 上传到 `/root/GenlotVPN-Cloud`
- 远端解压替换 `docker` 目录

---

## 4. 在 10.9.2.177 分阶段重启服务（推荐流程）

进入服务器项目目录：

```bash
cd /root/GenlotVPN-Cloud/docker
```

### 4.1 先停业务

```bash
sh deploy.sh stop
```

> 建议先用 `docker compose ps` 确认业务容器已停止或退出，再继续下一步。

### 4.2 先启动基础服务

```bash
sh deploy.sh base
```

基础服务为：`ruoyi-mysql`、`ruoyi-redis`、`ruoyi-nacos`。

### 4.3 基础服务正常后，再启动业务服务

```bash
sh deploy.sh modules
```

`modules` 默认包含：`ruoyi-nginx`、`ruoyi-gateway`、`ruoyi-auth`、`ruoyi-modules-system`。

---

## 5. 验证部署结果

```bash
docker compose ps
docker compose logs -f ruoyi-nacos
docker compose logs -f ruoyi-gateway
docker compose logs -f ruoyi-auth
docker compose logs -f ruoyi-modules-system
```

建议重点确认：
- `base` 相关容器均为 `Up`
- `modules` 相关容器均为 `Up`
- 网关与认证服务无持续报错
- 前端可访问：`http://<服务器IP>/`
- Nacos 可访问：`http://<服务器IP>:8848/nacos`

---

## 6. 关键配置检查

### 6.1 Nacos 的 MySQL 连接

文件：`docker/nacos/conf/application.properties`

重点检查：
- `db.url.0`
- `db.user`
- `db.password`

如果你使用 compose 内置 MySQL，建议将 host 配成容器名 `ruoyi-mysql`（而不是外部 IP）。

### 6.2 MySQL Root 密码一致性

文件：`docker/docker-compose.yml`

检查 `MYSQL_ROOT_PASSWORD` 与 Nacos 配置中的数据库密码一致。

### 6.3 服务访问 Nacos 的地址

各微服务 `bootstrap.yml` 默认多为 `127.0.0.1:8848`。容器部署时，通常应改为 `ruoyi-nacos:8848`（可在 Nacos 配置中心统一维护）。

---

## 7. 常用运维命令

```bash
# 停止
docker compose stop

# 停止并删除容器网络（默认不删命名卷）
docker compose down

# 重新构建并启动
docker compose up -d --build
```

脚本方式：

```bash
sh deploy.sh stop
sh deploy.sh rm
```

---

## 8. 常见问题排查

1. **容器启动失败：找不到 jar 或 dist**
   - 先执行后端/前端构建，再执行 `sh copy.sh`。

2. **服务连不上 Nacos**
   - 检查服务内配置是否仍是 `127.0.0.1:8848`。
   - 容器内通常应使用 `ruoyi-nacos:8848`。

3. **Nacos 无法连接数据库**
   - 检查 `application.properties` 的 `db.url.0`、账号密码与 `docker-compose.yml` 是否一致。

4. **数据库初始化不完整**
   - 检查 `docker/mysql/db` 中是否有初始化 SQL（由 `copy.sh` 复制）。
