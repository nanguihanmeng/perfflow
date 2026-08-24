/**
 * 角色工具函数（isAdmin / 角色导航等）
 */
import { Role, type Role as RoleType } from '@/types/enums'

/** 角色中文名映射 */
export const RoleLabel: Record<RoleType, string> = {
  [Role.EMP]: '员工',
  [Role.DEPT_LEAD]: '部门领导',
  [Role.LEAD]: '公司领导',
  [Role.PERFORMANCE_HR]: '绩效考核管理员',
  [Role.ADMIN]: '系统管理员'
}

/** 是否为管理员 */
export const isAdmin = (role?: RoleType | null): boolean => role === Role.ADMIN

/** 主表状态中文名映射（AssessmentState 枚举名 → 中文） */
export const StateLabel: Record<string, string> = {
  SELF_DRAFTING: '自评中',
  SELF_SUSPENDED: '自评挂起',
  DEPT_REVIEW: '部门审核',
  LEAD_SCORING: '领导评分',
  FINISHED: '已完成'
}

/** 行分类中文名映射 */
export const RowCategoryLabel: Record<string, string> = {
  PLAN: '个人季度工作计划',
  OPEN: '开放型指标',
  BONUS: '加减分项'
}

/** 流程动作中文名映射 */
export const ActionLabel: Record<string, string> = {
  SUBMIT: '提交自评',
  PUSH: '推送到部门审核',
  APPROVE: '提交给领导评分',
  REJECT: '打回重填',
  LEAD_SCORE: '领导评分',
  EXTEND_SUSPEND: '延长挂起',
  CREATE: '创建考核表'
}
