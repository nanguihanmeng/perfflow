/**
 * 统一响应体与错误码定义，对齐后端 Result<T> / ResultCode
 */

/** 后端统一响应体 Result<T> */
export interface Result<T = unknown> {
  /** 0 成功；非 0 见 ResultCode */
  code: number
  /** 成功时为 "ok"，失败时为错误描述 */
  message: string
  /** 业务数据 */
  data: T
}

/**
 * 业务错误码（const 对象替代 enum，见编码规范）
 */
export const ResultCode = {
  SUCCESS: 0,
  BAD_REQUEST: 1001,
  UNAUTHORIZED: 1002,
  FORBIDDEN: 1003,
  NOT_FOUND: 1004,
  METHOD_NOT_ALLOWED: 1005,
  VALIDATION_FAILED: 1006,
  ADMIN_NO_BUSINESS_VISIBILITY: 1007,
  LOGIN_INVALID: 1101,
  ACCOUNT_DISABLED: 1102,
  TOKEN_INVALID: 1103,
  TOKEN_EXPIRED: 1104,
  MUST_CHANGE_PASSWORD_INIT: 1105,
  INIT_PASSWORD: 1106,
  PERIOD_NOT_OPEN: 2001,
  PERMISSION_DENIED_FOR_ROW: 2002,
  STATE_NOT_ALLOWED: 2010,
  SUBMIT_REQUIRED_FIELDS: 2011,
  REJECT_COMMENT_REQUIRED: 2012,
  ADJUST_REMARK_REQUIRED: 2013,
  LEADER_SCORE_REQUIRED: 2020,
  SCORE_OUT_OF_RANGE: 2021,
  EXTEND_OVER_LIMIT: 2030,
  INTERNAL_ERROR: 3000
} as const

export type ResultCodeValue = (typeof ResultCode)[keyof typeof ResultCode]

/** 请求层统一抛出的错误 */
export class ApiError extends Error {
  /** 业务码，见 ResultCode */
  code: number
  /** HTTP 状态码 */
  httpStatus: number

  constructor(message: string, code: number, httpStatus: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
  }
}

/** MyBatis-Plus 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
  searchCount: boolean
}
