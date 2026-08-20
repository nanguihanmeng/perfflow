/**
 * 系统管理模块接口（/admin/users、/admin/departments）
 */
import { request } from '@/utils/request'
import type {
  DeptReq,
  DeptResp,
  PasswordResetResp,
  UserCreateReq,
  UserResp,
  UserUpdateReq
} from '@/types/dto'
import type { PageResult, Result } from '@/types/result'

/** 用户分页查询参数 */
export interface UserPageQuery {
  username?: string
  role?: string
  deptId?: number | null
  status?: number | null
  pageNo: number
  pageSize: number
}

/** 用户分页 */
export const getUserPageApi = (params: UserPageQuery): Promise<Result<PageResult<UserResp>>> =>
  request.get<PageResult<UserResp>>('/admin/users', { params })

/** 新建用户（返回用户 ID） */
export const createUserApi = (data: UserCreateReq): Promise<Result<number>> =>
  request.post<number>('/admin/users', data)

/** 修改用户 */
export const updateUserApi = (id: number, data: UserUpdateReq): Promise<Result<void>> =>
  request.put<void>(`/admin/users/${id}`, data)

/** 删除用户 */
export const deleteUserApi = (id: number): Promise<Result<void>> =>
  request.delete<void>(`/admin/users/${id}`)

/** 重置密码（返回临时密码） */
export const resetUserPasswordApi = (id: number): Promise<Result<PasswordResetResp>> =>
  request.post<PasswordResetResp>(`/admin/users/${id}/reset-password`)

/** 启用 / 禁用切换 */
export const toggleUserStatusApi = (id: number): Promise<Result<void>> =>
  request.post<void>(`/admin/users/${id}/toggle-status`)

/** 部门列表 */
export const getDeptListApi = (): Promise<Result<DeptResp[]>> => request.get<DeptResp[]>('/admin/departments')

/** 参与考核的员工选项（HR 开启周期勾选用） */
export const getUserOptionsApi = (): Promise<Result<UserResp[]>> => request.get<UserResp[]>('/users/options')

/** 新建部门 */
export const createDeptApi = (data: DeptReq): Promise<Result<number>> =>
  request.post<number>('/admin/departments', data)

/** 修改部门 */
export const updateDeptApi = (id: number, data: DeptReq): Promise<Result<void>> =>
  request.put<void>(`/admin/departments/${id}`, data)

/** 删除部门 */
export const deleteDeptApi = (id: number): Promise<Result<void>> =>
  request.delete<void>(`/admin/departments/${id}`)
