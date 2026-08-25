# PerfFlow 绩效考核系统

企业季度岗位绩效考核系统：发布周期 → Excel 导入考核明细 → 员工自评 → 部门审核 → 领导评分 → 自动计算总分与考核结果。

前后端分离，Spring Boot 3 + Vue 3，JWT 鉴权，五角色权限隔离。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3、Java 17、MyBatis-Plus、Spring Security、jjwt、Hutool-POI |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia |
| 数据库 | MySQL 8.0+ |

## 核心特性

- **五角色隔离**：系统管理员 / 绩效考核管理员 / 部门负责人 / 领导 / 员工，各角色仅见自己环节的数据
- **Excel 导入导出**：HR 用 Excel 一次导入「员工名单 + 考核明细」，导出模板 A1:G16 格式（自动换行、列宽自适应）
- **自动计分**：自评得分 = 指标分数 × 完成率%；总分 = 50% 自评 + 50% 领导评分；考核结果 A≥90 / B≥75 / C≥60 / D<60
- **单账号互踢**：同一账号新设备登录自动使旧设备失效（JWT 令牌版本号）
- **权限双保险**：前端仅控制展示，后端 `@PreAuthorize` + 数据权限 `scopeOf` 独立鉴权

## 快速开始

```bash
# 1. 初始化数据库（建库 + 表 + 种子数据）
mysql -u root -p < backend/sql/perfflow.sql

# 2. 配置数据库连接
#    编辑 backend/src/main/resources/application-dev.yml 的 url/username/password

# 3. 启动后端
cd backend
mvn -DskipTests package
java -jar -Dspring.profiles.active=dev target/perfflow-backend.jar

# 4. 启动前端
cd frontend
npm install
npm run dev   # 访问 http://localhost:5173
```

接口文档：http://localhost:8080/api/swagger-ui.html

## 默认账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 系统管理员 | `admin` | `12345678` |
| 绩效考核管理员 | `hr` | `12345678` |
| 部门负责人 | `bumen1` / `bumen2` | `Init@123456` |
| 领导 | `leader` | `12345678` |
| 员工 | `emp01` / `emp02` / `emp03` / `wanggong` | `12345678` |

> 首次登录可能提示修改密码。

## 业务流程

```
发布周期 → Excel 导入员工与考核明细 → 员工填完成率并提交
→ 绩效考核管理员推送 → 部门负责人审核（可改自评得分/加减分项）→ 提交给领导
→ 领导评分 → 生成最终得分与考核结果
```

## 安全说明

- 前端路由/按钮仅为展示控制，**所有权限判断在后端独立实现**（`@PreAuthorize` 方法级 + 数据权限过滤）
- 自评挂起阶段仅本人与绩效考核管理员可见
- 管理员不可访问业务接口（403）
- 管理员不可创建/查看/操作绩效考核管理员账号

## 目录结构

```
perftlow/
├── backend/            # Spring Boot 后端
│   ├── sql/perfflow.sql
│   └── src/main/java/com/perfflow/
│       ├── module/     # auth / system / period / assessment / home
│       ├── security/   # JWT 过滤器 / 数据权限
│       └── common/     # 统一响应 / 错误码 / 全局异常
└── frontend/           # Vue3 前端
    └── src/
        ├── api/        # Axios 封装
        ├── views/      # 各角色页面
        ├── store/      # Pinia
        └── router/     # 路由 + 权限守卫
```

## 贡献

欢迎提交 Issue 与 PR。代码遵循《阿里巴巴 Java 开发手册》。
