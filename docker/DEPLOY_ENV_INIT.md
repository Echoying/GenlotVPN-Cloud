# GenlotVPN-Cloud 三机生产部署 - 环境初始化手册

可复制粘贴的命令手册。目标：把 3 台全新服务器初始化到可运行本系统的状态，并完成部署。

## 0. 基本信息


| 角色    | IP         | 子目录       | 部署服务                                                                    |
| ----- | ---------- | --------- | ----------------------------------------------------------------------- |
| 中间件机  | 10.27.0.91 | `node-91` | MySQL / Redis / Nacos                                                   |
| 管理端机  | 10.27.0.92 | `node-92` | gateway / auth / system / gen / job / file / monitor / yianlian / nginx |
| VPN 机 | 10.27.0.93 | `node-93` | vpn-auth（9400 / 9443） |


- 登录账号：`root`（密码由运维单独保管，勿写入文档或代码库）
- 三台均使用 **host 网络模式**，跨机通过 `extra_hosts` 别名解析 + `SPRING_CLOUD_NACOS_DISCOVERY_IP` 注册本机 IP。

### 操作系统

三台为同一基础版本：**Rocky Linux 9.8 (Blue Onyx)**，`platform:el9`，RHEL 9 兼容（`ID_LIKE="rhel centos fedora"`）。本文所有命令按 **EL9** 编写：包管理用 `dnf`，防火墙用 `firewalld`/`firewall-cmd`，服务管理用 `systemctl`。

```text
NAME="Rocky Linux"
VERSION="9.8 (Blue Onyx)"
ID="rocky"        ID_LIKE="rhel centos fedora"
VERSION_ID="9.8"  PLATFORM_ID="platform:el9"
CPE_NAME="cpe:/o:rocky:rocky:9::baseos"
```

> 校验当前系统：`cat /etc/os-release`。若版本不符，相应调整换源 URL 与包名。

### 硬件资源（三台规格一致）

以 `ztna1`（10.27.0.91）实测为准，92 / 93 同为该基础版本。


| 项目   | 规格                                       |
| ---- | ---------------------------------------- |
| CPU  | 4 核（`%Cpu0` ~ `%Cpu3`）                   |
| 内存   | 约 **15 GiB**（15732 MiB total，可用约 14 GiB） |
| Swap | **8 GiB**（当前未使用）                         |
| 负载   | 空闲（load average 约 0.05 / 0.01 / 0.00）    |


```text
# free -h
              total        used        free      shared  buff/cache   available
Mem:            15Gi       581Mi        14Gi       8.0Mi       284Mi        14Gi
Swap:          8.0Gi          0B       8.0Gi
```

> 校验：`free -h`、`nproc`、`uptime`。

**已落地的资源调优（仓库内已配置，重建镜像/容器后生效）：**


| 节点  | 调优项                        | 配置                                                         |
| --- | -------------------------- | ---------------------------------------------------------- |
| 91  | MySQL InnoDB 缓冲池           | `innodb-buffer-pool-size=1G`（`node-91/docker-compose.yml`） |
| 92  | 8 个 Java 微服务 JVM           | `-Xms256m -Xmx512m`（各服务 `dockerfile`）                      |
| 93  | vpn-auth JVM | `-Xms256m -Xmx512m`（`dockerfile`）                      |


> 92 上 8×512M ≈ 4G 堆上限，加系统与 nginx 后 15G 内存较稳妥。若上线后某服务频繁 Full GC，可单独将该服务 `-Xmx` 调至 768m，并观察 `free -h`。

### 磁盘分区


| 挂载点         | 设备                           | 容量       | 已用    | 可用        | 用途建议                             |
| ----------- | ---------------------------- | -------- | ----- | --------- | -------------------------------- |
| `/`         | `/dev/mapper/rl-root`        | 50G      | ~4G   | ~46G      | 系统与软件                            |
| `/boot`     | `/dev/sda2`                  | 960M     | 498M  | 463M      | 引导                               |
| `/boot/efi` | `/dev/sda1`                  | 599M     | 7.7M  | 592M      | EFI                              |
| `**/data`** | `/dev/mapper/datavg-lv_data` | **200G** | ~1.5G | **~199G** | **部署目录、MySQL 数据、上传文件、Docker 镜像** |


```text
# df -h（节选）
Filesystem                  Size  Used Avail Use% Mounted on
/dev/mapper/rl-root          50G  4.1G   46G   9% /
/dev/sda2                   960M  498M  463M  52% /boot
/dev/sda1                   599M  7.7M  592M   2% /boot/efi
/dev/mapper/datavg-lv_data  200G  1.5G  199G   1% /data
```

> 根分区仅 50G，**本文部署目录统一使用 `/data/genlotvpn`**，避免 MySQL / Docker 镜像占满系统盘。校验：`df -h`。

---

## 1. 登录服务器

在本地终端分别登录（每台一个窗口）：

```bash
ssh root@10.27.0.91
ssh root@10.27.0.92
ssh root@10.27.0.93
```

> 可选：若本地装有 `sshpass`，可免交互登录（密码含特殊字符时用单引号包裹）：
>
> ```bash
> sshpass -p '<your-password>' ssh -o StrictHostKeyChecking=no root@10.27.0.91
> ```

---

## 2. 基础环境初始化（三台都要执行，命令完全相同）

> 下面整段可一次性粘贴到 **每一台** 服务器上执行。

### 2.1 配置 dnf/yum 软件源（最先执行）

> Rocky Linux 9 默认源在国内拉取较慢，先换成阿里云镜像。Rocky 9 的换源方式是把官方 repo 文件里的 `mirrorlist`/默认 `baseurl` 替换为阿里云地址（不是下载单独的 repo 文件）。

```bash
# 把 Rocky 9 官方 repo 指向阿里云镜像
sed -e 's|^mirrorlist=|#mirrorlist=|g' \
    -e 's|^#baseurl=http://dl.rockylinux.org/$contentdir|baseurl=https://mirrors.aliyun.com/rockylinux|g' \
    -i.bak /etc/yum.repos.d/[Rr]ocky*.repo

# EPEL（提供 sshpass、chrony 等额外软件包）
dnf install -y epel-release || true
# 将 EPEL 也指向阿里云（可选，加速）
sed -e 's|^metalink=|#metalink=|g' \
    -e 's|^#baseurl=https\?://download.example/pub/epel|baseurl=https://mirrors.aliyun.com/epel|g' \
    -e 's|^#baseurl=https\?://download.fedoraproject.org/pub/epel|baseurl=https://mirrors.aliyun.com/epel|g' \
    -i.bak /etc/yum.repos.d/epel*.repo 2>/dev/null || true

# 重建缓存
dnf clean all
dnf makecache
dnf repolist
```

> - 内网无外网出口时：把 `mirrors.aliyun.com` 换成内网镜像站地址，或挂载系统 ISO 配置本地源：
>   ```bash
>   mount /dev/cdrom /mnt
>   cat > /etc/yum.repos.d/local.repo <<'EOF'
>   [local-baseos]
>   name=local-baseos
>   baseurl=file:///mnt/BaseOS
>   enabled=1
>   gpgcheck=0
>   [local-appstream]
>   name=local-appstream
>   baseurl=file:///mnt/AppStream
>   enabled=1
>   gpgcheck=0
>   EOF
>   dnf clean all && dnf makecache
>   ```
> - 在 Rocky 9 上 `yum` 是 `dnf` 的软链接，本文用 `dnf`，写 `yum` 亦可。

### 2.2 时区与时间同步（关键：Nacos 心跳/令牌依赖时间一致）

```bash
dnf install -y chrony
timedatectl set-timezone Asia/Shanghai
systemctl enable --now chronyd
chronyc -a makestep || true
date
```

### 2.2.1 安装 rz/sz（lrzsz，三台都要）

> 用于 SSH 终端（Xshell、SecureCRT、FinalShell 等）向服务器**上传/下载**文件。Rocky 9 包名为 `lrzsz`，提供 `rz`（接收）、`sz`（发送）命令。

```bash
dnf install -y lrzsz
which rz sz
rz --version
```

上传文件到当前目录：在终端执行 `rz`（客户端会弹出文件选择框）。  
下载文件到本地：在服务器执行 `sz <文件名>`。

### 2.3 关闭 SELinux

```bash
setenforce 0 2>/dev/null || true
sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config
getenforce
```

### 2.4 内核与资源参数（MySQL / Redis 需要）

```bash
# 句柄数
cat >> /etc/security/limits.conf <<'EOF'
* soft nofile 655350
* hard nofile 655350
EOF

# 内核参数
cat > /etc/sysctl.d/99-genlotvpn.conf <<'EOF'
vm.overcommit_memory = 1
vm.max_map_count = 262144
net.core.somaxconn = 1024
net.ipv4.tcp_max_syn_backlog = 1024
EOF
sysctl --system

# 关闭透明大页（Redis 建议）。EL9 默认不启用 rc.local，用 systemd 持久化
echo never > /sys/kernel/mm/transparent_hugepage/enabled 2>/dev/null || true
echo never > /sys/kernel/mm/transparent_hugepage/defrag   2>/dev/null || true
cat > /etc/systemd/system/disable-thp.service <<'EOF'
[Unit]
Description=Disable Transparent Huge Pages
After=local-fs.target

[Service]
Type=oneshot
ExecStart=/bin/sh -c 'echo never > /sys/kernel/mm/transparent_hugepage/enabled; echo never > /sys/kernel/mm/transparent_hugepage/defrag'

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable --now disable-thp.service
```

### 2.5 安装 Docker CE + docker-compose

```bash
# 卸载旧版本（如有）
dnf remove -y docker docker-common docker-selinux docker-engine podman runc 2>/dev/null || true

# 安装 Docker CE（阿里云源，el9；内网若有私有源请替换）
dnf install -y dnf-plugins-core
dnf config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
# 阿里云该 repo 默认指向 $releasever；el9 已提供对应目录，可直接安装
dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
systemctl enable --now docker
docker --version
```

> Rocky 9 自带 `podman`，与 Docker 冲突，上面已一并卸载。若 `dnf config-manager` 提示找不到命令，先 `dnf install -y dnf-plugins-core`。

```bash
# 清理曾 curl 下载失败的假二进制
rm -f /usr/local/bin/docker-compose docker-compose

# 安装 docker-compose 命令（包装 dnf 自带的 compose 插件，无需从 GitHub 下载）
cat > /usr/bin/docker-compose <<'EOF'
#!/bin/sh
exec docker compose "$@"
EOF
chmod +x /usr/bin/docker-compose
hash -r    # 刷新 bash 命令缓存（删过旧文件后必做，否则会找 /usr/local/bin/docker-compose）

docker-compose version
```

> `deploy.sh` 使用 **`docker-compose`** 命令。上述脚本将 `docker-compose` 转发到 `docker compose` 插件，内网无需访问 GitHub。
>
> 若报错 `docker: 'compose' is not a docker command`，先执行：
> `dnf install -y docker-compose-plugin && systemctl restart docker`，再重新创建包装脚本。

### 2.6 配置 Docker 镜像加速 + 日志限制

> 系统需要拉取 `mysql:5.7`、`nacos/nacos-server:v2.4.2`、`redis`、`nginx`、`eclipse-temurin:8-jre` 等基础镜像。

```bash
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ],
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" }
}
EOF
systemctl daemon-reload
systemctl restart docker
```

> 若为完全离线环境，请改为配置内网私有 registry，或在有网机器 `docker save` 导出后 `docker load` 到三台。

---

## 3. 防火墙放通（按机器分别执行）

跨机端口必须放通；下表为各机入站端口（来源为另两台 / 客户端）。

部署目录上传后（见第 4 步），可直接进入对应目录执行 `sh deploy.sh port` 自动放通；也可手动执行下列命令。

> 先确保 firewalld 已启用（Rocky 9 默认安装）：`systemctl enable --now firewalld`。`deploy.sh port` 末尾使用 `firewall-cmd --reload` 生效（已适配 EL9，不再用 `service`）。

**91（中间件机）：**

```bash
firewall-cmd --add-port=3306/tcp --permanent   # MySQL
firewall-cmd --add-port=6379/tcp --permanent   # Redis
firewall-cmd --add-port=8848/tcp --permanent   # Nacos http
firewall-cmd --add-port=9848/tcp --permanent   # Nacos gRPC（2.x 必开）
firewall-cmd --add-port=9849/tcp --permanent   # Nacos gRPC
firewall-cmd --reload
```

**92（管理端机）：**

```bash
for p in 80 8080 9100 9200 9201 9202 9203 9205 9300; do firewall-cmd --add-port=${p}/tcp --permanent; done
firewall-cmd --reload
```

**93（VPN 机）：**

```bash
for p in 9400 9443; do firewall-cmd --add-port=${p}/tcp --permanent; done
firewall-cmd --reload
```

> 若三台处于完全可信内网，也可直接关闭防火墙（二选一）：
> `systemctl stop firewalld && systemctl disable firewalld`

---

## 4. 上传部署文件

部署产物（jar / 前端 dist / sql）需先在**构建机**用 `docker/copy.sh`（或 `python copy_artifacts.py`）分发到 `node-91/92/93` 子目录，再上传到对应服务器。

### 4.1 构建要求（重要）

本项目 [pom.xml](../pom.xml) 指定 **`java.version=1.8`**，Docker 运行时镜像为 **`eclipse-temurin:8-jre`**。构建前必须确认：

```bash
java -version    # 必须显示 1.8.x
mvn -version     # Java version 也必须是 1.8
```

完整编译：

```bash
mvn clean package -DskipTests
```

若部分模块曾用 JDK 17 单独编译，会导致 `UnsupportedClassVersionError: class file version 61.0`。`copy.sh` 末尾会自动校验所有 JAR 的 class major version 必须为 **52**（Java 8），非 52 将报错退出。

在**构建机**（本仓库 `docker/` 目录）执行：

```bash
# 1) 确保已用 JDK 8 编译出各 jar 与前端 dist，然后分发到 node-9x
cd docker
sh copy.sh            # 或：python copy_artifacts.py

# 2) 上传到三台服务器的 /data/genlotvpn/ 下
ssh root@10.27.0.91 'mkdir -p /data/genlotvpn'
ssh root@10.27.0.92 'mkdir -p /data/genlotvpn'
ssh root@10.27.0.93 'mkdir -p /data/genlotvpn'
scp -r node-91 root@10.27.0.91:/data/genlotvpn/
scp -r node-92 root@10.27.0.92:/data/genlotvpn/
scp -r node-93 root@10.27.0.93:/data/genlotvpn/
```

> 各机只需自己的子目录。确认对应 `node-9x/.env` 中 IP 正确：
> `MIDDLEWARE_HOST=10.27.0.91 / APP_HOST=10.27.0.92 / VPN_HOST=10.27.0.93`。

---

## 5. 启动服务（严格按 91 → 92 → 93 顺序）

### 5.1 在 91 启动中间件，并等待 MySQL 初始化、Nacos 健康

```bash
cd /data/genlotvpn/node-91
sh deploy.sh port        # 放通端口（已手动放通可跳过）
sh deploy.sh up          # 启动 mysql/redis/nacos

# 观察初始化（首次启动 MySQL 会执行 sql 初始化，约 30~60s）
docker ps
docker logs -f ruoyi-mysql   # 看到 ready for connections 即可 Ctrl-C
curl -s http://10.27.0.91:8848/nacos/ -o /dev/null -w "nacos http=%{http_code}\n"
```

### 5.2 在 92 启动管理端

```bash
cd /data/genlotvpn/node-92
sh deploy.sh port
sh deploy.sh up
docker ps
```

### 5.3 在 93 启动 VPN 服务

```bash
cd /data/genlotvpn/node-93
sh deploy.sh port
sh deploy.sh up
docker ps
```

---

## 6. 部署验证

```bash
# 1) Nacos 服务列表（确认实例 IP 是 10.27.0.92 / 93，而非 172.x 容器内网）
#    浏览器打开：http://10.27.0.91:8848/nacos  （账号见 ry-config 中 nacos 配置）

# 2) 各机容器状态
ssh root@10.27.0.91 'docker ps --format "{{.Names}}\t{{.Status}}"'
ssh root@10.27.0.92 'docker ps --format "{{.Names}}\t{{.Status}}"'
ssh root@10.27.0.93 'docker ps --format "{{.Names}}\t{{.Status}}"'

# 3) 接口连通性
curl -s -o /dev/null -w "管理端=%{http_code}\n" http://10.27.0.92/
# VPN 桌面客户端连接 10.27.0.93:9443（TLS/TCP）；HTTP 9400 为服务内部端口
```

- 管理端：`http://10.27.0.92`（默认管理员 `admin`，初始密码见系统说明）
- VPN 桌面客户端：连接 `10.27.0.93:9443`（TLS/TCP）

---

## 7. 停止 / 删除 / 重启

```bash
# 在对应 node-9x 目录内
sh deploy.sh stop     # 停止本机服务
sh deploy.sh rm       # 删除本机容器
sh deploy.sh up       # 重新构建并启动
```

---

## 8. 挂载目录权限说明

用 `scp`/`rz` 上传部署目录时，宿主机上的数据/日志目录常以 **root:root 755** 创建，而容器内部分服务以**非 root** 运行，会导致写失败。

| 机器 | 目录 | 容器用户 | 风险 | 影响 |
|------|------|----------|------|------|
| 91 | `redis/data` | redis(999) | **高** | Redis 无法持久化，触发 MISCONF，auth 登录失败 |
| 91 | `nacos/logs` | nacos(1000) | 中 | Nacos 日志写失败，严重时影响启动 |
| 91 | `mysql/data` | mysql | 低 | 官方 entrypoint 通常会自行 `chown` |
| 91 | `mysql/logs` | mysql | 中 | 自定义日志配置时可能写失败 |
| 92 | `nginx/logs` | nginx(~101) | 中 | 访问日志/错误日志写失败 |
| 92 | `ruoyi/uploadPath` | root(Java) | 低 | 文件上传服务写文件失败 |
| 92 | `ruoyi/**/logs` | root(Java) | 低 | 应用日志写失败（一般不影响业务） |
| 93 | `ruoyi/vpn/**/logs` | root(Java) | 低 | VPN 服务日志写失败 |

三台 `deploy.sh up` 已内置 `prep()` 自动 `mkdir` 并放宽权限。若已部署，可手动一次性修复：

```bash
# ztna1
cd /data/genlotvpn/node-91
mkdir -p redis/data redis/logs mysql/data mysql/logs nacos/logs
chmod -R 777 redis/data redis/logs nacos/logs mysql/logs 2>/dev/null || true
chown 999:999 redis/data 2>/dev/null || true
chown 1000:1000 nacos/logs 2>/dev/null || true
docker-compose restart ruoyi-redis ruoyi-nacos

# ztna2
cd /data/genlotvpn/node-92
mkdir -p nginx/logs ruoyi/uploadPath
find ruoyi -type d -name logs -exec mkdir -p {} \;
chmod -R 777 nginx/logs ruoyi/uploadPath
find ruoyi -type d -name logs -exec chmod 777 {} \;
docker-compose restart ruoyi-nginx

# ztna3
cd /data/genlotvpn/node-93
find ruoyi/vpn -type d -name logs -exec mkdir -p {} \;
find ruoyi/vpn -type d -name logs -exec chmod 777 {} \;
docker-compose restart ruoyi-vpn-auth
```

---

## 9. 常见问题

- **Nacos 注册成 172.x 内网 IP**：确认 compose 中 `SPRING_CLOUD_NACOS_DISCOVERY_IP=${本机IP}` 生效、`.env` IP 正确。
- **92/93 连不上 MySQL/Redis/Nacos**：检查 91 防火墙是否放通 3306/6379/8848/**9848/9849**（gRPC 必开），以及 91 是否先于 92/93 启动完成。
- **MySQL 未初始化业务库**：仅首次启动（`./mysql/data` 为空）才会执行 `mysql/db/*.sql`；重置需 `sh deploy.sh rm` 并清空 `node-91/mysql/data` 后重启。
- **拉取基础镜像失败**：检查 `/etc/docker/daemon.json` 镜像加速；离线环境改用私有 registry 或 `docker load`。
- **文件服务跨机访问异常**：`ry-config` 中 `ruoyi-file-dev.yml` 的 `file.domain` 已设为 `http://10.27.0.92:9300`、`file.path=/home/ruoyi/uploadPath`。
- `**docker-compose: line 1: status:403: command not found`**：曾从 GitHub curl 到 403 错误页并存成假二进制。删除后用包装脚本即可：
  ```bash
  rm -f /usr/local/bin/docker-compose /usr/bin/docker-compose
  dnf install -y docker-compose-plugin
  systemctl restart docker
  cat > /usr/bin/docker-compose <<'EOF'
  #!/bin/sh
  exec docker compose "$@"
  EOF
  chmod +x /usr/bin/docker-compose
  hash -r                        # 刷新 bash 命令缓存（重要）
  docker-compose version
  ```
- **`docker-compose: No such file or directory`**（已创建 `/usr/bin/docker-compose` 仍报错）：当前 shell 仍缓存了已删除的 `/usr/local/bin/docker-compose` 路径。执行 `hash -r` 后重试，或新开一个终端，或直接用 `/usr/bin/docker-compose version`。
- **`deploy.sh: line 2: $'\r': command not found`**：脚本为 Windows CRLF 换行，Linux 无法解析。仓库内 `*.sh` 已统一为 LF；若服务器上仍是旧文件，在对应目录执行：
  ```bash
  sed -i 's/\r$//' deploy.sh
  # 或批量：find . -name '*.sh' -exec sed -i 's/\r$//' {} \;
  sh deploy.sh up
  ```
  从 Windows 上传时建议用 `git clone`/`scp`，或安装 `dos2unix` 后 `dos2unix deploy.sh`。
- **`invalid IP address in add-host: ""`**：缺少 `node-91/.env` 或 `MIDDLEWARE_HOST` 为空。91 机 compose 已改为 `ruoyi-mysql:127.0.0.1`；92/93 仍需 `.env` 中 `MIDDLEWARE_HOST=10.27.0.91`。
- **Redis 未启动（`docker-compose ps` 无 ruoyi-redis）**：多为 `./redis/data` 挂载目录权限不足或 `logfile` 无法写入。查看日志：
  ```bash
  docker ps -a | grep redis
  docker-compose logs ruoyi-redis
  # 临时修复后重启
  mkdir -p redis/data redis/logs && chmod 777 redis/data redis/logs
  docker-compose up -d ruoyi-redis
  ```
- **auth/网关报 `MISCONF Redis is configured to save RDB snapshots... stop-writes-on-bgsave-error`**：91 机 Redis 无法写入 `./redis/data`（权限或磁盘满），所有写 Redis 的操作（登录/token）都会失败。在 **ztna1** 执行：
  ```bash
  cd /data/genlotvpn/node-91
  df -h .                          # 确认磁盘未满
  mkdir -p redis/data && chmod 777 redis/data
  chown 999:999 redis/data 2>/dev/null || true
  docker-compose restart ruoyi-redis
  redis-cli -h 127.0.0.1 ping      # PONG
  redis-cli -h 127.0.0.1 BGSAVE    # 应返回 OK
  ```
  若仍报错，查看 `docker-compose logs ruoyi-redis`。仓库 `redis.conf` 已设 `stop-writes-on-bgsave-error no`，同步新配置后 `docker-compose up -d --build ruoyi-redis`。
- **`UnsupportedClassVersionError: class file version 61.0`**：JAR 用 JDK 17 编译，但 Docker 容器是 JRE 8。在构建机用 **JDK 8** 重新编译后分发上传：
  ```bash
  # 构建机（JAVA_HOME 必须为 JDK 8）
  mvn clean package -pl ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-file,ruoyi-modules/ruoyi-gen,ruoyi-visual/ruoyi-monitor -am -DskipTests
  cd docker && sh copy.sh
  # 上传到 92 机后重建受影响服务
  scp node-92/ruoyi/modules/system/jar/ruoyi-modules-system.jar root@10.27.0.92:/data/genlotvpn/node-92/ruoyi/modules/system/jar/
  scp node-92/ruoyi/modules/file/jar/ruoyi-modules-file.jar root@10.27.0.92:/data/genlotvpn/node-92/ruoyi/modules/file/jar/
  scp node-92/ruoyi/modules/gen/jar/ruoyi-modules-gen.jar root@10.27.0.92:/data/genlotvpn/node-92/ruoyi/modules/gen/jar/
  scp node-92/ruoyi/visual/monitor/jar/ruoyi-visual-monitor.jar root@10.27.0.92:/data/genlotvpn/node-92/ruoyi/visual/monitor/jar/
  # ztna2 上
  cd /data/genlotvpn/node-92
  docker-compose up -d --build ruoyi-modules-system ruoyi-modules-file ruoyi-modules-gen ruoyi-visual-monitor
  docker-compose logs --tail 30 ruoyi-modules-system
  ```

---

## 附录 A：一键基础初始化脚本（Rocky Linux 9，三台通用）

把第 2 节整合为一个脚本，三台各跑一次：

```bash
cat > /tmp/init-base.sh <<'SH'
#!/bin/sh
set -e
# 0) 换源到阿里云（Rocky 9）
sed -e 's|^mirrorlist=|#mirrorlist=|g' \
    -e 's|^#baseurl=http://dl.rockylinux.org/$contentdir|baseurl=https://mirrors.aliyun.com/rockylinux|g' \
    -i.bak /etc/yum.repos.d/[Rr]ocky*.repo
dnf clean all && dnf makecache
# 1) 基础组件
dnf install -y chrony epel-release lrzsz
timedatectl set-timezone Asia/Shanghai
systemctl enable --now chronyd; chronyc -a makestep || true
# 2) SELinux
setenforce 0 2>/dev/null || true
sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config
# 3) 内核/资源参数
cat > /etc/sysctl.d/99-genlotvpn.conf <<'EOF'
vm.overcommit_memory = 1
vm.max_map_count = 262144
net.core.somaxconn = 1024
EOF
sysctl --system >/dev/null
echo never > /sys/kernel/mm/transparent_hugepage/enabled 2>/dev/null || true
cat > /etc/systemd/system/disable-thp.service <<'EOF'
[Unit]
Description=Disable Transparent Huge Pages
After=local-fs.target
[Service]
Type=oneshot
ExecStart=/bin/sh -c 'echo never > /sys/kernel/mm/transparent_hugepage/enabled; echo never > /sys/kernel/mm/transparent_hugepage/defrag'
[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload; systemctl enable --now disable-thp.service
# 4) Docker CE（el9）
dnf remove -y podman runc 2>/dev/null || true
dnf install -y dnf-plugins-core
dnf config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{ "registry-mirrors": ["https://docker.m.daocloud.io","https://hub-mirror.c.163.com"],
  "log-driver":"json-file","log-opts":{"max-size":"50m","max-file":"3"} }
EOF
systemctl enable --now docker; systemctl restart docker
# 5) docker-compose 命令（包装插件）
rm -f /usr/local/bin/docker-compose /usr/bin/docker-compose
cat > /usr/bin/docker-compose <<'EOF'
#!/bin/sh
exec docker compose "$@"
EOF
chmod +x /usr/bin/docker-compose
hash -r
docker --version; docker-compose version
echo "==== 基础环境初始化完成 ===="
SH
sh /tmp/init-base.sh
```

## 附录 B：其他系统适配要点（非本次环境）

- **CentOS 7 / EL7**：已 EOL，换源需下载 `https://mirrors.aliyun.com/repo/Centos-7.repo`；包管理用 `yum`；THP 可走 `/etc/rc.d/rc.local`。
- **Ubuntu/Debian**：包管理用 `apt`（`apt-get install -y chrony`）；Docker 用 apt 官方源；防火墙若用 `ufw`，`deploy.sh port`（firewalld）不生效，需 `ufw allow <port>/tcp` 或关闭 ufw。

