/**
 * 格式化工具
 */
import dayjs from 'dayjs'

/** 空值占位符 */
export const EMPTY_PLACEHOLDER = '—'

/** 日期格式化（LocalDate） */
export const formatDate = (value?: string | null, pattern = 'YYYY-MM-DD'): string =>
  value ? dayjs(value).format(pattern) : EMPTY_PLACEHOLDER

/** 日期时间格式化（LocalDateTime） */
export const formatDateTime = (value?: string | null): string =>
  formatDate(value, 'YYYY-MM-DD HH:mm')

/**
 * 分数展示：后端为字符串 BigDecimal（如 "95.50"），保留两位小数
 */
export const formatScore = (value?: string | null, placeholder = EMPTY_PLACEHOLDER): string => {
  if (value === null || value === undefined || value === '') {
    return placeholder
  }
  const num = Number(value)
  return Number.isNaN(num) ? placeholder : num.toFixed(2)
}

/**
 * 脱敏分数：masked 时显示「**」（API.md 10.3 节）
 */
export const maskScore = (masked: boolean, value?: string | null): string =>
  masked ? '**' : formatScore(value)
