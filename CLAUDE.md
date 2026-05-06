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

## Deployment-specific notes
- `docker/docker-compose.yml` defines exposed ports and all core containers.
- `docker/copy.sh` is mandatory before image build when jars/dist change.
- `deploy.sh modules` starts only nginx/gateway/auth/system (not gen/job/file/monitor/yianlian).
- `docker/copy.sh` expects yianlian jar name `ruoyi-modules-yianlian.jar`, which matches `ruoyi-yianlian` module `artifactId`/`finalName`.
- Recommended deployment sequence from current project practice: frontend build → backend build → `docker/copy.sh` → upload/extract → remote `deploy.sh stop` → `deploy.sh base` → `deploy.sh modules` → health checks.

## Environment/config gotchas
- Root `pom.xml` profiles define Nacos addresses (`10.9.2.177:8848`) and namespaces (`dev`/`test`); service bootstrap files may point to in-network hostnames (e.g. `ruoyi-nacos:8848`) when running in Docker.
- For module startup issues, prioritize validating Nacos DataId/Group/namespace alignment (especially `*-dev.yml`) before changing code.
- Yianlian service is not included in `deploy.sh modules`; if deploying it, build/copy/upload and start that container explicitly.
