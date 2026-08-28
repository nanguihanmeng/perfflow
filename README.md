# PerfFlow 绩效考核系统

企业级绩效考核平台：**个人考核**（发布周期 → Excel 导入考核明细 → 员工自评 → 部门审核 → 领导评分 → 自动计分）+ **部门考核**（KPI 填报 → 四级审批 → 部门等级联动员工名额）。

前后端分离，Spring Boot 3 + Vue 3，JWT 鉴权，八角色权限隔离，双保险权限（前端仅控制展示，后端独立鉴权 + 数据权限过滤）。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3.4、Java 17、Spring Security、MyBatis-Plus（含 jsqlparser）、jjwt、springdoc-openapi、Hutool-POI |
| 前端 | Vue 3.5、TypeScript 5、Vite 6、Element Plus 2.9、Pinia、ECharts 6 |
| 数据库 | MySQL 8.0+（utf8mb4 / InnoDB） |

## 项目结构

```
perftlow/
├── README.md                # 本文档（项目全貌）
├── docs/
│   ├── session-archive.md              # 会话归档：开发上下文 / 待办 / 验证记录
│   └── code-review/code_review_report.md  # 代码审查报告（华为规范 88.14/100）
├── backend/                 # Spring Boot 后端
│   ├── sql/
│   │   ├── perfflow.sql                 # 建库脚本（12 张表 + 基础种子数据）
│   │   ├── seed_data.sql                # 演示种子（分块运行参考，内容同 perfflow.sql）
│   │   └── upgrade_202608_matrix.sql    # 增量迁移：周期类型(7张表)/被考核人扩展/dept_id可空
│   ├── logs/                # 运行日志（backend-run.log / perfflow-dev.log）
│   ├── src/main/java/com/perfflow/
│   │   ├── module/          # 业务模块（见下表）
│   │   ├── security/        # JWT 过滤器 / 数据权限上下文
│   │   ├── config/          # Security / MyBatis-Plus / MVC / OpenAPI / Admin 拦截器
│   │   ├── common/          # 统一响应 / 错误码 / 全局异常 / 枚举 / Excel 工具
│   │   └── task/            # 定时任务（挂起提醒 / 催办 / 自动推送）
│   └── src/main/resources/  # application.yml + dev/prod 配置
└── frontend/                # Vue3 前端
    └── src/
        ├── api/             # Axios 接口封装（按模块）
        ├── views/           # 页面（按角色分组）
        ├── router/          # 路由 + 权限守卫
        ├── store/           # Pinia（auth/app）
        ├── layout/          # 侧边菜单（按角色显隐）
        └── types/           # TS 类型 / 枚举 / 角色映射
```

## 后端功能模块

| 模块 | 控制器 | 接口前缀 | 职责 |
|---|---|---|---|
| auth | AuthController | `/auth/**` | 登录 / 刷新令牌 / 改密 / 个人资料 |
| system | SysUserController / SysDeptController / UserOptionController / DeptOptionController | `/admin/users`、`/admin/depts`、`/users/options`、`/departments/options` | 部门与用户管理、角色下拉、部门下拉 |
| period | PeriodController | `/periods` | 周期发布（7 类）、开启（个人勾人 / 部门勾部门）、Excel 导入 |
| assessment | AssessmentTableController / AssessmentFlowController / AssessmentRowController / AssessmentExportController | `/assessment-tables` | 个人考核主表、流程流转（提交/推送/审核/评分/挂起）、行明细、导出 |
| deptassessment | DeptAssessmentController | `/dept-assessments` | 部门考核 4 级审批（填报→复核→初审→审批） |
| grade | GradeController | `/grade` | 等级名额配置、权重配置、等级联动计算 |
| monitor | MonitorController | `/monitor` | 考核进度统计（ECharts 看板） |
| notification | NotificationController | `/notifications` | 站内通知 |
| audit | AdjustLogController | `/audit-logs` | 加减分/调分审计日志 |
| home | HomeController | `/home` | 工作台提醒汇总 |
| common/excel | ExcelController | `/excel` | 导入模板下载 / 明细导入 |

## 前端页面（按角色菜单）

| 页面 | 路由 | 可见角色 |
|---|---|---|
| 工作台 | `/home` | 全部 8 角色 |
| 个人中心 | `/profile` | 全部 8 角色 |
| 我的通知 | `/notifications` | 除 ADMIN 外全部 |
| 我的考核表 | `/me/assessment` | EMP |
| 部门审核 | `/dept/review` | DEPT_LEAD |
| 领导评分 | `/lead/score` | LEAD |
| 周期管理 | `/hr/period` | PERFORMANCE_HR |
| 考核列表 | `/hr/list` | LEAD / PERFORMANCE_HR |
| 部门考核填报 | `/dept-staff/assessment` | DEPT_STAFF |
| 进度看板 | `/operation/dashboard` | OPERATION / COMMITTEE |
| 部门考核初审 | `/operation/dept-audit` | OPERATION |
| 部门考核审批 | `/committee/approve` | COMMITTEE |
| 用户管理 | `/admin/users` | ADMIN |
| 部门管理 | `/admin/depts` | ADMIN |

## 角色体系（8 个）

| 角色 | 编码 | 主要职责 |
|---|---|---|
| 系统管理员 | ADMIN | 部门管理、用户管理；不可操作自己、不可创建/查看绩效考核管理员账号 |
| 绩效考核管理员 | PERFORMANCE_HR | 发布周期、勾选被考核人、Excel 导入、推送、挂起管理（最高权限，不参与考核） |
| 公司领导 | LEAD | 对推送来的考核表评分（本人的表由绩效委员会评分） |
| 部门领导 | DEPT_LEAD | 审核本部门员工自评，可调整得分与加减分项 |
| 员工 | EMP | 填报完成率并提交自评 |
| 部门绩效专员 | DEPT_STAFF | 填报本部门 KPI，发起部门考核 |
| 运营管理部 | OPERATION | 查看全公司数据、初审部门考核、查看审计日志、触发等级联动计算 |
| 绩效委员会 | COMMITTEE | 最终审批部门考核、评分 LEAD 的个人考核表 |

### 角色参与矩阵

| 角色 | 部门考核填报 | 部门考核复核 | 部门考核初审 | 部门考核审批 | 个人考核填报 | 个人考核调分 | 个人考核评分 | 结果查看 |
|---|---|---|---|---|---|---|---|---|
| 员工 EMP | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | 本人 |
| 部门绩效专员 DEPT_STAFF | ✅ | ❌ | ❌ | ❌ | ✅（作为被考核人） | ❌ | ❌ | 本部门 |
| 部门负责人 DEPT_LEAD | ❌ | ✅ | ❌ | ❌ | ✅（作为被考核人） | ✅（审核本部门员工） | ❌ | 本部门 |
| 公司领导 LEAD | ❌ | ❌ | ❌ | ❌ | ✅（作为被考核人） | ❌ | ✅（评他人） | 全公司 |
| 运营管理部 OPERATION | ❌ | ❌ | ✅ | ❌ | ✅（作为被考核人） | ❌ | ❌ | 全公司+审计 |
| 绩效委员会 COMMITTEE | ❌ | ❌ | ❌ | ✅ | ✅（作为被考核人） | ❌ | ✅（评 LEAD） | 全公司 |
| 绩效考核管理员 HR | — | — | — | — | — | — | — | 全量管理 |
| 系统管理员 ADMIN | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 不参与 |

**特殊规则**：公司领导（LEAD）无部门，个人考核表由绩效委员会评分，push 后跳过部门审核直达领导评分；被考核人角色覆盖 EMP/DEPT_LEAD/LEAD/DEPT_STAFF/OPERATION/COMMITTEE（排除 ADMIN/HR）。

## 核心业务流程

```
个人考核：
HR 发布周期（选类型 + 勾选被考核人）→ Excel 导入考核明细 → 被考核人填完成率并提交（挂起）
→ HR 推送 → 部门领导审核（可改自评得分/加减分项）→ 提交给公司领导
→ 领导评分（领导本人的表由绩效委员会评分）→ 自动计分与定级

部门考核：
HR 发布部门类周期（勾选部门）→ 绩效专员填报部门 KPI → 部门负责人复核
→ 运营管理部初审 → 绩效委员会审批 → 部门等级自动计算 → 等级联动填充员工名额
```

**计分规则**：自评得分 = 指标分数 × 完成率%；总分 = 50% 自评 + 50% 领导评分（含 BONUS 加减分）；等级 A≥90 / B≥75 / C≥60 / D<60。中层（DEPT_LEAD）最终得分 = 部门分×部门权重 + 个人分×个人权重（默认 50/50），员工（BASIC）默认 30/70。

## 七种周期类型（对应七张表）

| 类型 | 对应表 | 线 | 周期 |
|---|---|---|---|
| 季度目标填报 QUARTER_GOAL | 表1：被考核人岗位+季度目标填报 | 个人 | 季度 |
| 岗位季度绩效考核 QUARTER_ASSESS | 表2：岗位季度绩效考核表 | 个人 | 季度 |
| 个人年度绩效考核 ANNUAL_ASSESS | 表3：个人岗位年度绩效考核表 | 个人 | 年度 |
| 加减分项信息申请 BONUS_APPLY | 表4：加减分项信息申请表 | 个人 | 季度 |
| 年度部门任务分解 DEPT_YEAR_TASK | 表5：年度部门工作任务分解表 | 部门 | 年度 |
| 季度部门任务调整 DEPT_QUARTER_ADJUST | 表6：季度部门工作任务分解（调整）表 | 部门 | 季度 |
| 年度部门绩效考核评分 DEPT_YEAR_SCORE | 表7：年度部门绩效考核评分表 | 部门 | 年度 |

> 个人线类型开启时勾选被考核人（EMP/DEPT_LEAD/LEAD/DEPT_STAFF/OPERATION/COMMITTEE），部门线类型开启时勾选部门（按部门生成主表 + 7 行 KPI 模板）；年度类型强制 quarter=0，同一 年+季度+类型 全局唯一。表 1-7 均映射现有明细表结构，表 4/表 6 暂未建独立表。

## 定时任务

| 时间 | 任务 | 说明 |
|---|---|---|
| 每天 08:00 | 挂起到期提醒检查 | 挂起即将结束（≤3 天）实时提醒（工作台展示） |
| 每天 09:00 | 催办 | 截止前 3 天未提交 → 每日推送员工；逾期未提交 → 推送员工 + 部门负责人 |
| 每天 23:00 | 自动推送 | 超期未推送且 auto_push_on_expire=1 的主表自动 PUSH 进入部门审核 |

## 安全设计

- **双保险权限**：前端路由/按钮仅为展示控制，所有权限判断在后端独立实现（`@PreAuthorize` 方法级 + 数据权限独立鉴权过滤）
- **JWT + 单账号互踢**：access token 2h / refresh token 7d，登录令牌带版本号（`token_version`），新设备登录自动使旧设备失效
- **数据权限**：`DataScopeContext` 按角色/部门限定可见范围（如 DEPT_LEAD 仅见本部门 + 本人，COMMITTEE 见 LEAD 的表 + 全公司已完成）
- **管理隔离**：ADMIN 不可访问业务接口（拦截器 403），不可操作自己，不可创建/查看绩效考核管理员账号
- **状态流转**：状态机 + 乐观锁（version 字段）防并发；首次登录强制改密（must_change_password）
- **审计留痕**：加减分/分数调整写入 `adjust_log`（默认前台不可见，运营管理部/管理员可见）

## 数据库设计（12 张表）

| 域 | 表 | 说明 |
|---|---|---|
| 基础 | `sys_department` / `sys_user` | 部门 / 用户（8 角色，含 token_version、must_change_password） |
| 个人考核 | `assessment_period` / `assessment_table` / `assessment_row` / `assessment_flow_log` | 周期（7 类）/ 主表（含等级联动字段）/ 行明细 / 流程日志 |
| 部门考核 | `dept_assessment` / `dept_kpi_row` | 部门考核主表（乐观锁 version）/ KPI 明细行 |
| 等级配置 | `grade_quota_config` / `weight_config` | 名额比例（部门等级×员工层级）/ 权重配置（全局默认 + 周期覆盖） |
| 支撑 | `adjust_log` / `notification` | 调整审计日志 / 站内通知 |

**SQL 脚本说明**：
- `backend/sql/perfflow.sql` — 建库脚本：12 张表 DDL + 基础种子数据（部门、用户、等级配额、权重配置）。**考核周期与考核主表不预置数据**，由 HR 在系统内发布周期、开启后自动生成。
- `backend/sql/upgrade_202608_matrix.sql` — 旧库增量迁移（幂等，information_schema 动态判断）：周期表加 `period_type` 列、唯一键升级为 `uk_year_quarter_type`、`assessment_table.dept_id` 改可空。

## 快速开始

```bash
# 1. 初始化数据库（建库 + 12 张表 + 基础种子数据）
mysql -u root -p < backend/sql/perfflow.sql

# 2. 配置数据库连接
#    编辑 backend/src/main/resources/application-dev.yml 的 url/username/password

# 3. 启动后端（默认 dev profile，端口 8080，context-path /api）
cd backend
mvn -DskipTests package
java -jar -Dspring.profiles.active=dev target/perfflow-backend.jar

# 4. 启动前端
cd frontend
npm install
npm run dev   # 访问 http://localhost:5173
```

接口文档（Swagger）：http://localhost:8080/api/swagger-ui.html

### 常用命令

| 操作 | 命令 |
|---|---|
| 后端编译 | `cd backend && mvn compile` |
| 后端测试 | `cd backend && mvn test` |
| 后端打包 | `cd backend && mvn -DskipTests package`（需先停运行中的进程） |
| 后端启动 | `java -jar -Dspring.profiles.active=dev target/perfflow-backend.jar` |
| 前端类型检查 | `cd frontend && npx vue-tsc --noEmit` |
| 前端构建 | `cd frontend && npm run build` |
| 旧库迁移 | `mysql -u root -p perfflow < backend/sql/upgrade_202608_matrix.sql` |

## 默认账号

所有账号初始密码均为 `12345678`（首次登录需修改密码）。

| 角色 | 账号 | 部门 |
|---|---|---|
| 系统管理员 | `admin` | — |
| 绩效考核管理员 | `hr` | — |
| 公司领导 | `leader` | — |
| 部门领导 | `bumen1` / `bumen2` | 技术部 / 产品部 |
| 员工 | `emp01` / `emp02` / `wanggong` / `emp03` | 技术部 / 产品部 |
| 部门绩效专员 | `deptstaff` | 技术部 |
| 运营管理部 | `yunying` | 人事部 |
| 绩效委员会 | `weiyuan` | 人事部 |

## 已知事项

- 表4（加减分申请）/表6（季度调整）当前仅做「类型 + 导入格式提示 + 映射现有明细表」，未建独立表结构
- 等级联动小人数边界：2 人时名额向下取整为 0 全落 D（已知简化）
- 开发/验证记录与待办见 `docs/session-archive.md`

## 贡献

欢迎提交 Issue 与 PR。代码遵循《阿里巴巴 Java 开发手册》。
