# PerfFlow Backend

> Spring Boot 3.3.4 + Java 17 + MyBatis-Plus 3.5.9 + Spring Security 6 + JWT (jjwt 0.12.6) + springdoc-openapi 2.6.0。
>
> 详细技术方案见 [`../docs/TECHNICAL_DESIGN.md`](../docs/TECHNICAL_DESIGN.md)。

## 一、技术栈

| 模块 | 版本/选型 |
|---|---|
| JDK | 17 |
| Spring Boot | 3.3.4 |
| MyBatis-Plus | 3.5.9 |
| Spring Security | 6.x |
| JWT | jjwt 0.12.6 |
| API 文档 | springdoc-openapi 2.6.0（`/swagger-ui.html`） |
| 数据库 | MySQL 8.0+ |
| 导出 | Hutool-poi |

## 二、目录结构

```
backend/
├── pom.xml
├── README.md
├── sql/
│   ├── perfflow.sql              # 建库 + DDL + 种子数据
│   └── seed_data.sql             # 演示数据
└── src/
    └── main/
        ├── java/com/perfflow/
        │   ├── PerfFlowApplication.java
        │   ├── common/                # Result / BizException / GlobalHandler / RoleConst
        │   ├── config/                # Security / MybatisPlus / OpenAPI / Cors
        │   ├── security/              # JwtUtil / Filter / DataScope
        │   ├── module/
        │   │   ├── auth/              # 登录/刷新/我
        │   │   ├── system/            # 部门/用户（仅管理员）
        │   │   ├── period/            # 考核周期
        │   │   ├── assessment/        # 主表/行/流程/状态机/计算/导出
        │   │   └── home/              # 首页提醒
        │   └── task/                  # PendingScheduleTask
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── application-prod.yml
            ├── mapper/                # 扩展 XML
            └── logback-spring.xml
```

## 三、快速开始

### 1. 准备数据库

```bash
mysql -u root -p < sql/perfflow.sql
```

默认种子数据：1 管理员 + 1 HR + 1 LEAD + 2 部门领导 + 3 员工，2 个部门 + 1 个人事部，1 个 2026Q3 周期（已自动开启 + 全员主表与 10 行模板）。

**所有默认密码**：`Init@123456`，首次登录强制修改（响应体里 `mustChangePassword: true`）。

### 2. 修改连接信息

`src/main/resources/application-dev.yml` 中调整 `url/username/password`。

### 3. 启动

```bash
./mvnw spring-boot:run
```

| 地址 | 说明 |
|---|---|
| http://localhost:8080/api/swagger-ui.html | 接口文档 |
| http://localhost:8080/api/v3/api-docs | OpenAPI JSON |

## 四、默认账号

| 角色 | 账号 |
|---|---|
| 系统管理员 | `admin` |
| 部门领导 | `dept_lead` / `dept_lead2` |
| 领导 | `leader` |
| 人事 | `hr` |
| 员工 | `emp01` / `emp02` / `emp03` |

## 五、API 速览

| 模块 | 前缀 | 典型接口 |
|---|---|---|
| 鉴权 | `/api/auth/*` | `/login`, `/refresh`, `/me`, `/logout` |
| 管理员 | `/api/admin/*` | `/users`, `/departments`, `/users/{id}/reset-password` |
| 考核周期 | `/api/periods/*` | `/current`, `/{id}/open`, `/{id}/close` |
| 考核主表 | `/api/assessment-tables/*` | `/submit`, `/push`, `/approve`, `/reject`, `/lead-score`, `/extend-suspend` |
| 考核行 | `/api/assessment-tables/{tid}/rows/{rid}` | `PUT`, `POST /adjust` |
| 流程日志 | `/api/assessment-tables/{id}/logs` | 仅领导/部门领导/人事 可读 |
| 导出 | `/api/assessment-tables/export` | 仅人事 |
| 首页提醒 | `/api/home/reminders` | 已登录 |

## 六、状态机

```
SELF_DRAFTING(1)  --SUBMIT-->  SELF_SUSPENDED(2)
SELF_SUSPENDED    --PUSH---->  DEPT_REVIEW(3)
DEPT_REVIEW       --APPROVE->  LEAD_SCORING(4)
DEPT_REVIEW       --REJECT-->  SELF_DRAFTING(1)
LEAD_SCORING      --SUBMIT-->  FINISHED(5)
```

挂起结束日 + `suspend_extended_days` 后的 23:00 自动 PUSH（任务见 `task/PendingScheduleTask`）。

## 七、字段脱敏

**员工 / 部门领导** 在 API 返回中被脱敏的字段：
- 行：`self_score / adjusted_score / leader_score`
- 主表：`self_total_score / leader_score / final_score / grade`

**`adjust_remark`** 永远不返回给前端（DB 可见，前端隐藏）。

## 八、测试

```bash
mvn test
```

## 九、常见问题

**Q：管理员访问业务接口？** → 403 `ADMIN_NO_BUSINESS_VISIBILITY`。
**Q：周期未开启能提交吗？** → 否，`/submit` 会校验 `period.status=1`。
