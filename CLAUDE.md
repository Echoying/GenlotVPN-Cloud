# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Tech stack and scope
- This repo is the **RuoYi-Cloud springboot2** line: Spring Boot **2.7.18**, Spring Cloud **2021.0.9**, Spring Cloud Alibaba **2021.0.6.1**, Java **8** (`pom.xml`).
- Frontend is Vue 2 + Element UI in `ruoyi-ui`.
- It is a multi-module Maven project (aggregator in root `pom.xml`):
  - `ruoyi-gateway` (gateway)
  - `ruoyi-auth` (auth)
  - `ruoyi-modules` (system/gen/job/file)
  - `ruoyi-visual` (monitor)
  - shared APIs/common libs in `ruoyi-api` and `ruoyi-common`

## Common commands

### Backend (root)
```bash
# Build all backend modules (used by docker packaging flow)
mvn clean package -DskipTests

# Build a single module with dependencies
mvn -pl ruoyi-gateway -am clean package -DskipTests
mvn -pl ruoyi-auth -am clean package -DskipTests
mvn -pl ruoyi-modules/ruoyi-system -am clean package -DskipTests

# Run tests for one module
mvn -pl ruoyi-auth -am test

# Run a single test (pattern)
mvn -pl ruoyi-auth -am -Dtest=TokenControllerTest test
mvn -pl ruoyi-auth -am -Dtest=TokenControllerTest#login test
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
# Copy SQL + built frontend dist + built backend jars into docker contexts
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

## High-level architecture

### Request path
- Browser hits `ruoyi-nginx` on port 80.
- Nginx forwards `/prod-api/` to `ruoyi-gateway:8080` (`docker/nginx/conf/nginx.conf`).
- Gateway routes traffic to downstream services via Spring Cloud Gateway + Nacos discovery.
- `GET /code` is handled directly in gateway functional routing (`ruoyi-gateway/src/main/java/com/ruoyi/gateway/config/RouterFunctionConfiguration.java`) and served by `ValidateCodeHandler`.

### Config and service discovery model
- Each service has `bootstrap.yml` with `spring.application.name` and profile `dev`.
- Nacos is both:
  - discovery center (`spring.cloud.nacos.discovery.server-addr`)
  - config center (`spring.cloud.nacos.config.server-addr`, `file-extension: yml`)
- Services also load shared config: `application-${profile}.yml`.
- Operationally important configs (datasource, redis, gateway routes, etc.) are expected from Nacos `*-dev.yml` dataIds.

### Module boundaries
- `ruoyi-auth`: login/token/logout/auth endpoints.
- `ruoyi-gateway`: unified entry, filters, captcha route (`/code`), forwarding.
- `ruoyi-modules/ruoyi-system`: core system/business APIs.
- `ruoyi-modules/ruoyi-gen`: code generator APIs.
- `ruoyi-modules/ruoyi-job`: scheduler/task APIs.
- `ruoyi-modules/ruoyi-file`: file service.
- `ruoyi-common/*`: shared security, datasource, redis, logging, etc. used by all services.

## Deployment-specific notes
- `docker/docker-compose.yml` defines all core containers and exposed ports (80/8080/8848/9848/9849/3306/6379/9100/9200/9201/9202/9203/9300).
- `docker/copy.sh` is required before image build if jars/dist changed; compose images depend on those copied artifacts.
- `deploy.sh modules` starts only nginx/gateway/auth/system; it does **not** include `gen/job/file/monitor`.
