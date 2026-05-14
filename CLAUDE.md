# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Tech Stack

- **RuoYi-Cloud**: Spring Boot **2.7.18**, Spring Cloud **2021.0.9**, Spring Cloud Alibaba **2021.0.6.1**, Java **8**
- **Frontend**: Vue 2 + Element UI in `ruoyi-ui/`
- **Infrastructure**: Nacos (config + discovery), MySQL, Redis, Nginx, Docker Compose
- Custom 易安联 VPN integration modules: `ruoyi-modules/ruoyi-yianlian` and `ruoyi-api/ruoyi-api-yianlian`

## Common Commands

### Backend (repo root)
```bash
# Full build
mvn clean package -DskipTests

# Compile single module (fast dev cycle)
mvn -pl ruoyi-modules/ruoyi-yianlian -am clean compile

# Package single module
mvn -pl ruoyi-modules/ruoyi-yianlian -am clean package -DskipTests

# Run tests for a module
mvn -pl ruoyi-modules/ruoyi-yianlian -am test

# Single test class / method
mvn -pl ruoyi-modules/ruoyi-yianlian -am -Dtest=SomeTest test
mvn -pl ruoyi-modules/ruoyi-yianlian -am -Dtest=SomeTest#methodName test
```

### Frontend (`ruoyi-ui/`)
```bash
npm install
npm run dev
npm run build:prod
```

### Docker Deployment (`docker/`)
```bash
sh copy.sh          # copies jars + dist into build context
docker compose up -d --build
sh deploy.sh base   # start infra (nacos, mysql, redis, nginx)
sh deploy.sh modules # start services (gateway, auth, system)
```

## Architecture

### Request Flow
Browser → Nginx (:80) → `/prod-api/` → Gateway (:8080) → downstream services via Nacos discovery

### Module Boundaries
| Module | Purpose |
|--------|--------|
| `ruoyi-gateway` | Route forwarding, filters, captcha endpoint |
| `ruoyi-auth` | Login/token/logout |
| `ruoyi-modules/ruoyi-system` | Core system APIs (user/role/dept/menu) |
| `ruoyi-modules/ruoyi-yianlian` | 易安联 VPN integration + VPN user/role/dept/service management |
| `ruoyi-api/*` | Feign client contracts for inter-service calls |
| `ruoyi-common/*` | Shared libs (core, security, redis, log, swagger, etc.) |

### Internal Service Calls
- Feign clients in `ruoyi-api/` with `FallbackFactory` for circuit breaking
- Internal-only endpoints guarded by `@InnerAuth` + `from-source: inner` header
- Response wrapper: `R<T>` for internal, `AjaxResult` for external

### YiAnLian Module Structure
```
ruoyi-modules/ruoyi-yianlian/src/main/java/com/ruoyi/yianlian/
├── client/             # OpenApiClient (HTTP client with Redis-cached tokens)
│   ├── dto/            # Request/Response DTOs (extend YiAnLianRequest base)
│   └── dto/vo/         # Value objects for YiAnLian entities
├── constant/           # YiAnLianConstants (all API paths)
├── controller/         # REST controllers (VPN user/role/dept/service/line)
├── domain/             # Database entities
├── mapper/             # MyBatis mapper interfaces
├── service/
│   ├── vpn/            # Local CRUD services (IVpnUserService, etc.)
│   └── yianlian/       # YiAnLian API integration services
│       └── impl/       # Implementations
└── resources/mapper/   # MyBatis XML mappings
```

### Key Patterns

**OpenApiClient**:
- Single HTTP client for all YiAnLian API calls
- Token cached in Redis key `yianlian_token:<appId>`, auto-refreshed
- When `responseType == Boolean.class`, checks only `code` field (because `data` may be `[]`)
- Create APIs expect **list format**: `[{...}]`, not single object

**Multi-line support**:
- Controllers iterate all `LineApp` records and call YiAnLian API for each line
- Roles use `vpn_role_yianlian_mapping` table to track IDs across lines
- Users/depts use name-matching to find corresponding YiAnLian entities

**DTO base class**:
- `com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest` (it's a package, not inner class)
- Has `@JsonIgnore String appId` field

**Permissions**: VPN controllers use `yianlian:*` prefix (e.g., `yianlian:user:list`, `yianlian:dept:edit`)

## YiAnLian API Paths

All defined in `YiAnLianConstants.java`, base: `/enadmin/api/open/v1/`

| Category | Operations |
|----------|----------|
| Token | `GET /enadmin/api/open/getToken` |
| Dept | list, create, update, delete under `.../contact/dept/` |
| User | list, create, update/{id}, delete, resetpassword, session under `.../contact/user/` |
| Role | list, create, update, delete under `.../role/` |
| Service Group | list, create, update, delete under `.../service/group/` |
| Service | list, create, update, delete under `.../service/` |
| Authority | grant group permissions: `.../contact/authority/group` |

## Database

VPN tables: `vpn_user`, `vpn_role`, `vpn_dept`, `vpn_user_role`, `vpn_role_dept`, `vpn_service_group`, `vpn_service`, `vpn_role_yianlian_mapping`

SQL schemas in `sql/vpn/`.

## Deployment Notes

- Gateway strips service prefix before forwarding (e.g., `/yianlian/vpn/dept` → `/vpn/dept`)
- YiAnLian service is NOT included in `deploy.sh modules`; deploy separately
- `docker/copy.sh` expects jar name `ruoyi-modules-yianlian.jar`
- Nacos addresses configured in root `pom.xml` profiles (`10.9.2.177:8848`); Docker uses `ruoyi-nacos:8848`

## Testing & CI

- No test infrastructure exists (no `src/test/java` in any module)
- No CI/CD pipelines configured
- No linting configuration
- Deployment is manual via docker scripts and `bin/upload_and_extract_docker.py`
