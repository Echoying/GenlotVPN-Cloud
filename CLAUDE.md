# CLAUDE.md
## 语言规范
- 所有对话和文档尽量使用简体中文。
- 所有回答尽量使用简体中文[reference:8]。
- 代码注释尽量使用简体中文[reference:9]。

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GenlotVPN-Cloud is a VPN management platform built on **RuoYi Cloud (若依微服务) v3.6.8**. The core custom module is **YianLian (易安联)** (`ruoyi-modules/ruoyi-yianlian`), which manages VPN users, departments, roles, services, and service groups locally, then synchronizes all changes to one or more external YianLian VPN platform instances.

## Tech Stack

- **Backend**: Java 1.8, Spring Boot 2.7.18, Spring Cloud 2021.0.9, Spring Cloud Alibaba 2021.0.6.1
- **Frontend**: Vue 2.6.12, Element UI 2.15.14, Vue Router, Vuex
- **Infrastructure**: Nacos (service discovery + config), Sentinel (circuit breaking), Redis, MySQL 5.7
- **Build**: Maven (backend), npm/vue-cli (frontend)
- **ORM**: MyBatis with XML mapper files

## Build Commands

### Backend
```bash
# Full build
mvn clean package -DskipTests

# Build single module with dependencies
mvn clean package -pl ruoyi-modules/ruoyi-yianlian -am -DskipTests

# Maven profiles: dev (default), test
mvn clean package -P dev -DskipTests
```

### Frontend (Admin)
```bash
cd ruoyi-ui
npm install
npm run dev          # Dev server on port 80, proxies API to 10.9.2.177:80
npm run build:prod   # Production build
npm run build:stage  # Staging build
```

### Frontend (VPN User Login)
```bash
cd ruoyi-vpn-ui
npm install
npm run dev          # Dev server on port 8060, proxies API to 10.9.2.177:8060
npm run build:prod   # Production build
```

### Docker Deployment
```bash
cd docker
sh deploy.sh base      # Start MySQL, Redis, Nacos
sh deploy.sh modules   # Start all application modules
sh deploy.sh stop      # Stop all
sh deploy.sh rm        # Remove all containers
# Individual: sh deploy.sh gateway|auth|system|yianlian|gen|job|file|nginx|monitor
```

`docker/copy.sh` stages built JARs, SQL, and frontend dist into Docker build contexts.

### Local Development (Windows)
Batch scripts in `bin/`: `package.bat`, `clean.bat`, and `run-*.bat` for each service.

## Architecture

### Request Flow

Browser → Nginx (:80) → `/prod-api/` → Gateway (:8080) → downstream services

### Microservice Topology

| Service | Port | Purpose |
|---|---|---|
| ruoyi-gateway | 8080 | API Gateway (Spring Cloud Gateway) for system admin |
| ruoyi-vpn-gateway | 8081 | API Gateway for VPN users (separate gateway instance) |
| ruoyi-auth | 9200 | Authentication & token management (system users) |
| ruoyi-vpn-auth | 9400 | VPN user authentication (uses `ruoyi-api-yianlian`) |
| ruoyi-modules-system | 9201 | System management (users, roles, menus) |
| ruoyi-modules-yianlian | 9205 | **VPN business logic** (core custom module) |
| ruoyi-modules-gen | 9202 | Code generator |
| ruoyi-modules-job | 9203 | Scheduled tasks (Quartz) |
| ruoyi-modules-file | 9300 | File upload/storage |
| ruoyi-visual-monitor | 9100 | Spring Boot Admin monitoring |
| Nacos | 8848 | Service discovery + configuration center |

All services register with Nacos (namespace: `dev`). Application config is stored in Nacos, not in local `application.yml` — the local `bootstrap.yml` files only configure the Nacos connection.

### Maven Module Structure

- **ruoyi-api/**: Feign client interfaces and shared domain objects for inter-service calls
  - `ruoyi-api-system` — remote system service calls
  - `ruoyi-api-yianlian` — `RemoteVpnUserService` for VPN user authentication, shared DTOs (`VpnUser`, `VpnLoginUser`)
- **ruoyi-common/**: 9 shared libraries (core, redis, security, log, swagger, datascope, datasource, sensitive, seata)
- **ruoyi-modules/**: Service implementations — system, yianlian, gen, job, file
- **ruoyi-gateway/**: Route definitions, filters, auth validation (system admin)
- **ruoyi-vpn-gateway/**: Separate gateway for VPN users with captcha validation
- **ruoyi-auth/**: Login, logout, token refresh (system users)
- **ruoyi-vpn-auth/**: VPN user authentication service
- **ruoyi-visual/**: Spring Boot Admin monitoring

### VPN User Authentication Flow

VPN user login is handled by `ruoyi-vpn-auth` (separate from system admin auth in `ruoyi-auth`):

1. `VpnLoginService.login()` validates username/password length, checks the Redis IP blacklist (`CacheConstants.SYS_LOGIN_BLACKIPLIST`)
2. Calls the YianLian module via Feign (`RemoteVpnUserService.getUserInfo`, using `SecurityConstants.INNER` for internal trust)
3. On success, caches the **plaintext password** in Redis under `vpn_plain_pwd:{userId}` with a 30-minute TTL (matching token lifetime). This is consumed by downstream YianLian controller operations that must re-authenticate against the remote platform.
4. Password change (`changePassword`) and login-info recording are also delegated to the YianLian module through `RemoteVpnUserService`

`ruoyi-api/ruoyi-api-yianlian` defines the Feign contract (`RemoteVpnUserService`) and shared models (`VpnLoginUser`, `VpnUserInfo`, `VpnChangePasswordRequest`) that bridge `ruoyi-vpn-auth` and `ruoyi-modules-yianlian`.

### YianLian Module — Dual Service Layer

The YianLian module has a distinctive **dual service layer** architecture:

```
Controller
    ├── service/vpn/         (IVpnXxxService)       → Local MySQL CRUD
    │       └── mapper/      (XxxMapper + XML)       → MyBatis
    └── service/yianlian/    (IYiAnLianXxxService)   → Remote YianLian API sync
            └── client/      (OpenApiClient)          → RestTemplate HTTP calls
```

**Controllers** (11): `VpnUserController`, `VpnDeptController`, `VpnRoleController`, `VpnServiceController`, `VpnServiceGroupController`, `LineAppController`, `YalDeptAuthController`, `YalRoleAuthController`, `YalUserAuthController`, `YiAnLianAuthorityController`, `VpnLogininforController`

**Multi-line sync pattern** — the core architectural pattern: every write operation follows this sequence:
1. Persist to local DB via `IVpnXxxService`
2. Fetch all active `LineApp` instances (each represents a YianLian VPN device with its own `appId`/`appSecret`/`url`)
3. For each LineApp, call `IYiAnLianXxxService` to sync the change to the remote platform
4. Store the returned remote ID in the corresponding `*YianlianMapping` table

**OpenApiClient** (`client/OpenApiClient`): RestTemplate-based HTTP client for the YianLian external API:
- OAuth-style token management: acquires token via `appId`/`appSecret`, caches in Redis with key pattern `yianlian_token:{appId}`
- API base path: `/enadmin/api/open/v1/`
- All API paths defined in `YiAnLianConstants`
- Responses deserialized into `YiAnLianResponse<T>`

### YianLian Module — Package Layout

Under `ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/`:

- `controller/` — 11 REST controllers (see list above)
- `domain/` + `domain/vo/` — local entities and view objects
- `mapper/` — MyBatis mapper interfaces (XML lives under `src/main/resources/mapper/`)
- `service/vpn/` + `service/vpn/impl/` — local CRUD service layer (`IVpnXxxService`)
- `service/yianlian/` + `service/yianlian/impl/` — remote sync service layer (`IYiAnLianXxxService`)
- `client/`, `client/YiAnLianBase/`, `client/dto/`, `client/dto/vo/` — `OpenApiClient` and request/response DTOs for the external API
- `constant/` — `YiAnLianConstants` (API paths, token cache keys)
- `config/` — module configuration
- `utils/` — helpers (e.g. MD5 encryption for `appSecret`/`spaKey`)

### Domain Entities

| Entity | Purpose |
|---|---|
| `LineApp` | VPN line config — each is a separate YianLian device instance with `appId`, `appSecret`, `url` |
| `VpnUser` | VPN users (with `yianlianId` linking to remote platform) |
| `VpnDept` | Department hierarchy (tree with `ancestors`) |
| `VpnRole` | VPN user roles |
| `VpnService` | VPN-accessible applications/services |
| `VpnServiceGroup` | Tree-structured grouping of services |
| `YalDeptAuth` | Department-level authorization (which depts can access which services) |
| `YalRoleAuth` | Role-level authorization (which roles can access which services) |
| `YalUserAuth` | User-level authorization (which users can access which services) |
| `VpnUserRole` | User-role join table |
| `VpnLogininfor` | VPN user login logs (username, IP, status, access time) |
| `VpnDeptYianlianMapping` | Local dept ID ↔ remote YianLian dept ID per LineApp |
| `VpnRoleYianlianMapping` | Local role ID ↔ remote YianLian role ID per LineApp |
| `VpnUserYianlianMapping` | Local user ID ↔ remote YianLian user ID per LineApp |

The three `*YianlianMapping` tables are critical — they maintain ID correspondence between local DB and each remote YianLian instance (each LineApp has its own independent ID space).

### Code Conventions

- List endpoints use `startPage()` for pagination, return `TableDataInfo`
- Single-entity endpoints return `AjaxResult.success()`
- Permissions: `@RequiresPermissions("vpn:user:add")`, `"yianlian:dept:list"`, etc.
- Logging: `@Log(title = "VPN用户管理", businessType = BusinessType.INSERT)`
- REST pattern: `GET /list`, `GET /{id}`, `POST`, `PUT`, `DELETE /{ids}`

## Database

SQL scripts in `sql/`:
- `ry_20260402.sql` — RuoYi framework tables (database: `ry-cloud`)
- `ry_config_20250902.sql` — Nacos config tables (database: `ry-config`, includes all service configs)
- `quartz.sql` — Quartz scheduler tables
- `vpn/` — VPN tables: `vpn_user`, `vpn_dept`, `vpn_service`, `vpn_service_group`, `vpn_role_yianlian_mapping`, `yal_dept_auth`
- `update/` — Schema migrations

MySQL 5.7. Docker credentials: root/root123456.

## Notes

- No test infrastructure or CI/CD exists; deployment is manual
- Commit messages and code comments are in Chinese
- API documentation: `易安联对外开放接口文档 V1.6.docx`
- Nacos server for local dev: `10.9.2.177:8848` (configured in Maven profiles)
