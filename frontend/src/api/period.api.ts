/**
 * 考核周期模块接口（/periods）
 */
import { request } from '@/utils/request'
import type { PeriodCreateReq, PeriodResp } from '@/types/dto'
import type { Result } from '@/types/result'

/** 周期列表 */
export const getPeriodListApi = (): Promise<Result<PeriodResp[]>> => request.get<PeriodResp[]>('/periods')

/** 当前周期（无活跃周期时 data 为 null） */
export const getCurrentPeriodApi = (): Promise<Result<PeriodResp | null>> =>
  request.get<PeriodResp | null>('/periods/current')

/** 新建周期（返回周期 ID） */
export const createPeriodApi = (data: PeriodCreateReq): Promise<Result<number>> =>
  request.post<number>('/periods', data)

/** 开启周期（生成勾选员工考核表；userIds 为空则全员） */
export const openPeriodApi = (id: number, userIds?: number[]): Promise<Result<void>> =>
  request.post<void>(`/periods/${id}/open`, userIds && userIds.length > 0 ? { userIds } : {})

/** 导入周期考核明细（员工+明细一个文件，返回参与员工ID） */
export const importPeriodApi = (id: number, file: File): Promise<Result<number[]>> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<number[]>('/periods/' + id + '/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 关闭周期 */
export const closePeriodApi = (id: number): Promise<Result<void>> =>
  request.post<void>(`/periods/${id}/close`)
