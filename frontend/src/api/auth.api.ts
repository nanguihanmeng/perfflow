/**
 * 鉴权模块接口（/auth）
 */
import { request } from '@/utils/request'
import type { ChangePasswordReq, CurrentUserSnapshot, LoginReq, LoginResp, ProfileReq, RefreshReq } from '@/types/dto'
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
 * 用户改密：成功后返回新令牌（mustChangePassword=false），前端直接覆盖本地登录态，无需重新登录
 */
export const changePasswordApi = (data: ChangePasswordReq): Promise<Result<LoginResp>> =>
  request.put<LoginResp>('/auth/password', data, { silent: true })

/** 修改个人资料（姓名/邮箱/电话） */
export const updateProfileApi = (data: ProfileReq): Promise<Result<void>> =>
  request.put<void>('/auth/profile', data)
