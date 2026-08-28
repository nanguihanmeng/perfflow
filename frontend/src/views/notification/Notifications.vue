<script setup lang="ts">
/**
 * 我的通知（全员）：站内通知列表 + 标记已读
 */
import { onMounted, ref } from 'vue'
import { getNotificationsApi, markNotificationReadApi } from '@/api/notification.api'
import type { NotificationItem } from '@/types/dto'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const list = ref<NotificationItem[]>([])

const load = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getNotificationsApi()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

const markRead = async (item: NotificationItem): Promise<void> => {
  if (item.readFlag) {
    return
  }
  await markNotificationReadApi(item.id)
  item.readFlag = true
}
</script>

<template>
  <div class="notifications page-container">
    <PageHeader title="我的通知" description="考核提醒 / 催办 / 系统通知" />

    <div class="card">
      <el-empty v-if="!loading && list.length === 0" description="暂无通知" />
      <el-timeline v-else style="padding: 16px">
        <el-timeline-item
          v-for="item in list"
          :key="item.id"
          :timestamp="item.createdAt"
          placement="top"
          @click="markRead(item)"
        >
          <div class="notice-item" :class="{ unread: !item.readFlag }">
            <div class="notice-title">
              {{ item.title }}
              <el-tag v-if="!item.readFlag" size="small" type="danger" effect="plain">未读</el-tag>
            </div>
            <div v-if="item.content" class="notice-content">{{ item.content }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<style scoped lang="scss">
.notice-item {
  cursor: pointer;
  &.unread .notice-title {
    font-weight: 700;
  }
  .notice-title {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .notice-content {
    color: #888;
    margin-top: 4px;
    font-size: 13px;
  }
}
</style>
