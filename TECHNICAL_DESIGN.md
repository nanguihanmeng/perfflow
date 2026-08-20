# PerfFlow 技术方案文档（v1.0）

> 目的：在编码前固化所有技术细节、字段、状态机、接口与权限边界。本文档通过评审后再进入实现。
> 适配范围：基于需求文本 + 现有空壳 Spring Boot 工程（Spring Boot 3.3.4 + Java 17）。

---

## 0. 与工程相关的关键决策

| 项 | 决策 |
|---|---|
| Spring Boot | **3.3.4**（LTS 稳定） |
| Java | **17** |
| 持久层 | MyBatis-Plus **3.5.9**（Spring Boot3 适配） |
| 安全 | Spring Security 6 + **jjwt 0.12.6** |
| API 文档 | **springdoc-openapi 2.6.0**（Swagger UI `/swagger-ui.html`） |
| 数据库 | MySQL 8.0+ |
| 导出 | Hutool-poi（避免引入 EasyExcel） |

---

## 1. 项目概述

**PerfFlow** 是面向企业的**季度绩效考核**系统，覆盖"自评→挂起→部门审核→领导评分→完成"五态全流程。

### 1.1 核心目标
1. 员工按 10 行模板填报季度工作，自评行分由 `基础分 × 完成率` 自动计算。
2. 部门领导进行调分（带强原因）与通过/打回。
3. 领导进行领导评分（占比 50%）。
4. 人事负责确认推送、延长挂起、汇总导出。
5. 系统管理员仅负责账号/部门/重置密码，**与业务数据完全隔离**。

### 1.2 角色

| 编码 | 中文 |
|---|---|
| ROLE_EMP | 员工 |
| ROLE_DEPT_LEAD | 部门领导 |
| ROLE_LEAD | 领导（公司层） |
| ROLE_HR | 人事 |
| ROLE_ADMIN | 系统管理员 |

---

## 2. 项目目录

```
perftlow/
├── TECHNICAL_DESIGN.md       # 本文件
├── README.md
├── backend/                   # Spring Boot 3.3.4 后端
│   ├── pom.xml
│   ├── src/main/java/com/perfflow/
│   ├── src/main/resources/
│   ├── src/test/java/com/perfflow/
│   ├── sql/perfflow.sql
│   └── target/                # Maven 构建产物
└── frontend/                  # 前端（Vue3 / React，待开发）
```

---

## 3. 数据库 6 张表（DDL）

字符集 `utf8mb4_0900_ai_ci`，引擎 `InnoDB`，主键统一 `bigint UNSIGNED AUTO_INCREMENT`，时间字段由 MyBatis-Plus `MetaObjectHandler` 填充。

### 3.1 `sys_department`
- `id PK`/`name`(UK)/`parent_id`(索引)/`leader_user_id`/`sort`/`remark`/`created_at`/`updated_at`

### 3.2 `sys_user`
- `id PK`/`username`(UK)/`password`(BCrypt)/`real_name`/`role`(EMP/DEPT_LEAD/LEAD/HR/ADMIN)/`dept_id`/`is_dept_lead`/`email`/`phone`/`status`/`last_login_at`/`must_change_password`/`created_at`/`updated_at`

### 3.3 `assessment_period`
- `id PK`/`name`/`year`/`quarter`/`start_date`/`suspend_end_date`/`dept_review_end_date`/`lead_score_end_date`/`auto_push_on_expire`/`status`(0=未开始/1=进行中/2=已结束)/`uk(year,quarter)`

### 3.4 `assessment_table`
- `id PK`/`period_id`/`user_id`/`dept_id`/`state`/`self_total_score`/`leader_score`/`final_score`/`grade`(A/B/C/D)/`suspend_extended_days`/`submitted_at`/`pushed_at`/`dept_approved_at`/`lead_finished_at`
- 索引：`uk(period_id,user_id)`/`idx_period_dept`/`idx_state`/`idx_period_state`

### 3.5 `assessment_row`
- `id PK`/`table_id`/`category`(PLAN/OPEN/BONUS)/`seq`(1-10)/`indicator_name`/`base_score`/`work_target`/`score_criteria`/`completion_rate`(0-150)/`self_score`/`adjusted_score`/`adjust_remark`(DB可见/前端隐藏)/`leader_score`/`row_result`/`is_frozen`
- 索引：`uk(table_id,seq)`/`idx_table_id`

### 3.6 `assessment_flow_log`
- `id PK`/`table_id`/`from_state`/`to_state`/`action`/`operator_id`/`operator_role`/`comment`/`created_at`

---

## 4. 模板数据（10 行）

| seq | category | base_score | 谁能编辑 |
|---|---|---|---|
| 1-5 | PLAN  | 16 × 5（=80） | 员工（自评阶段） |
| 6-7 | OPEN  | 10 × 2（=20） | 员工（自评阶段） |
| 8-10 | BONUS | +5/-5（合计 ±10）上限 | **员工不填**，由领导/部门领导处理 |

> 创建考核主表时按上表自动生成 10 行模板。BONUS 行 `frozen=1`。

---

## 5. 状态机

```
SELF_DRAFTING(1) ─SUBMIT─▶ SELF_SUSPENDED(2)
SELF_SUSPENDED ───PUSH───▶ DEPT_REVIEW(3)
DEPT_REVIEW  ──APPROVE─▶  LEAD_SCORING(4)
DEPT_REVIEW  ──REJECT──▶  SELF_DRAFTING(1)
LEAD_SCORING ─SUBMIT───▶  FINISHED(5)
```

挂起结束日 + `suspend_extended_days` 后当日 23:00 自动 PUSH（`task/PendingScheduleTask`）。

---

## 6. 计算规则

- `self_score = base_score × completion_rate / 100`，保留 2 位小数。
- `self_total_score = SUM(effective_score) WHERE category IN (PLAN, OPEN)`，`effective_score = adjusted_score != null ? adjusted_score : self_score`。
- `final_score = self_total_score × 0.5 + leader_score × 0.5`（截断 2 位）。
- 等级：`≥90 A`、`≥80 B`、`≥70 C`、`<70 D`。

---

## 7. 权限矩阵

| 数据维度 | EMP | DEPT_LEAD | LEAD | HR | ADMIN |
|---|---|---|---|---|---|
| 自己的表 | 可读（脱敏） | 可读（脱敏） | 可读 | 可读 | ❌ |
| 本部门所有表 | ❌ | 可读（脱敏） | 可读 | 可读 | ❌ |
| 全公司所有表 | ❌ | ❌ | 可读 | 可读 | ❌ |
| 写（员工行） | 可写（挂起前） | 调分（强 remark） | ❌ | ❌ | ❌ |
| 写领导评分 | ❌ | ❌ | ✓ | ❌ | ❌ |
| 推送 / 延挂 | ❌ | ❌ | ❌ | ✓ | 仅延挂 |
| 后台账号/部门 | ❌ | ❌ | ❌ | ❌ | ✓ |
| 脱敏（分数列） | 是 | 是 | 否 | 否 | — |

> 系统管理员访问业务接口统一返回 `403 ADMIN_NO_BUSINESS_VISIBILITY`（由 `AdminBusinessGuardInterceptor` 拦截）。
> 员工 / 部门领导看自己的表时，**考核得分 / 领导评分 / 最终得分 / 等级** 均置 `null`。
> `adjust_remark` 永远不返回给前端（DB 可见，前端隐藏）。

---

## 8. 接口设计

所有接口前缀 `/api`；Swagger UI 位于 `http://localhost:8080/api/swagger-ui.html`。

| 模块 | 路径 | 方法 | 角色 |
|---|---|---|---|
| 鉴权 | `/api/auth/login` `/api/auth/refresh` `/api/auth/me` `/api/auth/logout` | POST/GET | 任意 / 已登录 |
| 管理员 | `/api/admin/users` `/api/admin/users/{id}/reset-password` `/api/admin/departments` | 全部 | ADMIN |
| 考核周期 | `/api/periods` `/api/periods/current` `/api/periods/{id}/open` `/api/periods/{id}/close` | GET/POST | 任意 / ADMIN |
| 考核主表 | `/api/assessment-tables` `/api/assessment-tables/{id}` | GET | 任意 |
|  | `/api/assessment-tables/{id}/submit` | POST | EMP（本人） |
|  | `/api/assessment-tables/{id}/push` | POST | HR |
|  | `/api/assessment-tables/{id}/approve` `/reject` | POST | DEPT_LEAD |
|  | `/api/assessment-tables/{id}/lead-score` | POST | LEAD |
|  | `/api/assessment-tables/{id}/extend-suspend` | POST | HR / ADMIN |
| 考核行 | `/api/assessment-tables/{tid}/rows/{rid}` | PUT | EMP |
|  | `/api/assessment-tables/{tid}/rows/{rid}/adjust` | POST | DEPT_LEAD |
| 流程日志 | `/api/assessment-tables/{id}/logs` | GET | HR / LEAD / DEPT_LEAD |
| 导出 | `/api/assessment-tables/export` `/print` | GET | HR |
| 首页提醒 | `/api/home/reminders` | GET | 任意已登录 |

---

## 9. 安全方案

- BCrypt 哈希密码（cost=10）。
- JWT（HS256）双 Token：`accessToken 2h` + `refreshToken 7d`，请求头 `Authorization: Bearer`。
- `JwtAuthenticationFilter` 解析 token → 写入 `SecurityContext` + `DataScopeContext`（线程局部）。
- 控制器 `@PreAuthorize` + 数据级 scope 切面（`AssessmentPermissionService.scopeOf`）。
- `/admin/**` 仅 `ROLE_ADMIN`；其它业务接口对 `ROLE_ADMIN` 物理拦截。

---

## 10. 编码规范

| 类别 | 规范 |
|---|---|
| 类名 | UpperCamelCase |
| 方法/变量 | lowerCamelCase |
| 常量 | UPPER_SNAKE_CASE |
| 包名 | 全小写 |
| DB 字段 | snake_case |
| 分层 | Controller → Service → Mapper，DTO 隔离 Entity |
| 异常 | `BizException` + `@RestControllerAdvice` |
| 日志 | `@Slf4j`，关键操作使用 `Log.warn/info` |
| SQL | 禁止 `SELECT *`；`UPDATE` 必命中索引 |

---

## 11. 启动步骤

```bash
# 1) 准备数据库
mysql -u root -p < backend/sql/perfflow.sql

# 2) 调整 application-dev.yml 中 url/username/password

# 3) 编译 + 运行
cd backend
mvn -DskipTests spring-boot:run

# 4) 访问
# Swagger UI:    http://localhost:8080/api/swagger-ui.html
# OpenAPI JSON:  http://localhost:8080/api/v3/api-docs
```

---

## 12. 默认账号（密码均为 Init@123456）

| 角色 | 账号 |
|---|---|
| 系统管理员 | admin |
| 人事 | hr |
| 领导 | leader |
| 部门领导 | dept_lead / dept_lead2 |
| 员工 | emp01 / emp02 / emp03 |

首次登录后 `mustChangePassword = true`，前端按此强制跳改密页（后端只在重置时设置该字段）。
