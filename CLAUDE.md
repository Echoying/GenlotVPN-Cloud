# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Tech stack and scope
- This repo is **RuoYi-Cloud springboot2**: Spring Boot **2.7.18**, Spring Cloud **2021.0.9**, Spring Cloud Alibaba **2021.0.6.1**, Java **8**.
- Frontend is Vue 2 + Element UI in `ruoyi-ui`.
- Multi-module Maven project (root `pom.xml`): gateway/auth/business modules/visual + shared `ruoyi-api` and `ruoyi-common`.
- This branch also includes custom 易安联 modules:
  - `ruoyi-modules/ruoyi-yianlian` (`artifactId: ruoyi-modules-yianlian`)
  - `ruoyi-api/ruoyi-api-yianlian`

## Common commands

### Backend (repo root)
```bash
# Full backend build (used by docker packaging flow)
mvn clean package -DskipTests

# Build one service with dependent modules
mvn -pl ruoyi-gateway -am clean package -DskipTests
mvn -pl ruoyi-auth -am clean package -DskipTests
mvn -pl ruoyi-modules/ruoyi-system -am clean package -DskipTests
mvn -pl ruoyi-modules/ruoyi-yianlian -am clean package -DskipTests

# Compile only (faster for development)
mvn -pl ruoyi-modules/ruoyi-yianlian -am clean compile

# Run tests for one module
mvn -pl ruoyi-auth -am test
mvn -pl ruoyi-modules/ruoyi-yianlian -am test

# Run a single test / single test method
mvn -pl ruoyi-auth -am -Dtest=TokenControllerTest test
mvn -pl ruoyi-auth -am -Dtest=TokenControllerTest#login test
mvn -pl ruoyi-modules/ruoyi-yianlian -am -Dtest=YiAnLianTokenServiceImplTest test
mvn -pl ruoyi-modules/ruoyi-yianlian -am -Dtest=InnerYiAnLianControllerTest#getToken_success test
```

### Frontend (`ruoyi-ui`)
```bash
npm install
npm run dev
npm run build:prod
npm run build:stage
npm run preview
```

### Docker deployment (`docker`)
```bash
# Copy SQL + frontend dist + backend jars into docker build context
sh copy.sh

# Start all containers
docker compose up -d --build

# Check status / logs
docker compose ps
docker compose logs -f ruoyi-gateway
docker compose logs -f ruoyi-auth

# Stop / remove
docker compose stop
docker compose down
```

Alternative script entrypoints in `docker/deploy.sh`:
```bash
sh deploy.sh base
sh deploy.sh modules
sh deploy.sh stop
sh deploy.sh rm
```

### 177 packaging/upload helper (from repo root)
```bash
python bin/upload_and_extract_docker.py --password "<服务器密码>"
```

## High-level architecture

### Request path
- Browser hits `ruoyi-nginx` on port 80.
- Nginx forwards `/prod-api/` to `ruoyi-gateway:8080` (`docker/nginx/conf/nginx.conf`).
- Gateway routes to downstream services through Spring Cloud Gateway + Nacos discovery.
- `GET /code` is handled directly in gateway functional routing (`ruoyi-gateway/src/main/java/com/ruoyi/gateway/config/RouterFunctionConfiguration.java`) via `ValidateCodeHandler`.

### Config and service discovery model
- Each service has `bootstrap.yml` with `spring.application.name` and profile `dev`.
- Nacos is used for both discovery and config center.
- Services typically load config from Nacos `*-dev.yml` DataIds (datasource, redis, gateway routes, etc.).
- Container deployment must ensure service-side Nacos address points to reachable host (usually `ruoyi-nacos:8848` inside compose network).

### Module boundaries
- `ruoyi-gateway`: unified entry, filters, captcha endpoint, route forwarding.
- `ruoyi-auth`: login/token/logout/auth endpoints.
- `ruoyi-modules/ruoyi-system`: core business/system APIs.
- `ruoyi-modules/ruoyi-gen`, `ruoyi-job`, `ruoyi-file`: generator/scheduler/file services.
- `ruoyi-modules/ruoyi-yianlian`: 易安联 integration endpoints and service logic.
- `ruoyi-api/ruoyi-api-yianlian`: Feign/internal API contracts for 易安联.
- `ruoyi-common/*`: shared security, datasource, redis, logging, core helpers.

### Internal service call pattern
- Internal APIs generally use `R<T>` as response wrapper.
- Internal-only endpoints are guarded via `@InnerAuth` and `from-source: inner` header convention.
- Feign clients in `ruoyi-api/*` modules define service-to-service contracts.
- Each Feign interface has a corresponding `FallbackFactory` for circuit breaking.

### YiAnLian module architecture
- **External endpoints** (`/yianlian/*`): Public REST APIs returning `AjaxResult`.
- **Internal endpoints** (`/yianlian/inner/*`): Feign-accessible APIs with `@InnerAuth`, returning `R<T>`.
- **Service layer**: Business logic with validation, calls `OpenApiClient` for third-party integration.
- **DTO pattern**: Request/Response DTOs in `client.dto` package, VOs in `api.domain.vo` package.
- **Constants**: API paths centralized in `YiAnLianConstants.java`.
- **VPN module**: Separate user/role/dept management for VPN users (independent from system module):
  - Controllers: `VpnUserController`, `VpnRoleController`, `VpnDeptController` at `/vpn/user`, `/vpn/role`, `/vpn/dept`
  - Accessed via `/yianlian/vpn/*` through gateway
  - Database tables: `vpn_user`, `vpn_role`, `vpn_dept`, `vpn_user_role`, `vpn_role_dept`
  - SQL schema in `sql/vpn/vpn_dept.sql`
  - Frontend views in `ruoyi-ui/src/views/vpn/` with API calls in `ruoyi-ui/src/api/vpn/`
  - Permissions use `yianlian:user:*`, `yianlian:role:*`, `yianlian:dept:*` prefix
  - No menu/post management (simplified compared to system module)

### Cross-module dependencies
- `ruoyi-yianlian` can call `ruoyi-system` via `RemoteDeptService` and `RemoteRoleService` (Feign).
- `ruoyi-system` exposes internal endpoints at `/dept/inner/list` and `/role/inner/list`.
- All internal calls require `SecurityConstants.FROM_SOURCE` header with value "inner".

## Deployment-specific notes
- `docker/docker-compose.yml` defines exposed ports and all core containers.
- `docker/copy.sh` is mandatory before image build when jars/dist change.
- `deploy.sh modules` starts only nginx/gateway/auth/system (not gen/job/file/monitor/yianlian).
- `docker/copy.sh` expects yianlian jar name `ruoyi-modules-yianlian.jar`, which matches `ruoyi-yianlian` module `artifactId`/`finalName`.
- Recommended deployment sequence from current project practice: frontend build → backend build → `docker/copy.sh` → upload/extract → remote `deploy.sh stop` → `deploy.sh base` → `deploy.sh modules` → health checks.
- **CRITICAL**: After adding new controllers or modifying backend code, you MUST rebuild the jar (`mvn -pl :ruoyi-modules-yianlian -am clean package -DskipTests`) and redeploy the service. The 404 errors often indicate the service is running an old jar without the new endpoints.

## Environment/config gotchas
- Root `pom.xml` profiles define Nacos addresses (`10.9.2.177:8848`) and namespaces (`dev`/`test`); service bootstrap files may point to in-network hostnames (e.g. `ruoyi-nacos:8848`) when running in Docker.
- For module startup issues, prioritize validating Nacos DataId/Group/namespace alignment (especially `*-dev.yml`) before changing code.
- Yianlian service is not included in `deploy.sh modules`; if deploying it, build/copy/upload and start that container explicitly.

## Code patterns and conventions

### Adding new business modules
1. Create module structure following existing pattern (controller/service/mapper/domain).
2. Add module to root `pom.xml` and `ruoyi-modules/pom.xml`.
3. Create corresponding API module in `ruoyi-api/` if cross-service calls are needed.
4. Use `@InnerAuth` for internal-only endpoints.
5. Follow DTO/VO separation: DTOs for transport, VOs for domain entities.

### MyBatis mapper conventions
- ResultMap ID matches entity name (e.g., `LineAppResult` for `LineApp`).
- Common SQL fragments use `<sql id="selectXxxVo">` pattern.
- Dynamic SQL uses `<if test="field != null and field != ''">` for string fields.
- Insert/Update use dynamic columns with `<if>` tags.

### Frontend conventions
- API calls in `src/api/` directory, organized by module.
- Views in `src/views/` follow module structure.
- Use `dict.type.xxx` for dictionary data binding.
- Form validation rules defined in component `rules` data property.
- Use `v-hasPermi` directive for permission control on buttons.

### Database migrations
- SQL update scripts go in `sql/update/` directory.
- New module schemas go in `sql/<module>/` directory (e.g., `sql/vpn/vpn_dept.sql`).
- Use descriptive filenames (e.g., `add_line_app_url.sql`).
- Include comments explaining the change purpose.

## Troubleshooting

### 404 errors on new endpoints
1. Verify the controller is compiled: `jar -tf ruoyi-modules/ruoyi-yianlian/target/ruoyi-modules-yianlian.jar | grep ControllerName`
2. Check `@RequestMapping` path matches the URL (gateway strips `/yianlian` prefix before forwarding)
3. Rebuild and redeploy: `mvn -pl :ruoyi-modules-yianlian -am clean package -DskipTests` then restart service
4. Verify service registered with Nacos and gateway can route to it

### Frontend build errors
- Missing imports: Check if all imported components/files exist
- API path mismatches: Frontend calls `/yianlian/path` → backend controller should have `@RequestMapping("/path")`
- Permission directives: Ensure `v-hasPermi` values match backend `@RequiresPermissions` annotations
