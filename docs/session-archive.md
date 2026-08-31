# PerfFlow 会话归档（2026-08-28）

> 本文件为 ZCode 会话上下文归档，供后续开发快速恢复上下文使用。

## 项目概览
- **位置**：`E:\workspace\perftlow`（backend=Spring Boot 3.3.4+Java17+MyBatis-Plus，frontend=Vue3+TS+Element Plus，MySQL8，git 仓库 origin=github.com/nanguihanmeng/perfflow，分支 main）
- **编码规范**：遵循 `alibaba-java-development-guide` skill（阿里规约黄山版）；代码完成后用 `code-reviewer` skill 审查
- **数据库连接**：root/password123456@localhost:3306/perfflow；后端 dev profile 启动在 8080
- **运行中的后端**：`java -jar -Dspring.profiles.active=dev target/perfflow-backend.jar`（新打包后重启过，PID 可变，日志在 backend/logs/backend-run.log）

## 当前系统状态（全部已实现并验证）
- **角色**（8 个）：EMP / DEPT_LEAD / LEAD / PERFORMANCE_HR(绩效考核管理员) / ADMIN / DEPT_STAFF(部门绩效专员) / OPERATION(运营管理部) / COMMITTEE(绩效委员会)
- **个人考核流程**：HR 发布周期(选类型+勾选被考核人)→Excel 导入明细→被考核人填完成率提交→HR 推送→部门领导审核(可改自评得分含加减分项)→提交给领导→领导评分→自动计分(总分=50%自评+50%领导，等级 A≥90/B≥75/C≥60/D<60，含 BONUS 加减分)
- **部门考核**：4 层审批（专员填报→负责人复核→运营初审→委员会审批），部门等级自动计算，等级联动→员工名额填充
- **其他**：单账号多设备互踢(token_version)、管理员不可见/操作自己、挂起阶段仅 HR 可见、状态下拉中文、流程记录中文、Excel 列宽+换行、进度看板(ECharts)、站内通知/催办、审计日志、权重配置

## 最近一次工作（2026-08 角色与考核参与矩阵优化，未提交！）
**依据 `PerfFlow 角色与考核参与矩阵.md` 优化，保留现有 8 角色编码，行为对齐矩阵。32 项改动均在本地工作区，未 commit/push。**

### 一、角色显示修复（问题1）
- `frontend/src/views/hr/PeriodManage.vue`：删除局部写死 `roleLabel()`（错误：`HR` 编码不存在、缺 4 角色），改用 `types/role.ts` 的 `RoleLabel` 中文映射

### 二、周期类型 + 七张表（问题2）
- **新增枚举** `backend/.../period/enums/PeriodType.java`：7 值对应表1-表7（QUARTER_GOAL/QUARTER_ASSESS/ANNUAL_ASSESS/BONUS_APPLY/DEPT_YEAR_TASK/DEPT_QUARTER_ADJUST/DEPT_YEAR_SCORE），各带中文名/所属线/是否年度/导入格式说明/参与填报角色集
- **DB**：`assessment_period` 加 `period_type` 列；唯一键 `uk_year_quarter` → `uk_year_quarter_type`(year,quarter,period_type)；`assessment_table.dept_id` 改可空（无部门被考核人）
- **迁移**：`backend/sql/upgrade_202608_matrix.sql`（新库/旧库双场景幂等，用 information_schema 动态判断列/索引）
- **后端**：`PeriodCreateReq`/`PeriodResp` 加 periodType；`create()` 年度类型强制 quarter=0 并按 (year,quarter,type) 判重；`open()` 按类型分支——个人线 `initForPeriod(userIds)`、部门线新增 `DeptAssessmentService.initForPeriod(period, deptIds)` 按部门生成主表+7 行 KPI 模板
- **接口**：`GET /users/options` 加可选 `roles` 过滤；新增 `GET /departments/options`（HR 用部门下拉）
- **前端**：`types/periodType.ts` 七类型元数据；PeriodManage 新建周期 7 选项+导入格式提示+年度隐藏季度；开启对话框按类型切换「用户勾选/部门勾选」；dto.ts/period.api.ts/system.api.ts/dept.api.ts 同步

### 三、考核与被考核关系（问题3 + 矩阵落地）
- `AssessmentTableService.initForPeriod`：被考核人扩为 EMP/DEPT_LEAD/LEAD/DEPT_STAFF/OPERATION/COMMITTEE（排除 ADMIN/HR）；无部门（如领导）允许建表
- **评分人规则**：LEAD 评非 LEAD 表；LEAD 的表由 COMMITTEE 评分（controller `hasAnyRole('LEAD','COMMITTEE')` + service 二次校验：LEAD 不能自评、COMMITTEE 仅可评 LEAD 的表）
- **push 跳部门审核**：被考核人 dept_id 为 NULL 时 push 直达 LEAD_SCORING（动作 PUSH_DIRECT）
- **数据权限** `AssessmentPermissionService`：被考核人角色可见本人表；DEPT_LEAD=本部门+本人；COMMITTEE=LEAD 的表+全公司已完成；LEAD=全公司非挂起+本人；EMP/DEPT_STAFF/OPERATION=本人
- **等级联动** `GradeCalculationService.resolveStaffLevel`：DEPT_LEAD→MIDDLE（激活部门分×50%+个人分×50% 加权）、其余→BASIC
- **部门考核数据范围**：`DeptAssessmentService.listAll` 对 DEPT_STAFF/DEPT_LEAD 仅返回本部门

### 四、文档同步
- `sql/perfflow.sql`：建表含 period_type、新唯一键、dept_id 可空；种子周期带类型
- `README.md`：新增「七种周期类型」「角色参与矩阵」章节
- `docs/code-review/code_review_report.md`：code-reviewer 审查报告（华为规范 88.14/100）

## 验证记录（本次全部通过）
- 后端 `mvn compile` ✅ / `mvn test` ✅ / `mvn package` ✅（重启后端为最新代码）
- 前端 `npx vue-tsc --noEmit` ✅ / `npm run build` ✅
- DB：Docker MySQL 8.0.46 验证全量 perfflow.sql + 迁移脚本（新库/旧库双场景）✅；真实 dev 库(3306) 已执行迁移 ✅
- curl 全链路：HR 登录 → 建 7 类周期（年度类型 quarter 强制 0）→ 开启 ANNUAL_ASSESS 个人年度（勾 emp01/leader/deptstaff，leader 无部门建表成功）→ 开启 DEPT_YEAR_TASK 部门年度（勾部门 1/2 生成两张 dept_assessment）→ /users/options?roles= 过滤 → /departments/options ✅

## 已知事项 / 待办
1. **未提交**：本次 32 项改动需用户确认后再 commit+push（此前用户明确"先不要 git 上去"）
2. **dev 库已迁移**（period_type 列+新唯一键+dept_id 可空），测试产生的周期(2026-*)/主表/部门表为 curl 验证数据，如需清理可删（id>=3 的周期）
3. 表4（加减分申请）/表6（季度调整）按确认仅做「类型+格式提示+映射现有明细表」，未做独立表结构
4. 等级联动小人数边界（2人时名额向下取整为 0 全落 D）是已知简化
5. 浏览器自动化环境不稳定，前端 UI 靠 vue-tsc/build 验证；如需 UI 验证需用户浏览器人工确认
6. 后端运行时若连库报 `Unknown column 'period_type'`，说明 dev 库未执行 `sql/upgrade_202608_matrix.sql`

## 关键账号密码
admin/hr/leader/bumen1/bumen2/deptstaff/yunying/weiyuan/emp01-03/wanggong = `12345678`（部分遗留 Init@123456）

## 常用命令
- 启动后端：`cd backend && java -jar -Dspring.profiles.active=dev target/perfflow-backend.jar`（后台，日志 backend/logs/backend-run.log）
- 打包：`mvn -DskipTests package`（需先停运行中的后端进程）；测试：`mvn test`；前端：`npm run dev` / `npm run build` / `npx vue-tsc --noEmit`
- DB 迁移：`mysql -u root -p perfflow < backend/sql/upgrade_202608_matrix.sql`
- 提交推送：`git add -A && git commit -m "..." && git push origin main`

---

# 会话归档补充（2026-08-31）

## 一、功能改动（已 commit 并 push 为 v2.0.0）

**提交**：`297932d` `release: v2.0.0 双考核线七大周期类型，角色参与矩阵全面落地`（93 文件，+4485/-345，tag `v2.0.0`）
**注意**：`docs/` 目录按要求未纳入提交（git 未跟踪）

### 大版本功能（commit message 已覆盖）
- 七种周期类型（表1-表7）：个人线 + 部门线，年度类型 quarter 固定 0，年+季+类型唯一
- 角色参与矩阵落地：被考核人扩至 6 角色，LEAD 表由 COMMITTEE 评分、无部门直达领导评分
- 部门考核四级审批 + 等级联动填充名额
- 部门负责人历史数据查询（本部门全周期，含已完成）
- 员工查看本人考核全程流程留痕
- 新增进度看板/站内通知/审计日志/等级配置模块 + 定时任务
- 单账号互踢、管理员业务隔离、乐观锁防并发

### 本次会话追加的功能修复（含在 v2.0.0 内）
1. **强制改密后免重新登录**：`PUT /auth/password` 返回新 `LoginResp`（含新 token，mustChangePassword=false），token_version 自增使旧 token 失效；前端改密成功直接 `setLogin` 进 `/home`
2. **负数指标分修复**：
   - 加减分项默认自评 0 分（`AssessmentCalcService.recalcByCompletionRate`，负数不再自动计负分）
   - 部门领导调分上限改为 `|指标分数|`（`AssessmentRowService` + 前端 `AssessmentRows.vue`）
3. **Excel 导入校验**（`AssessmentImportService.importRows`）：指标分数必填、不得为 0、正数之和必须 = 100（负数不计入）
4. **历史数据查询**：`DeptReview.vue` 改为全状态 + 周期筛选（部门负责人可查本部门历史）
5. **员工看留痕**：`AssessmentFlowController` 权限放开为 `isAuthenticated()`，先经 `getRequired` 复用主表数据权限

## 二、部署上线（2026-08-31，详见 docs/deployment.md）

- **架构**：前端 nginx 容器同域反代 `/api` → 后端 Spring Boot 内网 → MySQL 内网（无 CORS）
- **地址**：前端 https://perfflow-frontend-production.up.railway.app ；后端 https://perfflow-backend-production.up.railway.app/api
- **镜像**：`ghcr.io/nanguihanmeng/perfflow-backend:v2.0.0`、`perfflow-frontend:v2.0.0`（均 Public）
- **数据库**：Railway MySQL `railway` 库，12 张表 + 种子数据已建（用一次性执行器镜像在平台内执行，执行器已删）
- **环境变量**：后端 `DB_HOST=mysql.railway.internal` / `DB_PORT=3306` / `DB_NAME=railway` / `DB_USER=root` / `DB_PWD=LracuWMOykskTrNLUlGKbcecLnrQhgaV`
- **Vercel 放弃**：`*.vercel.app` 在本机网络 TCP 不可达（非部署问题），前端改放 Railway

## 三、关键账号

生产与种子一致：admin/hr/leader/bumen1/bumen2/deptstaff/yunying/weiyuan/emp01-03/wanggong = `12345678`（首次登录强制改密）

## 四、待办 / 已知

1. 部署相关文件（`backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf`、`frontend/vercel.json`、`.env.production` 改动）均在本地**未提交 git**，需用户确认是否入库
2. `backend/railway-init/` 临时建表执行器目录可删
3. Railway token 过期需 `railway login` 刷新；新建 GHCR 包默认私有需设 Public
4. 生产库是 `railway`（非 `perfflow`），后续结构升级需对应 SQL
