/**
 * 认证状态 Store（登录态 / 用户快照 / 角色工具）
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { Role } from '@/types/enums'
import type { CurrentUserSnapshot, LoginResp } from '@/types/dto'
import {
  clearTokens,
  getAccessToken,
  setTokens
} from '@/utils/token'
import { logoutApi } from '@/api/auth.api'

const USER_KEY = 'perfflow_user'

/** 从 localStorage 恢复用户快照（刷新页面后守卫仍需角色判断） */
const restoreUser = (): LoginResp | null => {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as LoginResp
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

/** 以登录接口返回的 LoginResp 为准维护用户快照 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string>(getAccessToken())
  const user = ref<LoginResp | null>(restoreUser())

  const isLoggedIn = computed(() => accessToken.value !== '')
  const role = computed(() => user.value?.role ?? null)
  const isAdmin = computed(() => role.value === Role.ADMIN)
  const isEmployee = computed(() => role.value === Role.EMP)
  const isDeptLead = computed(() => role.value === Role.DEPT_LEAD)
  const isLeader = computed(() => role.value === Role.LEAD)
  const isHr = computed(() => role.value === Role.PERFORMANCE_HR)

  /** 登录成功：保存令牌与用户快照 */
  const setLogin = (data: LoginResp): void => {
    setTokens(data.accessToken, data.refreshToken)
    accessToken.value = data.accessToken
    user.value = data
    localStorage.setItem(USER_KEY, JSON.stringify(data))
  }

  /** 刷新上下文（/auth/me） */
  const applySnapshot = (snapshot: CurrentUserSnapshot): void => {
    if (!user.value) {
      return
    }
    user.value.userId = snapshot.userId
    user.value.username = snapshot.username
    user.value.realName = snapshot.realName
    user.value.deptId = snapshot.deptId
    user.value.deptLead = snapshot.deptLead
    user.value.mustChangePassword = snapshot.mustChangePassword
  }

  /** 修改资料后本地刷新真实姓名 */
  const applyProfile = (realName: string): void => {
    if (!user.value) {
      return
    }
    user.value.realName = realName
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  /** 登出：清空本地状态并调用后端（语义兼容） */
  const logout = async (): Promise<void> => {
    try {
      await logoutApi()
    } catch {
      // 登出接口失败不阻塞本地清理
    } finally {
      clearTokens()
      localStorage.removeItem(USER_KEY)
      accessToken.value = ''
      user.value = null
    }
  }

  return {
    accessToken,
    user,
    isLoggedIn,
    role,
    isAdmin,
    isEmployee,
    isDeptLead,
    isLeader,
    isHr,
    setLogin,
    applySnapshot,
    applyProfile,
    logout
  }
})
