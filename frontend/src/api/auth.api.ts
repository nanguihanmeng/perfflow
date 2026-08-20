/**
 * 鉴权模块接口（/auth）
 */
import { request } from '@/utils/request'
import type { ChangePasswordReq, CurrentUserSnapshot, LoginReq, LoginResp, RefreshReq } from '@/types/dto'
import type { Result } from '@/types/result'

/** 登录 */
export const loginApi = (data: LoginReq): Promise<Result<LoginResp>> =>
  request.post<LoginResp>('/auth/login', data)

/** 刷新令牌（旋转策略，旧 refreshToken 失效） */
export const refreshApi = (data: RefreshReq): Promise<Result<LoginResp>> =>
  request.post<LoginResp>('/auth/refresh', data)

/** 当前用户信息快照 */
export const getMeApi = (): Promise<Result<CurrentUserSnapshot>> =>
  request.get<CurrentUserSnapshot>('/auth/me')

/** 登出（后端无状态，仅语义兼容） */
export const logoutApi = (): Promise<Result<void>> => request.post<void>('/auth/logout')

/**
 * 用户改密（预留接口）
 * 前端仅提交新密码；后端实现后按此契约联调（错误码 1105/1106）
 */
export const changePasswordApi = (data: ChangePasswordReq): Promise<Result<void>> =>
  request.put<void>('/auth/password', data, { silent: true })
