/**
 * 等级配额与权重接口（/grade、/weight）
 */
import { request } from '@/utils/request'
import type { GradeQuotaConfig, GradeQuotaReq, WeightConfig, WeightConfigReq } from '@/types/dto'
import type { Result } from '@/types/result'

/** 等级配额列表 */
export const getQuotaListApi = (): Promise<Result<GradeQuotaConfig[]>> =>
  request.get<GradeQuotaConfig[]>('/grade/quota-config')

/** 新增等级配额 */
export const createQuotaApi = (data: GradeQuotaReq): Promise<Result<number>> =>
  request.post<number>('/grade/quota-config', data)

/** 更新等级配额 */
export const updateQuotaApi = (id: number, data: GradeQuotaReq): Promise<Result<void>> =>
  request.put<void>(`/grade/quota-config/${id}`, data)

/** 删除等级配额 */
export const deleteQuotaApi = (id: number): Promise<Result<void>> =>
  request.delete<void>(`/grade/quota-config/${id}`)

/** 触发等级自动计算 */
export const autoCalculateApi = (periodId: number): Promise<Result<void>> =>
  request.post<void>('/grade/auto-calculate', undefined, { params: { periodId } })

/** 权重配置列表 */
export const getWeightListApi = (): Promise<Result<WeightConfig[]>> =>
  request.get<WeightConfig[]>('/weight/config')

/** 新增权重配置 */
export const createWeightApi = (data: WeightConfigReq): Promise<Result<number>> =>
  request.post<number>('/weight/config', data)

/** 更新权重配置 */
export const updateWeightApi = (id: number, data: WeightConfigReq): Promise<Result<void>> =>
  request.put<void>(`/weight/config/${id}`, data)
