核心规范速查表

1. 组件命名 (Component Naming)
   文件名: 必须使用 PascalCase (如 UserProfile.vue) 。

模板中使用: 建议使用 kebab-case (如 <user-profile />)，或保持与 PascalCase 一致，但必须确保项目内统一 。

基础组件: 统一使用特定前缀，如 Base、App 或 V (BaseButton.vue) 。

单例组件: 使用 The 前缀 (TheHeader.vue) 。

紧密耦合组件: 以父组件名作为前缀 (TodoList.vue 和 TodoListItem.vue) 。

2. 组件结构 (Component Structure)
   对于使用 <script setup> 的组件，内部应遵循清晰一致的顺序 ：

vue

<script setup lang="ts">
// 1. 导入 (Imports)
// 2. Props 和 Emits 定义
// 3. 响应式状态 (ref, reactive)
// 4. 计算属性 (computed)
// 5. 组合式函数 (Composables)
// 6. 方法 (Functions)
// 7. 生命周期钩子 (Lifecycle Hooks)
</script>

<template>
  <!-- 模板 -->
</template>

<style scoped>
/* 样式 */
</style>

3. Props 与 Emits (Props & Emits)
   Props 定义: 必须使用 TypeScript 定义类型，并明确是否可选及默认值 。

Props 修改: 绝对禁止在子组件中直接修改 Props 。

Emits 定义: 必须使用 TypeScript 类型化 Emits 。

双向绑定: 推荐使用 defineModel() (Vue 3.4+) 来实现 v-model 。

4. 模板规范 (Template Rules)
   v-for 与 key: 必须始终为 v-for 添加 :key，且 key 值应唯一 。

v-if 与 v-for: 绝对禁止在同一个元素上同时使用 v-if 和 v-for 。应使用 <template> 包裹或通过计算属性过滤。

多词组件名: 自定义组件名必须是多个单词，防止与未来 HTML 元素冲突 。

5. TypeScript 规范 (TypeScript Rules)
   类型推断: 对于能明确推断出的类型，无需显式声明 。

接口 vs 类型: 对象形状用 interface，联合类型等用 type 。

枚举: 避免使用 TypeScript 的 enum，推荐使用 const 对象或字符串联合类型 。

📋 可直接使用的 Agent 约束片段
markdown

## Vue3 编码规范 (必须严格遵守)

- **组件名**: 文件名必须为 `PascalCase` (如 `UserProfile.vue`)，模板中使用 `kebab-case` (如 `<user-profile />`) [citation:1][citation:4]。
- **组件结构**: 必须使用 `<script setup lang="ts">` [citation:4][citation:6]，并遵循 导入 → Props/Emits → 响应式 → 计算属性 → 方法 → 生命周期 的顺序 [citation:4]。
- **Props/Emits**: 必须使用 TypeScript 进行类型定义，Props 的默认值通过 `withDefaults` 设置 [citation:4]。
- **模板**: `v-for` 必须绑定 `:key` [citation:1]；`v-if` 和 `v-for` 禁止同时出现在同一元素上 [citation:1][citation:8]。
- **TypeScript**: 优先使用类型推断 [citation:7]；对象用 `interface`，联合类型用 `type`，避免使用 `enum` [citation:7]。
- **组合式函数**: 文件名以 `use` 开头 (如 `useAuth.ts`)，保持单一职责 [citation:2][citation:3]。
  确认一下风格，要简约，商务
  Vue3 编码规范（简约商务版）
  适用场景：企业后台管理系统，强调代码可维护性、团队协作效率和界面专业感。

一、命名规范
类型 规则 示例
组件文件 PascalCase UserTable.vue、ScoreCard.vue
页面视图 PascalCase SelfScore.vue、TeamReview.vue
组合式函数 camelCase，use前缀 useAuth.ts、useTable.ts
工具函数 camelCase formatDate.ts、validateId.ts
常量 UPPER_SNAKE_CASE API_BASE_URL、PAGE_SIZE
类型/接口 PascalCase UserInfo、ScoreRecord
路由路径 kebab-case /employee/self-score
二、组件结构（统一顺序）
vue
<template>

  <!-- 模板内容 -->
</template>

<script setup lang="ts">
// 1. 导入（第三方 → 内部）
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

// 2. Props / Emits / Model
const props = defineProps<{
  tableId: number
  readonly?: boolean
}>()

const emit = defineEmits<{
  submit: []
  cancel: []
}>()

// 3. 状态（ref → reactive）
const loading = ref(false)
const formData = ref({})

// 4. 计算属性
const isValid = computed(() => !!formData.value.name)

// 5. 组合式函数
const router = useRouter()
const authStore = useAuthStore()

// 6. 方法（按调用顺序排列）
const handleSubmit = () => { /* ... */ }
const handleCancel = () => { /* ... */ }

// 7. 生命周期
onMounted(() => { /* ... */ })

// 8. 暴露（仅必要时）
defineExpose({ reset })
</script>

<style scoped lang="scss">
/* 样式 */
</style>

三、Props / Emits 规范
typescript
// ✅ 正确：完整类型定义
const props = defineProps<{
tableId: number
data: TableRow[]
readonly?: boolean
}>()

// ✅ 有默认值时使用 withDefaults
const props = withDefaults(defineProps<{
size?: 'small' | 'default' | 'large'
loading?: boolean
}>(), {
size: 'default',
loading: false
})

// ✅ Emits 类型化
const emit = defineEmits<{
update: [value: string]
submit: []
cancel: []
}>()

// ❌ 禁止运行时声明（Options API风格）
// defineProps({ tableId: Number })
四、模板规范
vue
<template>

  <!-- ✅ v-for 必须绑定 key -->
  <div v-for="item in list" :key="item.id">
    {{ item.name }}
  </div>

  <!-- ✅ v-if 和 v-for 分离 -->
  <template v-for="item in list" :key="item.id">
    <div v-if="item.visible">{{ item.name }}</div>
  </template>
  <!-- 或使用计算属性过滤后遍历 -->

  <!-- ✅ v-model 简写 -->
  <el-input v-model="formData.name" />

  <!-- ✅ 事件处理使用简洁语法 -->

<el-button @click="handleSubmit">提交</el-button>
<el-input @keyup.enter="handleSearch" />

  <!-- ❌ 禁止内联复杂逻辑 -->
  <!-- <div>{{ list.filter(x => x.status === 1).map(x => x.name).join(',') }}</div> -->
  <!-- ✅ 应使用计算属性 -->
</template>
五、TypeScript 规范
typescript
// ✅ 对象类型用 interface
export interface UserInfo {
  id: number
  name: string
  role: 'EMP' | 'DEPT_LEAD' | 'LEAD' | 'HR' | 'ADMIN'
  deptId?: number
}

// ✅ 联合类型用 type
export type RoleType = 'EMP' | 'DEPT_LEAD' | 'LEAD' | 'HR' | 'ADMIN'
export type ScoreGrade = 'A' | 'B' | 'C' | 'D'

// ✅ 枚举使用 const 对象（替代 enum）
export const Role = {
EMP: 'EMP',
DEPT_LEAD: 'DEPT_LEAD',
LEAD: 'LEAD',
HR: 'HR',
ADMIN: 'ADMIN'
} as const

export type Role = typeof Role[keyof typeof Role]

// ❌ 禁止使用 any
// const data: any = {}
// ✅ 应使用 unknown 或具体类型
const data: unknown = {}
六、API 调用规范
typescript
// api/assessment.ts
import request from '@/utils/request'
import type { ApiResponse } from '@/types'

// 每个接口独立导出，命名清晰
export const getSelfTable = () => {
return request.get<ApiResponse<TableData>>('/self/myTable')
}

export const saveRow = (data: SaveRowReq) => {
return request.put<ApiResponse<ScoreResult>>('/self/rows', data)
}

export const submitSelf = (tableId: number) => {
return request.post<ApiResponse<void>>(`/self/submit/${tableId}`)
}
七、路径别名规范
typescript
// ✅ 使用 @ 别名（已在 vite.config.ts 配置）
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

// ❌ 禁止相对路径跨多层引用
// import { useAuthStore } from '../../../stores/auth'
八、样式规范
scss
// ✅ scoped + 语义化命名

<style scoped lang="scss">
// 类名使用 kebab-case
.assessment-table {
  padding: 20px;
}

.assessment-table__header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.assessment-table__score {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

// 商务配色：灰白为主，蓝色点缀
.primary-color {
  color: #2b6cb0;
}

.success-color {
  color: #38a169;
}

.warning-color {
  color: #d69e2e;
}

.danger-color {
  color: #e53e3e;
}

// 字号层级
.text-sm { font-size: 13px; }
.text-base { font-size: 14px; }
.text-lg { font-size: 16px; }
.text-xl { font-size: 18px; }
.text-2xl { font-size: 20px; }

// 间距体系
.space-4 { margin: 4px; }
.space-8 { margin: 8px; }
.space-12 { margin: 12px; }
.space-16 { margin: 16px; }
.space-20 { margin: 20px; }
.space-24 { margin: 24px; }
</style>

九、核心强制约束
约束 说明
✅ 组件名 PascalCase 文件名必须 PascalCase
✅ 使用 <script setup lang="ts"> 统一语法风格
✅ Props/Emits 类型化 必须用 TypeScript 定义
✅ v-for 绑定 :key 禁止省略
✅ v-if 和 v-for 分离 禁止同元素使用
✅ 使用 @ 路径别名 禁止相对路径跨多层
❌ 禁止 any 使用具体类型或 unknown
❌ 禁止 Options API 统一 Composition API
❌ 禁止内联样式 使用 scoped SCSS
❌ 禁止未使用的变量 保持代码整洁
十、商务简约风格要点
维度 要求
配色 白底灰框，蓝色为主，避免鲜艳色彩
字体 14px 为主，层级清晰
间距 统一 8/16/24px 体系
组件 Element Plus 默认风格即可
表格 简洁边框，斑马纹关闭
按钮 文字按钮为主，主操作使用主色
反馈 ElMessage 简洁提示，避免弹窗干扰
Agent 约束片段（可直接使用）
text

## Vue3 编码规范

1. 组件文件使用 PascalCase 命名
2. 使用 <script setup lang="ts"> 语法
3. Props 和 Emits 必须用 TypeScript 类型化定义
4. v-for 必须绑定 :key，v-if 和 v-for 禁止同元素使用
5. 路径使用 @ 别名，禁止相对路径跨多层
6. 禁止使用 any，对象用 interface，联合类型用 type
7. 样式使用 scoped SCSS，类名使用 kebab-case
8. 商务简约风格：白底灰框，蓝色为主，字号 14px，间距统一
9. 使用 Element Plus 组件库
10. 每个页面保持单一职责，组件行数不超过 300 行