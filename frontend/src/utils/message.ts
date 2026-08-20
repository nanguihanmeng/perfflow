/**
 * 全局消息提示封装（Element Plus ElMessage）
 */
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElMessageBoxOptions } from 'element-plus'

/** 成功提示 */
export const toastSuccess = (message: string): void => {
  ElMessage.success(message)
}

/** 警告提示 */
export const toastWarning = (message: string): void => {
  ElMessage.warning(message)
}

/** 错误提示 */
export const toastError = (message: string): void => {
  ElMessage.error(message)
}

/**
 * 危险 / 不可逆操作确认弹窗
 * @returns resolve 表示确认，reject 表示取消
 */
export const confirmAction = (message: string, options?: Partial<ElMessageBoxOptions>): Promise<void> =>
  ElMessageBox.confirm(message, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    ...options
  }).then(() => undefined)
