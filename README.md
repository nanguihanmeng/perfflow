# PerfFlow 绩效考核系统

> 企业季度岗位绩效考核系统：人事发起周期 → HR 导入考核明细 → 员工自评 → 部门领导审核 → 领导评分 → 自动计算总分与考核结果。
>
> 前端 Vue3 + 后端 Spring Boot 3，前后端分离，JWT 鉴权，五角色权限隔离。

---

## 一、系统功能总览

### 角色与职责

| 角色 | 账号示例 | 主要功能 |
|---|---|---|
| **ADMIN** 系统管理员 | `admin` | 用户管理、部门管理（**不参与考核**，也看不到业务数据） |
| **HR** 人事 | `hr` | 新建/开启考核周期（勾选参与员工）、导入考核明细 Excel、推送考核表、删除考核表、导出 Excel/打印 |
| **DEPT_LEAD** 部门负责人 | `bumen1` / `bumen2` | 部门审核：直接编辑员工自评得分、提交给领导、打回 |
| **LEAD** 领导 | `leader` | 领导评分（0-100），生成最终结果 |
| **EMP** 员工 | `emp01` `emp02` `emp03` `wanggong` | 填写完成率、填写岗位、提交自评、查看自己的考核表 |

### 核心业务流程

```
HR 新建周期 → 开启周期(勾选员工) → HR 导入考核明细 Excel(指标/分数)
→ 员工填写完成率(0-100) 提交
→ HR 推送 → 部门负责人审核(可改自评得分) → 提交给领导
→ 领导评分 → 生成最终得分与考核结果
```

### 关键业务规则

- **自评得分** = 指标分数 × 完成率%（自动计算，完成率范围 0-100）
- **部门负责人**：由角色 `DEPT_LEAD` 决定，**每个部门有且只有一个**；考核表信息栏"部门负责人"自动实时显示该部门负责人姓名
- **自评得分可见性**：员工本人 / 本部门负责人 / 领导 / HR 可见；其他员工、其他部门领导不可见
- **考核结果**（原"行结果"）：任何人不可读写，只跟总分相关：
  - 总分 = **50% 自评总分 + 50% 领导评分**
  - A ≥ 90，B ≥ 75，C ≥ 60，D < 60
- **考核明细**：由 HR 通过 Excel（模板与《岗位季度绩效考核表》A1:G16 一致）导入，员工**不可手填**指标；指标分数默认 0，由 HR 导入时填写
- **参与考核范围**：仅 `EMP` 角色生成考核表；HR / ADMIN / LEAD / DEPT_LEAD 均不参与考核
- **管理员隔离**：ADMIN 账号访问业务接口（考核/周期/首页提醒）会被拦截（403）

---

## 二、技术栈

| 模块 | 选型 |
|---|---|
| 后端 | Spring Boot 3.3.4、Java 17、MyBatis-Plus 3.5.9、Spring Security 6、JWT (jjwt)、springdoc-openapi |
| 前端 | Vue 3、TypeScript、Vite 6、Element Plus、Pinia、Axios |
| 数据库 | MySQL 8.0+ |
| Excel 导入导出 | Hutool-poi + Apache POI |

---

## 三、目录结构

```
perftlow/
├── README.md                    # 本文件
├── TECHNICAL_DESIGN.md          # 技术设计文档
├── start-backend.bat            # Windows 一键启动后端
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   ├── sql/perfflow.sql         # 建库 + DDL + 种子数据
│   ├── logs/                    # 运行日志
│   └── src/main/java/com/perfflow/
│       ├── common/              # 统一响应 Result / 错误码 / 全局异常
│       ├── config/              # Security / MyBatisPlus / OpenAPI / 拦截器
│       ├── security/            # JWT 过滤器 / 数据权限上下文
│       ├── module/
│       │   ├── auth/            # 登录 / 刷新 / 改密
│       │   ├── system/          # 用户 / 部门（ADMIN）
│       │   ├── period/          # 考核周期（HR）
│       │   ├── assessment/      # 考核表 / 行 / 导入导出 / 计算 / 状态机
│       │   └── home/            # 首页提醒
│       └── task/                # 定时任务（挂起自动推送）
└── frontend/                    # Vue3 前端
    ├── package.json
    ├── vite.config.ts           # 开发代理 /api → localhost:8080
    └── src/
        ├── api/                 # Axios API 封装
        ├── views/               # 各角色页面（登录/工作台/周期管理/考核列表/部门审核/领导评分/我的考核/用户管理…）
        ├── store/               # Pinia（登录态）
        ├── router/              # 路由 + 权限守卫
        └── utils/               # request / token / 校验 / 提示
```

---

## 四、快速开始

### 1. 初始化数据库

```bash
# 在 MySQL 中执行建库脚本（会创建 perfflow 库 + 表 + 种子数据）
mysql -u root -p < backend/sql/perfflow.sql
```

### 2. 配置数据库连接

编辑 `backend/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/perfflow?...
    username: root
    password: 你的MySQL密码   # ← 改成实际密码
```

### 3. 启动后端

```bash
# 方式一：Windows 一键脚本
start-backend.bat

# 方式二：手动
cd backend
mvn -DskipTests package
java -jar -Dspring.profiles.active=dev target/perfflow-backend.jar
```

启动后访问接口文档：http://localhost:8080/api/swagger-ui.html

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：http://localhost:5173

---

## 五、默认账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 系统管理员 | `admin` | `12345678` |
| 人事 | `hr` | `12345678` |
| 领导 | `leader` | `Init@123456` |
| 部门负责人（技术部） | `bumen1` | `Init@123456` |
| 部门负责人（产品部） | `bumen2` | `Init@123456` |
| 员工 | `emp01` / `emp02` / `emp03` | `Init@123456` |
| 员工 | `wanggong` | `12345678` |

> 部分账号首次登录会提示修改密码（`mustChangePassword`），按提示设置新密码即可。

---

## 六、Git：改完代码如何提交并推送

项目托管在 GitHub（`origin` → `https://github.com/nanguihanmeng/perfflow`），默认分支 **main**。

### 提交推送三板斧

```bash
cd E:\workspace\perftlow

# 1. 查看改了什么（红色 = 已修改未暂存）
git status

# 2. 把改动加入暂存区（. 表示全部；也可 git add 指定文件）
git add .

# 3. 提交（-m 后面写本次改动的说明，方便以后回溯）
git commit -m "feat: 新增xxx功能 / fix: 修复xxx问题"

# 4. 推送到 GitHub 的 main 分支
git push origin main
```

### 常用辅助命令

| 命令 | 作用 |
|---|---|
| `git status` | 查看当前改动 |
| `git diff` | 查看具体改动内容 |
| `git add 文件名` | 只暂存某个文件 |
| `git log --oneline` | 查看提交历史 |
| `git pull` | 拉取远端最新代码（多人协作时推送前先 pull） |

### 建议的提交信息规范

- 新功能：`feat: 描述`
- 修 bug：`fix: 描述`
- 文档：`docs: 描述`
- 重构：`refactor: 描述`

### 注意事项

- **构建产物不会被推送**：`backend/target`、`frontend/node_modules`、`frontend/dist`、`logs/` 已在 `.gitignore` 中忽略，无需手动处理
- **推送前确认在 main 分支**：`git branch` 应显示 `* main`；若不在，先 `git checkout main`
- 第一次推送若提示需要登录 GitHub，会弹出浏览器授权，按提示完成即可

---

## 七、常见问题

**Q：管理员能看考核业务吗？**
A：不能。ADMIN 访问 `/api/assessment-tables`、`/api/periods`、`/api/home` 会被拦截返回 403。

**Q：为什么员工看不到考核表？**
A：考核表只在 HR"开启周期"时按**勾选的员工**生成。周期开启后新加入的员工不参与当期考核（下次开启时勾上即可）。

**Q：完成率能填多少？**
A：0-100。自评得分 = 指标分数 × 完成率%，超出会报错。

**Q：部门负责人如何设置？**
A：用户管理中把某员工角色设为 `部门负责人` 即可；每个部门只能有一个，已存在会提示。

**Q：登录报"系统开小差"？**
A：多为后端连不上数据库（`application-dev.yml` 密码错误）或后端未启动，查看 `backend/logs/perfflow-dev.log` 定位。
