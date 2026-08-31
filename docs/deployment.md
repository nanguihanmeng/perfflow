# PerfFlow 部署文档（2026-08-31）

> 前后端已部署到 Railway（新加坡区域）。前端同域 nginx 反代后端，无 CORS 问题。

## 线上地址

| 端 | 地址 | 说明 |
|---|---|---|
| 前端 | https://perfflow-frontend-production.up.railway.app | nginx 托管 Vue 静态页 + 反代 /api |
| 后端 | https://perfflow-backend-production.up.railway.app/api | Spring Boot 3.3（prod profile） |
| 数据库 | Railway MySQL（项目内网 `mysql.railway.internal:3306`） | 库名 `railway`，12 张表 + 种子数据 |

## 架构

```
浏览器
  → perfflow-frontend-production.up.railway.app (nginx:alpine 容器)
      ├── 静态文件 / SPA 路由回退
      └── /api/* → 反代 → perfflow-backend.railway.internal:8080 (Spring Boot 容器)
                              → mysql.railway.internal:3306/railway (MySQL 容器)
```

同项目内网 DNS 直连，无需 CORS、无需公网端口。

## Docker 镜像（GitHub Container Registry）

所有镜像已设为 **Public**，均可匿名拉取。

| 镜像 | 说明 |
|---|---|
| `ghcr.io/nanguihanmeng/perfflow-backend:v2.0.0` | 后端：多阶段构建（maven:3.9-eclipse-temurin-17 → eclipse-temurin:17-jre），非 root 运行，监听 8080 |
| `ghcr.io/nanguihanmeng/perfflow-frontend:v2.0.0` | 前端：多阶段构建（node:20-alpine → nginx:alpine），nginx 托管 + 反代，监听 80 |
| `ghcr.io/nanguihanmeng/perfflow-mysql:8` | mysql:8 的 GHCR 副本（建表执行器用过，可留可删） |

镜像源文件：
- `backend/Dockerfile`（后端）
- `frontend/Dockerfile` + `frontend/nginx.conf`（前端）

## 后端环境变量（Railway service: perfflow-backend）

| 变量 | 值 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod`（镜像内置，无需设） |
| `DB_HOST` | `mysql.railway.internal` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `railway` |
| `DB_USER` | `root` |
| `DB_PWD` | `LracuWMOykskTrNLUlGKbcecLnrQhgaV`（Railway 生成） |
| `JAVA_OPTS` | 可选，如 `-Xmx512m` |

## 数据库初始化（已执行）

Railway MySQL 初始为空库。建表方式：用**一次性建表执行器镜像**在 Railway 平台内执行建表脚本（本地无法连内网 MySQL）：

- 执行器：`ghcr.io/nanguihanmeng/perfflow-railway-init:v1`（基于 mysql:8，ENTRYPOINT 覆盖为 init.sh）
- SQL：`backend/sql/perfflow.sql` 改造库名为 `railway`（原为 `perfflow`），放 `backend/railway-init/perfflow.sql`
- 建表执行器服务用完即删，当前项目仅保留 MySQL / perfflow-backend / perfflow-frontend 三个服务

**注意**：后端连的是 `railway` 库（不是 `perfflow`）。以后升级数据库结构，需执行对应 `railway` 库版本的 SQL。

## 常用操作

### 更新后端

```bash
cd backend
mvn -DskipTests package
docker build -t ghcr.io/nanguihanmeng/perfflow-backend:v2.0.1 .
docker push ghcr.io/nanguihanmeng/perfflow-backend:v2.0.1
# Railway 界面 Redeploy 或 API serviceInstanceRedeploy
```

### 更新前端

```bash
cd frontend
docker build -t ghcr.io/nanguihanmeng/perfflow-frontend:v2.0.1 .
docker push ghcr.io/nanguihanmeng/perfflow-frontend:v2.0.1
# Railway Redeploy
```

### 本地调试后端

```bash
cd backend && java -jar -Dspring.profiles.active=dev target/perfflow-backend.jar
# 连本机 MySQL 3306 / perfflow 库
```

## Railway CLI / API

- CLI 登录：`railway login`（浏览器授权）；关联项目：`railway link`
- 常用：`railway status` / `railway variables` / `railway logs -s <服务>` / `railway up`
- GraphQL API 端点：`https://backboard.railway.com/graphql/v2`，Bearer token 存于 `C:\Users\lenovo\.railway\config.json` 的 `user.accessToken`（会随 `railway login` 刷新）
- 曾用 API 完成：serviceCreate（Docker 镜像服务）、variableUpsert（配环境变量）、serviceDomainCreate（公网域名）、serviceDelete（清理废弃服务）、serviceInstanceRedeploy（重部署）

## 已知问题

1. **Vercel 部署已放弃**：`frontend-six-ashen-88.vercel.app` 等域名在本机网络（国内）无法访问（TCP 层被阻断，非部署问题），故前端迁移到 Railway
2. Railway token 会过期：长时间后 GraphQL 报 `Not Authorized`，需重新 `railway login` 刷新
3. 新建 GHCR 包默认私有：Railway 拉取会 FAILED，需在 GitHub Packages 设为 Public
4. Railway 免费版资源有限：项目里多余服务（尤其废弃空壳）会触发 `resource provision limit exceeded`，需清理
