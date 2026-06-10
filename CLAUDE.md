# CLAUDE.md
## 语言规范
- 所有对话和文档尽量使用简体中文。
- 所有回答尽量使用简体中文[reference:8]。
- 代码注释尽量使用简体中文[reference:9]。

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 项目概述

GenlotVPN-Cloud 是一个基于 **RuoYi Cloud（若依微服务）v3.6.8** 构建的 VPN 管理平台。核心自定义模块是 **易安联（YianLian）**（`ruoyi-modules/ruoyi-yianlian`），它在本地管理 VPN 用户、部门、角色、服务和服务组，然后将所有变更同步到一个或多个外部易安联 VPN 平台实例。

## 技术栈

- **后端**：Java 1.8、Spring Boot 2.7.18、Spring Cloud 2021.0.9、Spring Cloud Alibaba 2021.0.6.1
- **前端**：Vue 2.6.12、Element UI 2.15.14、Vue Router、Vuex
- **基础设施**：Nacos（服务发现 + 配置中心）、Sentinel（熔断限流）、Redis、MySQL 5.7
- **构建**：Maven（后端）、npm/vue-cli（前端）
- **ORM**：MyBatis（XML mapper 文件）

## 构建命令

### 后端
```bash
# 完整构建
mvn clean package -DskipTests

# 构建单个模块及其依赖
mvn clean package -pl ruoyi-modules/ruoyi-yianlian -am -DskipTests

# Maven profile：dev（默认）、test
mvn clean package -P dev -DskipTests
```

### 前端（管理端）
```bash
cd ruoyi-ui
npm install
npm run dev          # 开发服务器，端口 80，API 代理到 10.9.2.177:80
npm run build:prod   # 生产构建
npm run build:stage  # 预发布构建
```

### 前端（VPN 用户登录端）
```bash
cd ruoyi-vpn-ui
npm install
npm run dev          # 开发服务器，端口 8060，API 代理到 10.9.2.177:8060
npm run build:prod   # 生产构建
```

### Docker 部署
```bash
cd docker
sh deploy.sh base      # 启动 MySQL、Redis、Nacos
sh deploy.sh modules   # 启动所有应用模块
sh deploy.sh stop      # 停止全部
sh deploy.sh rm        # 删除所有容器
# 单独启动：sh deploy.sh gateway|auth|system|yianlian|gen|job|file|nginx|monitor
```

`docker/copy.sh` 负责将构建好的 JAR、SQL 和前端 dist 产物拷贝到 Docker 构建上下文中。

### 本地开发（Windows）
`bin/` 目录下的批处理脚本：`package.bat`、`clean.bat`，以及各服务对应的 `run-*.bat`。

## 架构

### 请求流转

浏览器 → Nginx (:80) → `/prod-api/` → 网关 (:8080) → 下游服务

### 微服务拓扑

| 服务 | 端口 | 用途 |
|---|---|---|
| ruoyi-gateway | 8080 | 系统管理端 API 网关（Spring Cloud Gateway） |
| ruoyi-vpn-gateway | 8081 | VPN 用户端 API 网关（独立网关实例） |
| ruoyi-auth | 9200 | 认证与令牌管理（系统用户） |
| ruoyi-vpn-auth | 9400 | VPN 用户认证（使用 `ruoyi-api-yianlian`） |
| ruoyi-modules-system | 9201 | 系统管理（用户、角色、菜单） |
| ruoyi-modules-yianlian | 9205 | **VPN 业务逻辑**（核心自定义模块） |
| ruoyi-modules-gen | 9202 | 代码生成器 |
| ruoyi-modules-job | 9203 | 定时任务（Quartz） |
| ruoyi-modules-file | 9300 | 文件上传/存储 |
| ruoyi-visual-monitor | 9100 | Spring Boot Admin 监控 |
| Nacos | 8848 | 服务发现 + 配置中心 |

所有服务都注册到 Nacos（命名空间：`dev`）。应用配置存储在 Nacos 中，而非本地 `application.yml`——本地的 `bootstrap.yml` 文件仅用于配置 Nacos 连接。

### Maven 模块结构

- **ruoyi-api/**：服务间调用的 Feign 客户端接口和共享领域对象
  - `ruoyi-api-system` — 远程系统服务调用
  - `ruoyi-api-yianlian` — 用于 VPN 用户认证的 `RemoteVpnUserService`，以及共享 DTO（`VpnUser`、`VpnLoginUser`）
- **ruoyi-common/**：9 个共享库（core、redis、security、log、swagger、datascope、datasource、sensitive、seata）
- **ruoyi-modules/**：服务实现 — system、yianlian、gen、job、file
- **ruoyi-gateway/**：路由定义、过滤器、认证校验（系统管理端）
- **ruoyi-vpn-gateway/**：面向 VPN 用户的独立网关，带验证码校验
- **ruoyi-auth/**：登录、登出、令牌刷新（系统用户）
- **ruoyi-vpn-auth/**：VPN 用户认证服务
- **ruoyi-visual/**：Spring Boot Admin 监控

### VPN 用户认证流程

VPN 用户登录由 `ruoyi-vpn-auth` 处理（与 `ruoyi-auth` 中的系统管理端认证相互独立）：

1. `VpnLoginService.login()` 校验用户名/密码长度，检查 Redis IP 黑名单（`CacheConstants.SYS_LOGIN_BLACKIPLIST`）
2. 通过 Feign 调用易安联模块（`RemoteVpnUserService.getUserInfo`，使用 `SecurityConstants.INNER` 表示内部可信调用）
3. 成功后，将**明文密码**缓存到 Redis，键为 `vpn_plain_pwd:{userId}`，TTL 为 30 分钟（与令牌生命周期一致）。该密码供下游易安联控制器操作使用，因为这些操作需要再次向远程平台进行认证。
4. 修改密码（`changePassword`）和登录信息记录同样通过 `RemoteVpnUserService` 委托给易安联模块

`ruoyi-api/ruoyi-api-yianlian` 定义了 Feign 契约（`RemoteVpnUserService`）以及共享模型（`VpnLoginUser`、`VpnUserInfo`、`VpnChangePasswordRequest`），用于打通 `ruoyi-vpn-auth` 与 `ruoyi-modules-yianlian`。

### 易安联模块 — 双服务层

易安联模块采用一种独特的**双服务层**架构：

```
Controller
    ├── service/vpn/         (IVpnXxxService)       → 本地 MySQL 增删改查
    │       └── mapper/      (XxxMapper + XML)       → MyBatis
    └── service/yianlian/    (IYiAnLianXxxService)   → 远程易安联 API 同步
            └── client/      (OpenApiClient)          → RestTemplate HTTP 调用
```

**控制器**（11 个）：`VpnUserController`、`VpnDeptController`、`VpnRoleController`、`VpnServiceController`、`VpnServiceGroupController`、`LineAppController`、`YalDeptAuthController`、`YalRoleAuthController`、`YalUserAuthController`、`YiAnLianAuthorityController`、`VpnLogininforController`

**多线路同步模式** — 核心架构模式：每个写操作都遵循以下流程：
1. 通过 `IVpnXxxService` 持久化到本地数据库
2. 获取所有启用中的 `LineApp` 实例（每个代表一个易安联 VPN 设备，拥有各自的 `appId`/`appSecret`/`url`）
3. 对每个 LineApp，调用 `IYiAnLianXxxService` 将变更同步到远程平台
4. 将返回的远程 ID 存入对应的 `*YianlianMapping` 映射表

**OpenApiClient**（`client/OpenApiClient`）：基于 RestTemplate 的易安联外部 API HTTP 客户端：
- OAuth 风格的令牌管理：通过 `appId`/`appSecret` 获取令牌，按 `yianlian_token:{appId}` 的键模式缓存到 Redis
- API 基础路径：`/enadmin/api/open/v1/`
- 所有 API 路径定义在 `YiAnLianConstants` 中
- 响应反序列化为 `YiAnLianResponse<T>`

### 易安联模块 — 包结构

位于 `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/` 下：

- `controller/` — 11 个 REST 控制器（见上方列表）
- `domain/` + `domain/vo/` — 本地实体和视图对象
- `mapper/` — MyBatis mapper 接口（XML 位于 `src/main/resources/mapper/` 下）
- `service/vpn/` + `service/vpn/impl/` — 本地增删改查服务层（`IVpnXxxService`）
- `service/yianlian/` + `service/yianlian/impl/` — 远程同步服务层（`IYiAnLianXxxService`）
- `client/`、`client/YiAnLianBase/`、`client/dto/`、`client/dto/vo/` — `OpenApiClient` 及外部 API 的请求/响应 DTO
- `constant/` — `YiAnLianConstants`（API 路径、令牌缓存键）
- `config/` — 模块配置
- `utils/` — 工具类（例如对 `appSecret`/`spaKey` 进行 MD5 加密）

### 领域实体

| 实体 | 用途 |
|---|---|
| `LineApp` | VPN 线路配置 — 每个都是独立的易安联设备实例，含 `appId`、`appSecret`、`url` |
| `VpnUser` | VPN 用户（含关联远程平台的 `yianlianId`） |
| `VpnDept` | 部门层级（带 `ancestors` 的树结构） |
| `VpnRole` | VPN 用户角色 |
| `VpnService` | 可通过 VPN 访问的应用/服务 |
| `VpnServiceGroup` | 服务的树形分组 |
| `YalDeptAuth` | 部门级授权（哪些部门可访问哪些服务） |
| `YalRoleAuth` | 角色级授权（哪些角色可访问哪些服务） |
| `YalUserAuth` | 用户级授权（哪些用户可访问哪些服务） |
| `VpnUserRole` | 用户-角色关联表 |
| `VpnLogininfor` | VPN 用户登录日志（用户名、IP、状态、访问时间） |
| `VpnDeptYianlianMapping` | 每个 LineApp 下，本地部门 ID ↔ 远程易安联部门 ID |
| `VpnRoleYianlianMapping` | 每个 LineApp 下，本地角色 ID ↔ 远程易安联角色 ID |
| `VpnUserYianlianMapping` | 每个 LineApp 下，本地用户 ID ↔ 远程易安联用户 ID |

三张 `*YianlianMapping` 表至关重要——它们维护本地数据库与每个远程易安联实例之间的 ID 对应关系（每个 LineApp 拥有各自独立的 ID 空间）。

### 代码约定

- 列表接口使用 `startPage()` 分页，返回 `TableDataInfo`
- 单实体接口返回 `AjaxResult.success()`
- 权限：`@RequiresPermissions("vpn:user:add")`、`"yianlian:dept:list"` 等
- 日志：`@Log(title = "VPN用户管理", businessType = BusinessType.INSERT)`
- REST 模式：`GET /list`、`GET /{id}`、`POST`、`PUT`、`DELETE /{ids}`

## 数据库

SQL 脚本位于 `sql/`（由 `sql/export_init_db.py` 从测试库导出，可重入初始化）：
- `ry-cloud.sql` — 业务库（数据库：`ry-cloud`）完整初始化：含框架表、VPN 业务表（`vpn_user`、`vpn_dept`、`vpn_service`、`vpn_service_group`、映射表、`yal_*_auth` 等）。框架种子数据（`sys_menu`、`sys_dict_*`、`sys_user` 等）保留；业务/日志/代码生成表仅建表结构、不含数据；`qrtz_*` 表由 `quartz.sql` 单独管理，不包含在此脚本内
- `ry-config.sql` — Nacos 配置表（数据库：`ry-config`，含 `config_info` 等全部服务配置数据）
- `quartz.sql` — Quartz 调度器表（`qrtz_*`，建在 `ry-cloud` 库中）
- `update/*.yml` — Nacos 配置示例片段（TLS、选线验证码、TCP 等）
- `export_init_db.py` — 数据库初始化脚本导出工具：连接测试库、按表黑名单导出结构+种子数据

> 初始化顺序（MySQL `docker-entrypoint-initdb.d` 按文件名字母序）：`quartz.sql` → `ry-cloud.sql` → `ry-config.sql`。容器设 `MYSQL_DATABASE=ry-cloud`，故 `quartz.sql` 默认建在 `ry-cloud` 库。

MySQL 5.7。Docker 凭据：root/root123456。

## 注意事项

- 没有测试基础设施或 CI/CD；部署为手动方式
- 提交信息和代码注释均使用中文
- API 文档：`易安联对外开放接口文档 V1.6.docx`
- 本地开发使用的 Nacos 服务器：`10.9.2.177:8848`（在 Maven profile 中配置）
