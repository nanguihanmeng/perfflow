/**
 * 进度监控接口（/monitor）
 */
import { request } from '@/utils/request'
import type { ProgressResp } from '@/types/dto'
import type { Result } from '@/types/result'

/** 考核进度看板 */
export const getDashboardApi = (): Promise<Result<ProgressResp>> =>
  request.get<ProgressResp>('/monitor/dashboard')

/** 定向催办 */
export const remindApi = (userIds: number[], content?: string): Promise<Result<void>> =>
  request.post<void>('/monitor/remind', { userIds, content })
