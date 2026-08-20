PerfFlow 绩效考核系统 - Agent 初始提示词（完整版）
一、项目信息
项目 内容
项目名称 PerfFlow
架构模式 前后端分离
后端框架 Spring Boot 3.3.4
后端语言 Java 17
构建工具 Maven
持久层 MyBatis-Plus 3.5.9
安全框架 Spring Security + JJWT 0.12.6
API文档 springdoc-openapi 2.6.0
数据库 MySQL 8.0+
数据库名 perfflow
工具库 Hutool（仅用于Excel导出）
测试框架 JUnit 5 + Mockito + H2
二、前端技术栈（仅参考，不实现）
项目 内容
框架 Vue 3 或 React
构建工具 Vite
状态管理 Pinia 或 Redux Toolkit
HTTP请求 Axios
路由 Vue Router 或 React Router
前端独立部署，通过 /api 代理调用后端接口。
Agent 本次仅生成后端代码，前端由另一个 Agent 或人工实现。

三、项目目录结构（后端）
text
D:\workspace\perfflow\
│
├── backend/ # 后端项目（Spring Boot）
│ ├── pom.xml
│ ├── src/
│ │ └── main/
│ │ ├── java/com/perfflow/
│ │ │ ├── PerfFlowApplication.java
│ │ │ ├── common/ # 通用：异常/响应/常量
│ │ │ │ ├── api/
│ │ │ │ │ ├── Result.java
│ │ │ │ │ └── ResultCode.java
│ │ │ │ ├── exception/
│ │ │ │ │ ├── BizException.java
│ │ │ │ │ └── GlobalExceptionHandler.java
│ │ │ │ ├── constant/
│ │ │ │ │ ├── RoleConst.java
│ │ │ │ │ └── AssessmentStateConst.java
│ │ │ │ └── utils/
│ │ │ │ └── JwtUtil.java
│ │ │ ├── config/
│ │ │ │ ├── SecurityConfig.java
│ │ │ │ ├── JwtAuthenticationFilter.java
│ │ │ │ ├── MybatisPlusConfig.java
│ │ │ │ ├── OpenApiConfig.java
│ │ │ │ ├── CorsConfig.java
│ │ │ │ └── MetaObjectHandlerConfig.java
│ │ │ ├── module/
│ │ │ │ ├── auth/ # 登录/刷新/当前用户
│ │ │ │ │ ├── controller/AuthController.java
│ │ │ │ │ ├── service/AuthService.java
│ │ │ │ │ └── dto/LoginReq.java / LoginResp.java
│ │ │ │ ├── system/ # 用户/部门管理（仅管理员）
│ │ │ │ │ ├── controller/SysUserController.java
│ │ │ │ │ ├── controller/SysDepartmentController.java
│ │ │ │ │ ├── service/SysUserService.java
│ │ │ │ │ ├── service/SysDepartmentService.java
│ │ │ │ │ ├── entity/SysUser.java
│ │ │ │ │ ├── entity/SysDepartment.java
│ │ │ │ │ ├── mapper/SysUserMapper.java
│ │ │ │ │ └── mapper/SysDepartmentMapper.java
│ │ │ │ ├── period/ # 考核周期
│ │ │ │ │ ├── controller/AssessmentPeriodController.java
│ │ │ │ │ ├── service/AssessmentPeriodService.java
│ │ │ │ │ ├── entity/AssessmentPeriod.java
│ │ │ │ │ └── mapper/AssessmentPeriodMapper.java
│ │ │ │ └── assessment/ # 考核主表/行/流程/导出
│ │ │ │ ├── controller/
│ │ │ │ │ ├── AssessmentTableController.java
│ │ │ │ │ ├── AssessmentRowController.java
│ │ │ │ │ ├── AssessmentFlowController.java
│ │ │ │ │ └── AssessmentExportController.java
│ │ │ │ ├── service/
│ │ │ │ │ ├── AssessmentTableService.java
│ │ │ │ │ ├── AssessmentRowService.java
│ │ │ │ │ ├── AssessmentFlowService.java
│ │ │ │ │ ├── AssessmentCalcService.java
│ │ │ │ │ ├── AssessmentStateMachine.java
│ │ │ │ │ ├── AssessmentExportService.java
│ │ │ │ │ └── PendingReminderService.java
│ │ │ │ ├── entity/
│ │ │ │ │ ├── AssessmentTable.java
│ │ │ │ │ ├── AssessmentRow.java
│ │ │ │ │ └── AssessmentFlowLog.java
│ │ │ │ └── mapper/
│ │ │ │ ├── AssessmentTableMapper.java
│ │ │ │ ├── AssessmentRowMapper.java
│ │ │ │ └── AssessmentFlowLogMapper.java
│ │ │ └── task/
│ │ │ └── PendingScheduleTask.java
│ │ └── resources/
│ │ ├── application.yml
│ │ ├── application-dev.yml
│ │ ├── application-prod.yml
│ │ ├── logback-spring.xml
│ │ └── mapper/
│ │ └── （MyBatis-Plus XML 文件）
│ └── target/
│
├── frontend/ # 前端项目（由前端Agent实现，本次忽略）
│ └── src/
│
├── docs/ # 文档目录
│ ├── 01*数据库DDL.sql
│ ├── 02*枚举常量定义.md
│ ├── 03*接口文档.yaml
│ ├── 04*状态机流转图.puml
│ ├── 05*Mock数据初始化.sql
│ ├── 06*核心计算规则.md
│ └── 07*异常场景清单.md
│
├── sql/ # SQL执行脚本
│ ├── schema.sql
│ └── seed_data.sql
│
└── README.md
四、数据库表（6张）
序号 表名 说明
1 sys_department 部门表
2 sys_user 用户表（含角色）
3 assessment_period 考核周期表
4 assessment_table 考核主表
5 assessment_row 考核行明细表（10行）
6 assessment_flow_log 流程日志表
完整DDL见 docs/01*数据库DDL.sql

所有表引擎 InnoDB，字符集 utf8mb4_0900_ai_ci

通用字段：id、created_at、updated_at

五、角色定义（5种）
编码 角色名 说明
ROLE_EMP 员工 填报表格，提交后只对自己的表（除考核得分外）可读
ROLE_DEPT_LEAD 部门领导 本部门所有员工表可读，考核得分可读写，自己的表脱敏
ROLE_LEAD 领导 所有部门所有员工表可读，有专属领导评分功能
ROLE_HR 人事 所有员工表可读（只读），后台不可见
ROLE_ADMIN 系统管理员 业务数据绝对不可见，仅后台用户管理可见
六、角色权限矩阵
角色 可见数据 可写操作 后台管理可见
员工 仅自己的表（考核得分、考核结果不可见） 填报除考核得分外字段，挂起前可改 ❌
部门领导 本部门所有员工表 考核得分列（调分必填注释） ❌
领导 全公司所有员工表 领导评分 ❌
人事 全公司完整数据 只读 + 确认推送 + 延长挂起时间 ❌
系统管理员 无业务数据 账号增删改查 ✅
七、表格结构
横向字段（10行数据）
字段 类型 说明
考核项目 string 分类名称
序号 int 1-10
指标名称 string 员工填写
指标分数 int 基础分
工作目标 string 员工填写
评分标准 string 员工填写
完成情况 %int 员工填写，0-150
考核得分 decimal 基础分 × 完成率 / 100
考核结果 string A-D
纵向分类（3大类）
分类 行号 分值
个人季度工作计划 第1-5行 80分
开放型指标 第6-7行 20分
加减分项 第8-10行 加分上限10分（员工不填）
八、状态机（5个状态）
状态码 状态名 说明
1 自评中（SELF_SCORING） 员工可填报/修改
2 自评挂起（FROZEN） 员工只读，不可修改
3 部门审核（DEPT_REVIEW） 部门领导审核/调分
4 领导评分（LEADER_SCORING） 领导进行评分
5 已完成（COMPLETED） 流程结束，只读
状态流转图
text
[SELF_SCORING] → 员工提交 → [FROZEN]
[FROZEN] → 人事确认推送 → [DEPT_REVIEW]
[DEPT_REVIEW] → 部门领导通过 → [LEADER_SCORING]
[DEPT_REVIEW] → 部门领导打回 → [SELF_SCORING]
[LEADER_SCORING] → 领导提交评分 → [COMPLETED]
特殊规则
挂起结束前3天，首页置顶提醒

挂起结束后自动提交（但推送取决于人事）

系统管理员和人事可延长挂起时间

部门领导调分必须在 adjust_remark 写原因（DB可见，前端隐藏）

九、核心计算规则

1. 行自评分
   text
   self_score = (base_score × completion_rate) / 100
   completion_rate：0-150

结果四舍五入保留2位小数

2. 自评总分
   text
   self_total_score = SUM(所有行的self_score)
3. 最终得分
   text
   final_score = self_total_score × 0.5 + leader_score × 0.5
4. 等级生成
   分数区间 等级
   > = 90 A
   > = 80 且 < 90 B
   > = 70 且 < 80 C
   > < 70 D
   > 十、接口设计（RESTful，前缀 /api）
   > 10.1 认证模块 /api/auth
   > 方法 路径 角色 说明
   > POST /auth/login 匿名 登录，返回 JWT
   > POST /auth/refresh 任意 刷新 Token
   > POST /auth/logout 任意 登出
   > GET /auth/me 任意 当前用户信息
   > 10.2 员工端 /api/self
   > 方法 路径 角色 说明
   > GET /self/myTable 员工 查看自己的表（脱敏）
   > PUT /self/rows/{rowId} 员工 保存某行（挂起前可改）
   > POST /self/submit 员工 提交自评
   > 10.3 部门领导端 /api/dept
   > 方法 路径 角色 说明
   > GET /dept/teamTables 部门领导 查看本部门所有表
   > POST /dept/confirm 部门领导 确认通过
   > POST /dept/reject 部门领导 打回
   > POST /dept/adjustScore 部门领导 调分（必填注释）
   > 10.4 领导端 /api/leader
   > 方法 路径 角色 说明
   > GET /leader/allTables 领导 查看所有表
   > POST /leader/score 领导 提交领导评分
   > 10.5 人事端 /api/hr
   > 方法 路径 角色 说明
   > GET /hr/allFinalTables 人事 查看完整汇总表
   > POST /hr/pushToDept 人事 确认推送
   > POST /hr/extendFreeze 人事 延长挂起时间
   > GET /hr/export 人事 导出汇总表（Excel）
   > GET /hr/print 人事 打印友好HTML
   > 10.6 系统管理员端 /api/admin
   > 方法 路径 角色 说明
   > GET /admin/users 管理员 用户列表
   > POST /admin/users 管理员 新增用户
   > PUT /admin/users/{id} 管理员 修改用户
   > DELETE /admin/users/{id} 管理员 删除用户
   > POST /admin/users/{id}/reset-password 管理员 重置密码
   > GET /admin/departments 管理员 部门列表
   > POST /admin/departments 管理员 新增部门
   > PUT /admin/departments/{id} 管理员 修改部门
   > DELETE /admin/departments/{id} 管理员 删除部门
   > 10.7 考核周期 /api/periods
   > 方法 路径 角色 说明
   > GET /periods 任意已登录 周期列表
   > GET /periods/current 任意已登录 当前周期
   > POST /periods 管理员 新建周期
   > POST /periods/{id}/open 管理员 开启周期（批量创建考核表）
   > POST /periods/{id}/close 管理员 关闭周期
   > 10.8 首页提醒
   > 方法 路径 角色 说明
   > GET /home/reminders 任意已登录 待办提醒 + 挂起到期提醒
   > 十一、安全方案
   > 认证流程
   > /auth/login 通过 BCrypt 校验密码

签发 accessToken（2小时）+ refreshToken（7天）

JwtAuthenticationFilter 解析 Header，填充 SecurityContext

DataScopeInterceptor 将当前用户信息写入 RequestContextHolder

权限控制
粗粒度：@PreAuthorize("hasRole('HR')")

细粒度：@DataScope 注解 + AOP 拼接 SQL 数据范围条件

管理员访问业务接口：统一返回 403 ADMIN_NO_BUSINESS_VISIBILITY

配置要点
CSRF 禁用

Session 无状态

白名单：/api/auth/login、/api/auth/refresh、/swagger-ui/**、/v3/api-docs/**

十二、Spring Boot 文件创建约束（强制清单）
12.1 必须创建的文件
启动类与配置（6个）

文件 路径
PerfFlowApplication.java com.perfflow
application.yml src/main/resources
application-dev.yml src/main/resources
application-prod.yml src/main/resources
logback-spring.xml src/main/resources
pom.xml 项目根目录
通用层（7个）

文件 路径
Result.java com.perfflow.common.api
ResultCode.java com.perfflow.common.api
BizException.java com.perfflow.common.exception
GlobalExceptionHandler.java com.perfflow.common.exception
RoleConst.java com.perfflow.common.constant
AssessmentStateConst.java com.perfflow.common.constant
JwtUtil.java com.perfflow.common.utils
配置层（6个）

文件 路径
SecurityConfig.java com.perfflow.config
JwtAuthenticationFilter.java com.perfflow.config
MybatisPlusConfig.java com.perfflow.config
OpenApiConfig.java com.perfflow.config
CorsConfig.java com.perfflow.config
MetaObjectHandlerConfig.java com.perfflow.config
模块层（按6张表对应）

模块 必须创建
auth AuthController、AuthService、LoginReq、LoginResp
system SysUserController、SysUserService、SysUser、SysUserMapper
system SysDepartmentController、SysDepartmentService、SysDepartment、SysDepartmentMapper
period AssessmentPeriodController、AssessmentPeriodService、AssessmentPeriod、AssessmentPeriodMapper
assessment AssessmentTableController、AssessmentTableService、AssessmentTable、AssessmentTableMapper
assessment AssessmentRowController、AssessmentRowService、AssessmentRow、AssessmentRowMapper
assessment AssessmentFlowLogController、AssessmentFlowLogService、AssessmentFlowLog、AssessmentFlowLogMapper
assessment AssessmentCalcService（计算服务）
assessment AssessmentStateMachine（状态机）
assessment AssessmentExportService（导出服务）
assessment PendingReminderService（提醒服务）
定时任务（1个）

文件 路径
PendingScheduleTask.java com.perfflow.task
12.2 禁止创建的内容
禁止项 说明
❌ @Controller 返回HTML 只能用 @RestController，禁止返回视图
❌ Thymeleaf依赖 禁止引入 spring-boot-starter-thymeleaf
❌ JSP相关 禁止任何JSP配置
❌ ModelAndView 禁止使用
❌ @ResponseBody 重复标注 @RestController 已包含，无需重复
❌ 前端静态资源 src/main/resources/static 和 templates 禁止创建
❌ 多个SpringBoot启动类 只能有1个 @SpringBootApplication
❌ 循环依赖 禁止Service之间互相注入
❌ @Autowired 字段注入 强制使用构造器注入
❌ 硬编码角色/状态值 必须使用枚举常量
❌ SELECT \* 所有查询必须明确字段列表
12.3 POM依赖控制
依赖 必须 禁止
spring-boot-starter-web ✅
spring-boot-starter-security ✅
spring-boot-starter-validation ✅
mybatis-plus-spring-boot3-starter ✅
mysql-connector-j ✅
jjwt-api + jjwt-impl + jjwt-jackson ✅
springdoc-openapi-starter-webmvc-ui ✅
lombok ✅
hutool-all ✅
spring-boot-starter-test ✅
spring-boot-starter-thymeleaf ❌
spring-boot-starter-webflux ❌
12.4 代码风格约束
约束 说明
Controller 只做参数校验和调用Service，不写业务逻辑
Service 全部业务逻辑 + @Transactional
Mapper 继承 BaseMapper<T>，不写业务
DTO 入参用 XxxReq，出参用 XxxResp，不暴露Entity
包名 全小写，com.perfflow.module.模块名
类名 UpperCamelCase
方法/变量 lowerCamelCase
常量 UPPER_SNAKE_CASE
12.5 创建顺序（按优先级）
批次 内容
第1批 pom.xml → application.yml → 启动类
第2批 Result.java → BizException.java → GlobalExceptionHandler.java
第3批 所有Entity → 所有Mapper
第4批 SecurityConfig → JwtUtil → JwtAuthenticationFilter
第5批 所有Service → 所有Controller
第6批 状态机 → 计算服务 → 导出服务 → 定时任务
第7批 Swagger配置 → CORS配置
十三、编码规范
类名：UpperCamelCase

方法/变量：lowerCamelCase

常量：UPPER_SNAKE_CASE

包名：全小写

分层：Controller → Service → Mapper，DTO隔离Entity

异常：统一 BizException + @RestControllerAdvice

日志：关键操作必须记录（登录、提交、确认、调分、评分）

SQL：禁止 SELECT \*，更新必有索引条件

事务：Service层使用 @Transactional

十四、交付标准
代码要求
□ 所有表生成对应的实体类（Lombok + MyBatis-Plus注解）
□ Mapper接口继承 BaseMapper<T>
□ Service层实现完整业务逻辑
□ Controller层提供RESTful接口
□ Spring Security + JWT 实现认证授权
□ 权限拦截器严格按角色控制
□ 状态机流转完整
□ 所有计算规则正确实现
文档要求
□ 提供 application.yml 配置示例
□ Swagger接口文档可访问（/swagger-ui.html）
□ 提供初始化SQL脚本（含Mock数据）
测试要求
□ 所有接口通过 Swagger/Postman 可测
□ 状态机全分支覆盖
□ 行级单测覆盖率 ≥ 70%
