/**
 * 站内通知接口（/notifications）
 */
import { request } from '@/utils/request'
import type { NotificationItem } from '@/types/dto'
import type { Result } from '@/types/result'

/** 我的通知列表 */
export const getNotificationsApi = (): Promise<Result<NotificationItem[]>> =>
  request.get<NotificationItem[]>('/notifications')

/** 未读通知数量 */
export const getUnreadCountApi = (): Promise<Result<{ count: number }>> =>
  request.get<{ count: number }>('/notifications/unread-count')

/** 标记已读 */
export const markNotificationReadApi = (id: number): Promise<Result<void>> =>
  request.post<void>(`/notifications/${id}/read`)
