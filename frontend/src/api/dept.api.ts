/**
 * 部门考核模块接口（/dept-assessments）
 */
import { request } from '@/utils/request'
import type { DeptAssessmentReq, DeptAssessmentResp, DeptResp } from '@/types/dto'
import type { Result } from '@/types/result'

/** 获取本部门当前周期考核表 */
export const getDeptAssessmentApi = (deptId: number): Promise<Result<DeptAssessmentResp>> =>
  request.get<DeptAssessmentResp>('/dept-assessments/current', { params: { deptId } })

/** 提交部门考核（→待复核） */
export const submitDeptAssessmentApi = (deptId: number, data: DeptAssessmentReq): Promise<Result<void>> =>
  request.post<void>('/dept-assessments/submit', data, { params: { deptId } })

/** 部门负责人复核 */
export const reviewDeptAssessmentApi = (deptId: number, approve: boolean, comment?: string): Promise<Result<void>> =>
  request.post<void>(`/dept-assessments/${deptId}/review`, undefined, { params: { approve, comment } })

/** 运营管理部初审 */
export const auditDeptAssessmentApi = (deptId: number, approve: boolean, comment?: string): Promise<Result<void>> =>
  request.post<void>(`/dept-assessments/${deptId}/audit`, undefined, { params: { approve, comment } })

/** 绩效委员会最终审批 */
export const approveDeptAssessmentApi = (deptId: number): Promise<Result<void>> =>
  request.post<void>(`/dept-assessments/${deptId}/approve`)

/** 部门考核进度列表 */
export const listDeptAssessmentApi = (): Promise<Result<DeptAssessmentResp[]>> =>
  request.get<DeptAssessmentResp[]>('/dept-assessments/list')

/** 部门下拉选项（HR 开启部门线周期勾选用） */
export const getDeptOptionsApi = (): Promise<Result<DeptResp[]>> =>
  request.get<DeptResp[]>('/departments/options')
