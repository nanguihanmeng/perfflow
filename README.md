# PerfFlow

> 绩效考核系统（Spring Boot 3.3.4 后端 + 前端开发材料）

## 工程结构

```
perftlow/
├── TECHNICAL_DESIGN.md         # 详细技术文档（数据库/状态机/接口/权限）
├── README.md                   # 本文件
├── start-backend.bat           # 一键启动后端（Windows）
├── backend/                    # Spring Boot 3.3.4 后端工程
│   ├── pom.xml
│   ├── src/main/java/com/perfflow/
│   ├── src/main/resources/
│   ├── sql/perfflow.sql        # 建库 + DDL + 种子数据
│   └── target/                 # Maven 构建产物
└── frontend/                   # 前端开发材料（OpenAPI + TS 类型 + API 客户端 + 文档）
    ├── README.md               # 前端开发指南
    ├── START_BACKEND.md        # 后端启动 + 默认账号
    ├── openapi/openapi.json    # OpenAPI 3.0 规范
    ├── types/                  # TypeScript 类型
    │   ├── result.ts           # 统一响应体 Result<T>
    │   ├── enums.ts            # 全部枚举
    │   └── dto.ts              # 全部 DTO
    ├── api/                    # TS API 客户端骨架
    │   ├── client.ts           # fetch 封装 + JWT
    │   ├── auth.api.ts
    │   ├── period.api.ts
    │   ├── assessment.api.ts
    │   ├── system.api.ts
    │   └── home.api.ts
    ├── docs/                   # 接口文档
    │   ├── 01-response.md      # 统一响应体
    │   ├── 02-enums.md         # 枚举常量
    │   ├── 03-masking.md       # 字段脱敏规则
    │   ├── 04-api-list.md      # 完整接口清单
    │   └── 05-mocks.md         # Mock 响应示例
    └── examples/login-flow.md  # 前端接入示例
```

## 一、技术栈

| 模块 | 版本/选型 |
|---|---|
| JDK | 17 |
| Spring Boot | 3.3.4 |
| MyBatis-Plus | 3.5.9 |
| Spring Security | 6.x |
| JWT | jjwt 0.12.6 |
| API 文档 | springdoc-openapi 2.6.0 |
| 数据库 | MySQL 8.0+ |
| 导出 | Hutool-poi |

详见 [`TECHNICAL_DESIGN.md`](./TECHNICAL_DESIGN.md)。

## 二、快速开始（后端）

```bash
# 1) 建库与种子数据
mysql -u root -p < backend/sql/perfflow.sql

# 2) 修改 backend/src/main/resources/application-dev.yml 中 url/username/password

# 3) 启动（Windows 一键脚本）
start-backend.bat

# 或手工启动
cd backend
mvn -DskipTests package
java -jar -Dspring.profiles.active=dev target\perfflow-backend.jar
```

| 地址 | 说明 |
|---|---|
| http://localhost:8080/api/swagger-ui.html | Swagger UI |
| http://localhost:8080/api/v3/api-docs | OpenAPI JSON（可对接前端的 `openapi/openapi.json`） |

## 三、默认账号

| 角色 | 账号 |
|---|---|
| 系统管理员 | admin |
| 人事 | hr |
| 领导 | leader |
| 部门领导 | dept_lead / dept_lead2 |
| 员工 | emp01 / emp02 / emp03 |

> 默认密码：`Init@123456`。首次登录强制改密（`must_change_password=1`）。

## 四、状态机速览

```
SELF_DRAFTING(1) ─SUBMIT─▶ SELF_SUSPENDED(2)
SELF_SUSPENDED ───PUSH───▶ DEPT_REVIEW(3)
DEPT_REVIEW   ──APPROVE─▶ LEAD_SCORING(4)
DEPT_REVIEW   ──REJECT──▶ SELF_DRAFTING(1)
LEAD_SCORING  ─SUBMIT───▶ FINISHED(5)
```

## 五、前端开发

请进入 [`frontend/`](./frontend/README.md)：

1. `openapi/openapi.json` —— OpenAPI 3.0 规范，可用 `openapi-typescript` 一键生成。
2. `types/*.ts` —— 与 Java DTO 1:1 对应的 TypeScript 类型。
3. `api/*.api.ts` —— 开箱即用的 fetch 客户端（已带 JWT 注入、统一错误处理）。
4. `docs/05-mocks.md` —— 所有接口的 Mock 响应示例。

## 六、许可

仅用于内部演示，按公司规定执行。