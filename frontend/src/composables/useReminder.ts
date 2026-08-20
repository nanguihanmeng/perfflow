/**
 * 首页提醒组合式函数：加载提醒数据并维护加载态
 */
import { onMounted, ref } from 'vue'
import { getRemindersApi } from '@/api/home.api'
import type { Reminder, RemindersResp } from '@/types/dto'

export const useReminder = () => {
  const loading = ref(false)
  const reminders = ref<RemindersResp>({
    todos: [],
    upcomingSuspends: [],
    systemNotices: []
  })

  const load = async (): Promise<void> => {
    loading.value = true
    try {
      const res = await getRemindersApi()
      reminders.value = res.data
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    void load()
  })

  /** 合并三类提醒并按严重级别排序 */
  const allReminders = (): Reminder[] => [
    ...reminders.value.todos,
    ...reminders.value.upcomingSuspends,
    ...reminders.value.systemNotices
  ]

  return { loading, reminders, load, allReminders }
}
