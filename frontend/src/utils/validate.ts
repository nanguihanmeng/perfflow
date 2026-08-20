/**
 * 表单校验工具（与后端校验规则对齐）
 */

/** 数字字符串（BigDecimal 形式，最多两位小数） */
const DECIMAL_PATTERN = /^\d+(\.\d{1,2})?$/

/** 分数是否在 [min, max] 区间内 */
export const isScoreInRange = (value: string, min = 0, max = 100): boolean => {
  if (!DECIMAL_PATTERN.test(value)) {
    return false
  }
  const num = Number(value)
  return num >= min && num <= max
}

/** 完成率校验（0-100） */
export const isCompletionRateValid = (value: string): boolean => isScoreInRange(value, 0, 100)

/** 非空字符串 */
export const isNotBlank = (value?: string | null): boolean =>
  value !== null && value !== undefined && value.trim() !== ''

/** 最大长度 */
export const isWithinMaxLength = (value: string, max: number): boolean => value.length <= max
