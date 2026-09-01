/**
 * DTO 类型定义，字段与后端 Java 源码对齐（API.md 契约）
 * 约定：分数一律 string（BigDecimal），时间一律 string（LocalDate / LocalDateTime）
 */
import type { AssessmentState, ReminderSeverity, Role, RowCategory, UserStatus } from './enums'

/* ------------------------- 鉴权模块 /auth ------------------------- */

export interface LoginReq {
  username: string
  password: string
}

export interface LoginResp {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  userId: number
  username: string
  realName: string
  role: Role
  deptId: number | null
  deptName: string | null
  deptLead: boolean
  mustChangePassword: boolean
}

export interface RefreshReq {
  refreshToken: string
}

/** /auth/me 返回的 DataScopeContext 用户快照 */
export interface CurrentUserSnapshot {
  userId: number
  deptId: number | null
  username: string
  realName: string
  primaryRole: Role
  roles: string[]
  deptLead: boolean
  mustChangePassword: boolean
}

/**
 * 预留：用户改密（错误码 1105/1106）。
 * 前端不校验旧密码，仅提交新密码；后端如需校验旧密码可在联调时同步调整契约。
 */
export interface ChangePasswordReq {
  newPassword: string
}

/** 修改个人资料 */
export interface ProfileReq {
  realName?: string
  email?: string | null
  phone?: string | null
}

/* ------------------------- 考核周期 /periods ------------------------- */

export interface PeriodResp {
  id: number
  name: string
  periodType: string
  periodTypeLabel: string
  year: number
  quarter: number
  startDate: string
  suspendEndDate: string
  deptReviewEndDate: string
  leadScoreEndDate: string
  autoPushOnExpire: boolean
  status: number
}

export interface PeriodCreateReq {
  name: string
  periodType: string
  year: number
  quarter: number
  startDate: string
  suspendEndDate: string
  deptReviewEndDate: string
  leadScoreEndDate: string
  autoPushOnExpire?: boolean
}

/* ------------------------- 考核主表 /assessment-tables ------------------------- */

export interface AssessmentTableResp {
  id: number
  periodId: number
  userId: number
  deptId: number
  /** 岗位（被考核人填写） */
  position: string | null
  state: AssessmentState
  selfTotalScore: string | null
  leaderScore: string | null
  finalScore: string | null
  grade: string | null
  suspendExtendedDays: number | null
  submittedAt: string | null
  pushedAt: string | null
  deptApprovedAt: string | null
  leadFinishedAt: string | null
  periodName: string
  realName: string
  deptName: string
  /** 部门负责人姓名 */
  deptLeadName: string | null
  /** 分页列表接口不返回该字段 */
  rows?: RowResp[]
  /** 分页列表接口不返回该字段 */
  logs?: FlowLogResp[]
}

/** 考核行 */
export interface RowResp {
  id: number
  tableId: number
  category: RowCategory
  seq: number
  indicatorName: string
  baseScore: string | null
  workTarget: string
  scoreCriteria: string
  completionRate: string | null
  /** 脱敏时后端返回 null */
  selfScore: string | null
  /** 脱敏时后端返回 null */
  leaderScore: string | null
  frozen: boolean
  /** 是否脱敏（前端据此显示「**」） */
  masked: boolean
}

/** 更新行（EMP 改完成率 / DEPT_LEAD 改自评得分 / 岗位） */
export interface RowReq {
  completionRate?: string
  selfScore?: string
  position?: string
}

/** 领导评分 */
export interface LeadScoreReq {
  leaderScore: string
  comment?: string
}

/** 部门通过/打回（通过仅取 comment 作为备注；打回 comment 必填） */
export interface AssessmentRejectReq {
  comment: string
}

/** 延长挂起 */
export interface ExtendSuspendReq {
  days: number
  reason?: string
}

/** 流程日志 */
export interface FlowLogResp {
  id: number
  fromState: string
  toState: string
  action: string
  operatorId: number
  operatorName: string
  operatorRole: string
  comment: string
  createdAt: string
}

/* ------------------------- 首页 /home ------------------------- */

export interface Reminder {
  bizType: string
  type: string
  title: string
  description: string
  targetTableId: number | null
  targetAssessmentId: number | null
  targetPeriodId: number | null
  severity: ReminderSeverity
}

export interface RemindersResp {
  todos: Reminder[]
  upcomingSuspends: Reminder[]
  systemNotices: Reminder[]
}

/* ------------------------- 管理员-用户 /admin/users ------------------------- */

export interface UserResp {
  id: number
  username: string
  realName: string
  role: Role
  deptId: number | null
  deptName: string | null
  deptLead: boolean
  email: string | null
  phone: string | null
  status: UserStatus
  lastLoginAt: string | null
  mustChangePassword: boolean
}

export interface UserCreateReq {
  username: string
  realName: string
  role: Role
  deptId?: number | null
  email?: string | null
  phone?: string | null
}

export interface UserUpdateReq {
  realName: string
  role: Role
  deptId?: number | null
  email?: string | null
  phone?: string | null
  status: UserStatus
}

export interface PasswordResetResp {
  initialPassword: string
}

/* ------------------------- 管理员-部门 /admin/departments ------------------------- */

export interface DeptResp {
  id: number
  name: string
  parentId: number | null
  remark: string | null
}

export interface DeptReq {
  name: string
  parentId?: number | null
  remark?: string | null
}

/* ------------------------- 部门考核 /dept-assessments ------------------------- */

/** 部门 KPI 行 */
export interface DeptKpiRowResp {
  id: number
  deptAssessmentId: number
  rowType: string
  seqNo: number
  indicatorName: string | null
  targetValue: string | null
  actualValue: string | null
  scoringStandard: string | null
  score: string | null
  weight: string | null
}

/** 部门 KPI 行实际完成值请求 */
export interface DeptActualValueRowReq {
  seqNo: number
  actualValue: string | null
}

/** 部门考核填报请求（绩效专员仅提交实际完成值） */
export interface DeptActualValueReq {
  rows: DeptActualValueRowReq[]
}

/** 部门考核填报选项（绩效专员先选择再填报） */
export interface DeptAssessmentOptionResp {
  assessmentId: number
  periodId: number
  periodName: string
  periodType: string
  periodTypeLabel: string
  year: number
  quarter: number
  deptId: number
  deptName: string | null
  status: number
  submittedAt: string | null
}

/** 部门考核响应 */
export interface DeptAssessmentResp {
  id: number
  periodId: number
  deptId: number
  deptName: string | null
  kpiScore: string | null
  operationScore: string | null
  keyWorkScore: string | null
  bonusScore: string | null
  totalScore: string | null
  deptGrade: string | null
  status: number
  submittedAt: string | null
  reviewedAt: string | null
  approvedAt: string | null
  version: number
  adjustReason: string | null
  rows: DeptKpiRowResp[]
}

/** 部门考核流程日志响应 */
export interface DeptFlowLogResp {
  id: number
  assessmentId: number
  fromStatus: number
  toStatus: number
  action: string
  operatorId: number | null
  operatorName: string | null
  operatorRole: string | null
  comment: string | null
  createdAt: string
}

/* ------------------------- 进度看板 /monitor ------------------------- */

export interface DeptProgressResp {
  deptId: number
  deptName: string
  total: number
  filled: number
  unfilled: number
  reviewed: number
  overdue: number
  completionRate: number
}

export interface ProgressResp {
  total: number
  filled: number
  unfilled: number
  reviewed: number
  overdue: number
  deptDetails: DeptProgressResp[]
}

/* ------------------------- 通知 /notifications ------------------------- */

export interface NotificationItem {
  id: number
  targetUserId: number
  title: string
  content: string | null
  type: string
  readFlag: boolean
  createdAt: string
}

/* ------------------------- 等级配额 / 权重 /grade ------------------------- */

export interface GradeQuotaConfig {
  id: number
  deptGrade: string
  staffLevel: string
  gradeARatio: string
  gradeBRatio: string
  gradeCRatio: string
  gradeDRatio: string
  isDefault: boolean
}

export interface GradeQuotaReq {
  deptGrade: string
  staffLevel: string
  gradeARatio: string
  gradeBRatio: string
  gradeCRatio: string
  gradeDRatio: string
  isDefault?: boolean
}

export interface WeightConfig {
  id: number
  staffLevel: string
  deptWeight: string
  personalWeight: string
  periodId: number | null
}

export interface WeightConfigReq {
  staffLevel: string
  deptWeight: string
  personalWeight: string
  periodId?: number | null
}
