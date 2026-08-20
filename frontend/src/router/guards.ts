/**
 * 路由守卫
 * - 无 Token → 跳转 /login
 * - 角色不符 → 跳转 /403
 * - ADMIN 访问业务路由（adminDenied）→ 跳转 /403
 * - mustChangePassword=true 且非改密页 → 强制跳转改密页
 */
import type { Router } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { hasToken } from '@/utils/token'
import { Role } from '@/types/enums'

/** 改密相关页面（豁免强制改密跳转） */
const CHANGE_PASSWORD_PATHS = ['/change-password']

export const setupRouterGuards = (router: Router): void => {
  router.beforeEach((to) => {
    // 无需登录的页面（登录页 / 403 / 404）
    if (to.meta.requiresAuth === false) {
      return true
    }

    // 未登录 → 登录页
    if (!hasToken()) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    const authStore = useAuthStore()
    const role = authStore.role

    // ADMIN 禁止访问业务路由（与后端 403 + 1007 一致）
    if (role === Role.ADMIN && to.meta.adminDenied) {
      return { path: '/403' }
    }

    // 强制改密：mustChangePassword=true 且未在改密流程中
    if (
      authStore.user?.mustChangePassword &&
      !CHANGE_PASSWORD_PATHS.includes(to.path)
    ) {
      return { path: '/change-password' }
    }

    // 角色校验
    if (to.meta.roles && to.meta.roles.length > 0) {
      if (!role || !to.meta.roles.includes(role)) {
        return { path: '/403' }
      }
    }

    return true
  })

  router.afterEach((to) => {
    const title = to.meta.title
    document.title = title ? `${title} - ${import.meta.env.VITE_APP_TITLE}` : import.meta.env.VITE_APP_TITLE
  })
}
