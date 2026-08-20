/**
 * 认证组合式函数：暴露登录状态与用户信息，供各组件使用
 */
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/store/auth'

export const useAuth = () => {
  const authStore = useAuthStore()
  const { isLoggedIn, role, isAdmin, isEmployee, isDeptLead, isLeader, isHr, user } =
    storeToRefs(authStore)

  /** 当前用户是否拥有指定角色 */
  const hasRole = (...roles: string[]): boolean => {
    if (!role.value) {
      return false
    }
    return roles.includes(role.value)
  }

  return {
    isLoggedIn,
    role,
    isAdmin,
    isEmployee,
    isDeptLead,
    isLeader,
    isHr,
    user,
    hasRole
  }
}
