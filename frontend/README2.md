一、项目信息
项目 内容
项目名称 PerfFlow（绩效考核系统）
架构模式 前后端分离
工作目录 D:\workspace\perfflow\frontend（已存在，请直接使用）
框架 Vue 3 + Vite
语言 TypeScript
状态管理 Pinia
UI组件库 Element Plus
HTTP请求 Axios
路由 Vue Router 4
后端接口地址 http://localhost:8080/api
API代理 Vite代理 /api 到 http://localhost:8080
注意：D:\workspace\perfflow\frontend 目录已创建，请直接在该目录下生成代码，不要创建新的项目目录。

二、目录结构（已约定，必须遵守）
text
D:\workspace\perfflow\frontend\ # 工作目录（已存在）
│
├── index.html
├── package.json
├── pnpm-lock.yaml
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── .env.development
├── .env.production
├── .eslintrc.cjs
├── .prettierrc.json
│
├── public/
│ └── favicon.ico
│
└── src/
├── main.ts # 入口文件
├── App.vue # 根组件
├── shims-vue.d.ts
│
├── api/ # 接口定义
│ ├── index.ts # axios实例（拦截器）
│ ├── auth.ts # 登录/刷新/退出
│ ├── self.ts # 员工端接口
│ ├── dept.ts # 部门领导端接口
│ ├── leader.ts # 领导端接口
│ ├── hr.ts # 人事端接口
│ ├── admin.ts # 管理员端接口
│ └── period.ts # 考核周期接口
│
├── assets/ # 静态资源
│ ├── styles/
│ │ ├── index.scss # 全局样式
│ │ └── variables.scss # 样式变量
│ └── images/
│
├── components/ # 公共组件
│ ├── common/
│ │ ├── AppHeader.vue # 顶部导航
│ │ ├── AppSidebar.vue # 侧边菜单
│ │ ├── AppFooter.vue # 底部
│ │ └── Breadcrumb.vue # 面包屑
│ ├── assessment/
│ │ ├── AssessmentTable.vue # 考核表组件
│ │ ├── AssessmentRow.vue # 考核行组件
│ │ ├── ScoreDisplay.vue # 分数展示
│ │ └── GradeBadge.vue # 等级徽章
│ └── layout/
│ ├── DefaultLayout.vue # 默认布局
│ └── BlankLayout.vue # 空白布局（登录页用）
│
├── composables/ # 组合式函数
│ ├── useAuth.ts # 认证状态
│ ├── useTable.ts # 考核表操作
│ └── useReminder.ts # 提醒功能
│
├── router/ # 路由配置
│ ├── index.ts # 路由实例
│ ├── routes.ts # 路由定义
│ └── guards.ts # 路由守卫（权限控制）
│
├── stores/ # Pinia状态管理
│ ├── index.ts # store入口
│ ├── user.ts # 用户信息
│ ├── auth.ts # 认证状态
│ └── assessment.ts # 考核数据
│
├── types/ # TypeScript类型定义
│ ├── user.d.ts # 用户类型
│ ├── assessment.d.ts # 考核类型
│ ├── api.d.ts # API响应类型
│ └── enums.d.ts # 枚举类型
│
├── utils/ # 工具函数
│ ├── request.ts # Axios封装
│ ├── token.ts # Token管理
│ ├── validator.ts # 表单校验
│ ├── format.ts # 格式化工具
│ └── constants.ts # 常量定义
│
└── views/ # 页面视图
├── login/
│ └── Login.vue # 登录页
├── employee/
│ ├── SelfTable.vue # 我的考核表
│ └── SelfEdit.vue # 填报/编辑
├── deptLeader/
│ ├── TeamTables.vue # 部门所有表
│ └── ReviewDetail.vue # 审核详情
├── leader/
│ ├── AllTables.vue # 所有表
│ └── LeaderScore.vue # 领导评分
├── hr/
│ ├── AllFinalTables.vue # 完整汇总表
│ ├── PushConfirm.vue # 推送确认
│ └── ExportPrint.vue # 导出/打印
├── admin/
│ ├── UserManage.vue # 用户管理
│ └── DeptManage.vue # 部门管理
└── error/
├── 401.vue
├── 403.vue
└── 404.vue
三、文件命名规范
类型 命名规则 示例
Vue组件 PascalCase.vue AssessmentTable.vue
页面视图 PascalCase.vue SelfTable.vue
工具函数 camelCase.ts format.ts
API接口 camelCase.ts self.ts
类型定义 camelCase.d.ts user.d.ts
组合式函数 useCamelCase.ts useAuth.ts
常量文件 camelCase.ts constants.ts
Store文件 camelCase.ts user.ts
样式文件 kebab-case.scss variables.scss
四、代码风格规范
4.1 Vue组件规范
vue
<template>

  <!-- 模板内容 -->
</template>

<script setup lang="ts">
// 1. 导入
// 2. 类型定义
// 3. props/emits
// 4. 响应式数据
// 5. 计算属性
// 6. 方法
// 7. 生命周期
// 8. 导出
</script>

<style scoped lang="scss">
/* 样式 */
</style>

4.2 TypeScript规范
约束 说明
使用 interface 定义对象类型 禁止使用 any
使用 type 定义联合类型
枚举使用 enum 与后端枚举对齐
API响应必须定义类型 所有接口都要有类型声明
4.3 API调用规范
typescript
// api/auth.ts
import request from '@/utils/request'
import type { LoginReq, LoginResp } from '@/types'

export const loginApi = (data: LoginReq) => {
return request.post<LoginResp>('/auth/login', data)
}
4.4 路由命名规范
类型 命名规则 示例
路由路径 kebab-case /employee/self-table
路由名称 PascalCase EmployeeSelfTable
路由参数 camelCase :tableId
4.5 颜色/样式规范
用途 颜色值
主色 #409EFF
成功 #67C23A
警告 #E6A23C
危险 #F56C6C
等级A #67C23A
等级B #409EFF
等级C #E6A23C
等级D #F56C6C
4.6 提交信息规范
text
feat: 新功能
fix: 修复Bug
docs: 文档更新
style: 代码风格
refactor: 重构
test: 测试
chore: 构建/工具
五、Vite配置要求
typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
plugins: [vue()],
resolve: {
alias: {
'@': path.resolve(\_\_dirname, 'src')
}
},
server: {
port: 5173,
proxy: {
'/api': {
target: 'http://localhost:8080',
changeOrigin: true
}
}
}
})
六、环境变量配置
env

# .env.development

VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=PerfFlow 绩效考核系统
env

# .env.production

VITE_API_BASE_URL=https://api.perfflow.com/api
VITE_APP_TITLE=PerfFlow 绩效考核系统
七、依赖版本要求
json
{
"dependencies": {
"vue": "^3.4.0",
"vue-router": "^4.3.0",
"pinia": "^2.1.0",
"element-plus": "^2.6.0",
"axios": "^1.6.0",
"@element-plus/icons-vue": "^2.3.0"
},
"devDependencies": {
"@vitejs/plugin-vue": "^5.0.0",
"vite": "^5.0.0",
"typescript": "^5.3.0",
"sass": "^1.69.0",
"@types/node": "^20.0.0",
"eslint": "^8.55.0",
"prettier": "^3.1.0",
"@vue/eslint-config-typescript": "^12.0.0"
}
}
八、页面路由定义
路径 组件 权限 说明
/login Login.vue 无 登录页
/employee/self SelfTable.vue 员工 我的考核表
/employee/edit/:id SelfEdit.vue 员工 填报/编辑
/dept/team TeamTables.vue 部门领导 部门所有表
/dept/review/:id ReviewDetail.vue 部门领导 审核详情
/leader/all AllTables.vue 领导 所有表
/leader/score/:id LeaderScore.vue 领导 领导评分
/hr/all AllFinalTables.vue 人事 完整汇总表
/hr/export ExportPrint.vue 人事 导出/打印
/admin/users UserManage.vue 管理员 用户管理
/admin/depts DeptManage.vue 管理员 部门管理
九、路由守卫要求
typescript
// router/guards.ts

- 检查Token是否存在
- 无Token → 跳转 /login
- 检查用户角色
- 根据角色控制可访问页面
- 权限不足 → 跳转 /403
  十、权限控制
  角色 可访问页面
  员工 /employee/_
  部门领导 /dept/_
  领导 /leader/_
  人事 /hr/_
  管理员 /admin/\*
  不同角色显示不同菜单，登录页无菜单。

十一、Axios拦截器要求
请求拦截器
typescript

- 从localStorage读取Token
- 添加 Authorization: Bearer {token}
- Token不存在则阻止请求
  响应拦截器
  typescript
- 401 → 清除Token → 跳转登录
- 403 → 跳转 /403
- 统一错误提示（ElMessage）
  十二、禁止创建的内容
  禁止项 说明
  ❌ node_modules/ 禁止提交，由 pnpm install 生成
  ❌ dist/ 禁止提交，由 pnpm build 生成
  ❌ .vite/ 禁止提交
  ❌ 后端代码 前端目录只放前端代码
  ❌ \*.java 文件 禁止出现在 frontend 目录
  ❌ 新的项目目录 只能在 frontend/ 目录下工作
  十三、启动命令
  bash
  cd D:\workspace\perfflow\frontend
  pnpm install # 或 npm install
  pnpm dev # 启动开发服务器
  pnpm build # 构建生产版本
  十四、交付要求
  □ 所有页面正常访问
  □ 所有接口对接完成（与后端Agent同步）
  □ 权限控制完整
  □ 表单校验完整
  □ 错误处理完整
  □ 响应式布局
  □ 可通过 pnpm build 成功构建
  十五、开始编码
  工作目录：D:\workspace\perfflow\frontend（已存在，请直接使用）
