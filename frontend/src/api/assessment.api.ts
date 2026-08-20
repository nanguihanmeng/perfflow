/**
 * 考核模块接口（/assessment-tables、行、日志、导出/打印）
 */
import { request, getBlob, getText } from '@/utils/request'
import type {
  AssessmentTableResp,
  AssessmentRejectReq,
  ExtendSuspendReq,
  FlowLogResp,
  LeadScoreReq,
  RowReq
} from '@/types/dto'
import type { PageResult, Result } from '@/types/result'

/** 主表分页查询参数（分页参数名对齐后端 pageNo / pageSize） */
export interface TablePageQuery {
  periodId?: number | null
  deptId?: number | null
  state?: string
  pageNo: number
  pageSize: number
}

/** 主表详情 */
export const getTableDetailApi = (id: number): Promise<Result<AssessmentTableResp>> =>
  request.get<AssessmentTableResp>(`/assessment-tables/${id}`)

/** 主表分页（元素不含 rows / logs） */
export const getTablePageApi = (params: TablePageQuery): Promise<Result<PageResult<AssessmentTableResp>>> =>
  request.get<PageResult<AssessmentTableResp>>('/assessment-tables', { params })

/** 员工提交（仅本人 SELF_DRAFTING） */
export const submitTableApi = (id: number): Promise<Result<void>> =>
  request.post<void>(`/assessment-tables/${id}/submit`)

/** 人事推送 */
export const pushTableApi = (id: number): Promise<Result<void>> =>
  request.post<void>(`/assessment-tables/${id}/push`)

/** 部门通过（comment 为通过备注，可选） */
export const approveTableApi = (id: number, comment?: string): Promise<Result<void>> =>
  request.post<void>(`/assessment-tables/${id}/approve`, { comment })

/** 部门打回（comment 必填，否则 code=2012） */
export const rejectTableApi = (id: number, comment: string): Promise<Result<void>> =>
  request.post<void>(`/assessment-tables/${id}/reject`, { comment } satisfies AssessmentRejectReq)

/** 领导评分（leaderScore 0-100 字符串） */
export const leadScoreTableApi = (id: number, data: LeadScoreReq): Promise<Result<void>> =>
  request.post<void>(`/assessment-tables/${id}/lead-score`, data)

/** 延长挂起（累计上限 30 天） */
export const extendSuspendApi = (id: number, data: ExtendSuspendReq): Promise<Result<void>> =>
  request.post<void>(`/assessment-tables/${id}/extend-suspend`, data)

/** 删除主表（HR，级联删除行与流程日志） */
export const deleteTableApi = (id: number): Promise<Result<void>> =>
  request.delete<void>(`/assessment-tables/${id}`)

/** 更新行完成率（EMP 自评 / DEPT_LEAD 部门审核） */
export const updateRowApi = (tableId: number, rowId: number, data: RowReq): Promise<Result<void>> =>
  request.put<void>(`/assessment-tables/${tableId}/rows/${rowId}`, data)

/** 更新主表岗位（被考核人信息栏） */
export const updatePositionApi = (tableId: number, position: string): Promise<Result<void>> =>
  request.put<void>(`/assessment-tables/${tableId}/rows/position`, { position })

/** 导入考核明细（HR，模板A1:G16格式xlsx） */
export const importRowsApi = (tableId: number, file: File): Promise<Result<void>> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<void>(`/assessment-tables/${tableId}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 流程日志（HR / LEAD / DEPT_LEAD） */
export const getFlowLogsApi = (id: number): Promise<Result<FlowLogResp[]>> =>
  request.get<FlowLogResp[]>(`/assessment-tables/${id}/logs`)

/** 导出 Excel（二进制流，HR） */
export const exportExcelApi = (periodId?: number | null) =>
  getBlob('/assessment-tables/export', {
    params: periodId ? { periodId } : undefined
  })

/** 打印 HTML（文本，HR） */
export const getPrintHtmlApi = (periodId?: number | null) =>
  getText('/assessment-tables/export/print', {
    params: periodId ? { periodId } : undefined
  })
