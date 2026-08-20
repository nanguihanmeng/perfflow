/**
 * 枚举常量（const 对象替代 enum，见编码规范第五节），对齐后端枚举名
 */

/** 角色 */
export const Role = {
  EMP: 'EMP',
  DEPT_LEAD: 'DEPT_LEAD',
  LEAD: 'LEAD',
  HR: 'HR',
  ADMIN: 'ADMIN'
} as const
export type Role = (typeof Role)[keyof typeof Role]

/** 考核主表状态（状态机流转见 API.md 3.3 节） */
export const AssessmentState = {
  SELF_DRAFTING: 'SELF_DRAFTING',
  SELF_SUSPENDED: 'SELF_SUSPENDED',
  DEPT_REVIEW: 'DEPT_REVIEW',
  LEAD_SCORING: 'LEAD_SCORING',
  FINISHED: 'FINISHED'
} as const
export type AssessmentState = (typeof AssessmentState)[keyof typeof AssessmentState]

/** 考核行分类 */
export const RowCategory = {
  PLAN: 'PLAN',
  OPEN: 'OPEN',
  BONUS: 'BONUS'
} as const
export type RowCategory = (typeof RowCategory)[keyof typeof RowCategory]

/** 周期状态（number） */
export const PeriodStatus = {
  NOT_STARTED: 0,
  ACTIVE: 1,
  FINISHED: 2
} as const
export type PeriodStatus = (typeof PeriodStatus)[keyof typeof PeriodStatus]

/** 用户状态（number） */
export const UserStatus = {
  DISABLED: 0,
  ENABLED: 1
} as const
export type UserStatus = (typeof UserStatus)[keyof typeof UserStatus]

/** 提醒严重级别 */
export const ReminderSeverity = {
  INFO: 1,
  WARN: 2,
  URGENT: 3
} as const
export type ReminderSeverity = (typeof ReminderSeverity)[keyof typeof ReminderSeverity]
