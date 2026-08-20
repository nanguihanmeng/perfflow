/**
 * 路由定义与元信息类型扩展
 */
import type { Role } from '@/types/enums'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题 */
    title?: string
    /** 是否需要登录（默认 true） */
    requiresAuth?: boolean
    /** 允许访问的角色；缺省表示登录即可访问 */
    roles?: Role[]
    /** 业务路由标记：ADMIN 角色不可访问（后端 AdminBusinessGuardInterceptor 拒绝） */
    adminDenied?: boolean
  }
}
