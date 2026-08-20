/**
 * 首页模块接口（/home）
 */
import { request } from '@/utils/request'
import type { RemindersResp } from '@/types/dto'
import type { Result } from '@/types/result'

/** 首页提醒（待办 / 挂起预警 / 公示） */
export const getRemindersApi = (): Promise<Result<RemindersResp>> =>
  request.get<RemindersResp>('/home/reminders')
