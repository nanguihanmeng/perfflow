/**
 * 部门考核模块接口（/dept-assessments）
 */
import { request, getBlob } from '@/utils/request'
import type { DeptActualValueReq, DeptAssessmentOptionResp, DeptAssessmentResp, DeptFlowLogResp, DeptResp } from '@/types/dto'
import type { Result } from '@/types/result'

/** 查询部门考核流程日志 */
export const getDeptFlowLogsApi = (id: number): Promise<Result<DeptFlowLogResp[]>> =>
  request.get<DeptFlowLogResp[]>(`/dept-assessments/${id}/logs`)

/** 获取本部门可填报的部门考核选项（HR 已开启） */
export const getDeptAssessmentOptionsApi = (deptId: number): Promise<Result<DeptAssessmentOptionResp[]>> =>
  request.get<DeptAssessmentOptionResp[]>('/dept-assessments/options', { params: { deptId } })

/** 查询部门考核详情（按角色数据范围） */
export const getDeptAssessmentApi = (id: number): Promise<Result<DeptAssessmentResp>> =>
  request.get<DeptAssessmentResp>(`/dept-assessments/${id}`)

/** 提交部门考核实际完成值（→待复核） */
export const submitDeptAssessmentApi = (id: number, data: DeptActualValueReq): Promise<Result<void>> =>
  request.post<void>(`/dept-assessments/${id}/submit`, data)

/** 部门负责人复核 */
export const reviewDeptAssessmentApi = (id: number, approve: boolean, comment?: string): Promise<Result<void>> =>
  request.post<void>(`/dept-assessments/${id}/review`, undefined, { params: { approve, comment } })

/** 运营管理部初审 */
export const auditDeptAssessmentApi = (id: number, approve: boolean, comment?: string): Promise<Result<void>> =>
  request.post<void>(`/dept-assessments/${id}/audit`, undefined, { params: { approve, comment } })

/** 绩效委员会最终审批 */
export const approveDeptAssessmentApi = (id: number): Promise<Result<void>> =>
  request.post<void>(`/dept-assessments/${id}/approve`)

/** 部门考核进度列表 */
export const listDeptAssessmentApi = (): Promise<Result<DeptAssessmentResp[]>> =>
  request.get<DeptAssessmentResp[]>('/dept-assessments/list')

/** 下载部门考核指标模板（HR，可带 assessmentId 预填现有 KPI） */
export const downloadDeptTemplateApi = (assessmentId?: number | null) =>
  getBlob('/export/template/dept', { params: assessmentId ? { assessmentId } : undefined })

/** 上传部门考核指标（HR） */
export const importDeptApi = (assessmentId: number, file: File): Promise<Result<void>> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<void>(`/import/dept?assessmentId=${assessmentId}`, formData)
}

/** 按周期批量上传部门考核指标（HR） */
export const importDeptBatchApi = (periodId: number, file: File): Promise<Result<void>> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<void>(`/import/dept/batch?periodId=${periodId}`, formData)
}

/** 部门下拉选项（HR 开启部门线周期勾选用） */
export const getDeptOptionsApi = (): Promise<Result<DeptResp[]>> =>
  request.get<DeptResp[]>('/departments/options')
