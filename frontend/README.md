# PerfFlow 前端

> 企业绩效考核管理平台前端，对应后端 `E:\workspace\perftlow\backend`。
>
> 风格：Vue 3 + TypeScript + Element Plus + Pinia + Vue Router，商务简约配色（蓝/灰白）。

## 1. 技术栈

| 维度 | 选型 |
| --- | --- |
| 框架 | Vue 3.5 + `<script setup lang="ts">` |
| 构建 | Vite 6 |
| 包管理 | 推荐 pnpm / npm |
| 组件库 | Element Plus 2.9（按需自动导入） |
| 路由 | Vue Router 4 |
| 状态 | Pinia 2 |
| 类型 | TypeScript 5.7，路径别名 `@/* → src/*` |
| 样式 | SCSS（全局变量 + scoped），商务简约风格 |
| HTTP | 自研 `utils/request.ts`（fetch + JWT） |

## 2. 环境要求

- Node.js 18+（推荐 20 LTS 或 22）
- npm / pnpm（任一）
- 后端服务：`E:\workspace\perftlow\backend` 启动并监听 8080

## 3. 快速开始

```bash
cd E:\workspace\perftlow\frontend
npm install
npm run dev
# 浏览器打开 http://localhost:5173
```

### 3.1 默认账号（来自后端种子数据，密码统一 `Init@123456`）

| 登录名 | 角色 |
| --- | --- |
| `admin` | 系统管理员 |
| `hr` | 人事 |
| `leader` | 公司领导 |
| `dept_lead` / `dept_lead2` | 部门领导 |
| `emp01` / `emp02` / `emp03` | 员工 |

### 3.2 后端未启动

登录页直接提交会因 CORS 或 404 失败。打开 `vite.config.ts` 的代理：

```ts
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true }
}
```

后端启动后一切正常联调。

## 4. 目录结构

```
frontend/
├── api/                        接口定义（与 types/ 对齐）
│   ├── client.ts              fetch + JWT + Result<T> 封装
│   ├── auth.api.ts
│   ├── period.api.ts
│   ├── assessment.api.ts
│   ├── system.api.ts
│   └── home.api.ts
├── types/                      类型定义（Result / enums / dto）
├── docs/                       接口说明、字段脱敏、Mock 样例
├── examples/                   接入流程示例
├── openapi/                    OpenAPI 3.0 JSON
├── public/                     静态资源
├── src/
│   ├── main.ts                 入口
│   ├── App.vue
│   ├── router/                 路由 + 角色守卫
│   ├── store/                  Pinia: auth / app
│   ├── layout/                 AppLayout / Sidebar / TopBar / Breadcrumb
│   ├── components/common/      通用组件（StateTag / BasePagination / PageHeader ...）
│   ├── composables/            useTable / useForm / useDialog / useAuth
│   ├── utils/                  request / format / validate / download / message
│   ├── styles/                 全局 SCSS 变量与重置
│   ├── views/                  业务页面
│   │   ├── login/             登录 + 修改密码
│   │   ├── error/             403 / 404
│   │   ├── home/              工作台首页（含 StatCard / ReminderList）
│   │   ├── profile/           个人中心
│   │   ├── common/            考核主表详情（多角色复用）
│   │   ├── me/                员工自评（含 AssessmentRow）
│   │   ├── dept-lead/         部门领导审核
│   │   ├── lead/              公司领导评分
│   │   ├── hr/                周期管理 + 考核列表
│   │   └── admin/             用户 / 部门 / 系统概览
│   └── types/role.ts           角色工具函数（isAdmin / 角色导航）
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
└── ## Vue3 编码风格规范 (必须严格遵守).md
```

## 5. 编码规范摘要（详见 `## Vue3 编码风格规范 (必须严格遵守).md`）

- **组件名**：PascalCase（`UserProfile.vue`），模板中使用 `<user-profile />`
- **`<script setup lang="ts">`**：导入 → Props/Emits → 响应式状态 → computed → 方法 → 生命周期
- **Props/Emits**：TS 类型化，必填时使用 `withDefaults` 设默认值
- **v-for 必须有 `:key`**；**v-if 与 v-for 不可同元素**
- **TypeScript**：对象用 `interface`，联合/别名用 `type`，禁止 `any`
- **样式**：`<style scoped lang="scss">`，类名 kebab-case，BEM 风格
- **错误**：统一在 `utils/request.ts` 中抛出 `ApiError`，`utils/message.ts` 提供 `toast` 体验

## 6. 角色与路由矩阵

| 路径 | EMP | DEPT_LEAD | LEAD | HR | ADMIN |
| --- | --- | --- | --- | --- | --- |
| `/home`、`/profile` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `/me/assessment` | ✔ |  |  |  |  |
| `/dept/review` |  | ✔ |  |  |  |
| `/lead/score` |  |  | ✔ |  |  |
| `/hr/period` |  |  |  | ✔ | ✔ |
| `/hr/list` |  |  | ✔ | ✔ |  |
| `/admin/*` |  |  |  |  | ✔ |

> ADMIN 访问业务接口（`/api/assessment-tables` 等）会被 `AdminBusinessGuardInterceptor` 拒绝；
> 前端路由通过 `router/index.ts` 中的 `beforeEach` 守卫做了二次校验。

## 7. 开发常见问题

**Q: 启动后白屏？**
A: 检查 Vite 的代理配置与后端 8080 端口；或在 `request.ts` 中将 `API_BASE` 临时替换为绝对地址。

**Q: 字段全部显示 `—`？**
A: 你正在使用 `EMP` / `DEPT_LEAD` 账号；前端按后端 `masked=true` 渲染脱敏占位符。

**Q: 系统管理员访问业务页？**
A: 路由层会拦截并跳到 `/403`。如确需浏览业务，请切换为 HR/EMP 账号登录。

**Q: 如何按业务码精确处理错误？**
A: 在 `utils/request.ts` 的 `ApiError.code` 中读取，对照 `types/result.ts:ResultCode`。
