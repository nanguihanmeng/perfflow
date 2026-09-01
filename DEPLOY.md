# PerfFlow Railway 部署文档（repo 模式）

> 本文档记录 **从 GitHub 仓库直接构建**（repo 模式）的 Railway 部署方式。
> 之前使用的「本地构建 Docker 镜像 → 推 GHCR → Railway 拉镜像」方式已弃用，见 `docs/deployment.md` 的历史记录。

## 部署架构

```
GitHub push → Railway 自动重新部署
   ├── backend 服务（rootDirectory=/backend，用 backend/Dockerfile）
   │     Spring Boot 3.3（prod profile）监听 8080，context-path /api
   │     → mysql.railway.internal:3306/railway
   └── frontend 服务（rootDirectory=/frontend，用 frontend/Dockerfile）
         nginx 托管 Vue 静态页 + 同域反代 /api → perfflow-backend.railway.internal:8080
```

- 前端地址：https://perfflow-frontend-production.up.railway.app
- 后端地址：https://perfflow-backend-production.up.railway.app/api
- 数据库：Railway 项目内网 `mysql.railway.internal:3306`，库名 `railway`

## 首次接入配置（一次性）

### 1. 连接 GitHub 仓库

Railway 面板 → New Project → Deploy from GitHub repo，选择 `nanguihanmeng/perfflow` 仓库。

### 2. 设置各服务 Root Directory（关键）

Repo 模式是 monorepo，Railway 无法自动区分 backend/frontend，必须给每个服务设置 **Root Directory**：

| 服务 | Root Directory | 构建方式 |
|---|---|---|
| perfflow-backend | `/backend` | 自动用 `backend/Dockerfile`（Maven → temurin 17 JRE） |
| perfflow-frontend | `/frontend` | 自动用 `frontend/Dockerfile`（Node 构建 → nginx 托管 + 反代） |

面板操作：选中服务 → **Settings** → **Source** → **Root Directory** 填入上表值。

> Railway 规则：服务 rootDirectory 下检测到 Dockerfile 时**总是优先用 Dockerfile 构建**。
> 设置 Root Directory 后，Nixpacks 不会再从仓库根目录误探测 Java/Node，两个常见问题自然消失。

### 3. 核对后端环境变量

服务 perfflow-backend → **Variables**，确认以下变量存在：

| 变量 | 值 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_HOST` | `mysql.railway.internal` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `railway` |
| `DB_USER` | `root` |
| `DB_PWD` | `LracuWMOykskTrNLUlGKbcecLnrQhgaV`（Railway 生成，如已改请用现值） |
| `JAVA_OPTS` | 可选，如 `-Xmx512m` |

> `SPRING_PROFILES_ACTIVE=prod` 虽由 `backend/Dockerfile` 内置（`ENV`），但显式在面板配置更稳妥，便于排查。

### 4. 触发部署

Root Directory 与变量配置完成后，Deploy 一次。之后每次 `git push` 到 GitHub，Railway 会自动重新构建部署 backend 与 frontend 两个服务。

## 日常更新流程

```bash
# 改完代码，正常提交推送即可，无需任何 docker 命令
git add -A && git commit -m "fix(xxx): ..." && git push
# Railway 自动部署，数分钟后生效
```

## 排障记录

### 「The executable 'java' could not be found」

- **现象**：前端或构建日志持续报 `java not found`
- **根因**：仓库根目录无构建文件，Nixpacks 从根目录探测到 `backend/pom.xml`（Maven/Java），生成了依赖 JDK 的构建/启动命令，而容器内无 JDK
- **修复**：给 backend/frontend 服务分别设置 Root Directory（`/backend`、`/frontend`），Railway 改为使用各目录下的 Dockerfile，不再走 Nixpacks Java 探测

### 网站明显变卡 / 接口超时

- **现象**：页面加载慢、接口转圈超时
- **根因**（两处叠加）：
  1. 前端 `package.json` 无 `start` 脚本，Nixpacks 回退用 `vite preview` 跑（开发服务器：无 gzip、无静态缓存、非生产端口）
  2. `frontend/nginx.conf` 的同域反代 `/api → perfflow-backend.railway.internal:8080` 未被使用，前端所有 `/api` 请求 404/超时
- **修复**：设置 frontend Root Directory 为 `/frontend` 后走 `frontend/Dockerfile`（nginx 托管，含 gzip、静态资源 30d 缓存、/api 反代），问题消失

### 其他核对项

- 后端日志目录 `/var/log/perfflow` 由 `backend/Dockerfile` 创建（非 root 用户 perfflow），repo 模式下必须走 Dockerfile 才能保证写日志成功
- 数据库结构升级：需执行 `railway` 库版本的 SQL（见 `backend/railway-init/perfflow.sql`），与后端代码改动一起提交

## 参考

- 历史 Docker/GHCR 部署方式、镜像清单、数据库初始化：`docs/deployment.md`
- 后端 Dockerfile：`backend/Dockerfile`
- 前端 Dockerfile + nginx 反代：`frontend/Dockerfile` + `frontend/nginx.conf`
