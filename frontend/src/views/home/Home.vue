<script setup lang="ts">
/**
 * 工作台首页：欢迎信息 + 提醒概览（待办 / 挂起预警 / 公示）
 * 注意：后端拦截器禁止 ADMIN 访问 /home/**，故 ADMIN 角色不请求提醒接口
 */
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { getRemindersApi } from '@/api/home.api'
import type { Reminder, RemindersResp } from '@/types/dto'
import { Role } from '@/types/enums'
import { RoleLabel } from '@/types/role'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const reminders = ref<RemindersResp>({
  todos: [],
  upcomingSuspends: [],
  systemNotices: []
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const roleLabel = computed(() =>
  authStore.role ? RoleLabel[authStore.role] : ''
)

const loadReminders = async (): Promise<void> => {
  if (authStore.role === Role.ADMIN) {
    return
  }
  loading.value = true
  try {
    const res = await getRemindersApi()
    reminders.value = res.data
  } finally {
    loading.value = false
  }
}

void loadReminders()

/** 严重级别文案 */
const severityLabel = (severity: number): string => {
  if (severity === 3) return '紧急'
  if (severity === 2) return '提醒'
  return '信息'
}

const severityType = (severity: number): 'info' | 'warning' | 'danger' => {
  if (severity === 3) return 'danger'
  if (severity === 2) return 'warning'
  return 'info'
}

/** 各角色允许跳转的个人考核表详情路径（白名单，避免拼接任意路径） */
const detailPathByRole: Record<string, string> = {
  [Role.PERFORMANCE_HR]: '/hr/table/',
  [Role.DEPT_LEAD]: '/dept/review/',
  [Role.LEAD]: '/lead/score/',
  [Role.EMP]: '/me/assessment/'
}

/** 部门考核相关角色跳转路径（配合 targetAssessmentId） */
const deptPathByRole: Record<string, string> = {
  [Role.DEPT_STAFF]: '/dept-staff/assessment?assessmentId=',
  [Role.DEPT_LEAD]: '/dept/dept-review?assessmentId=',
  [Role.OPERATION]: '/operation/dept-audit?assessmentId=',
  [Role.COMMITTEE]: '/committee/approve?assessmentId='
}

/** 挂起预警按周期跳转（HR/部门负责人查看对应周期考核列表） */
const suspendPathByRole: Record<string, string> = {
  [Role.PERFORMANCE_HR]: '/hr/list?periodId=',
  [Role.DEPT_LEAD]: '/dept/review?periodId='
}

const goToTarget = (reminder: Reminder): void => {
  const role = authStore.role ?? ''
  // 部门考核待办：按 targetAssessmentId 跳转到各角色对应页面
  if (reminder.bizType === 'DEPT' && reminder.targetAssessmentId != null && reminder.targetAssessmentId > 0) {
    const base = deptPathByRole[role]
    if (base) {
      router.push(`${base}${reminder.targetAssessmentId}`)
    }
    return
  }
  // 挂起预警：按 targetPeriodId 跳转到周期相关列表
  if (reminder.bizType === 'SUSPEND_SOON' && reminder.targetPeriodId != null && reminder.targetPeriodId > 0) {
    const base = suspendPathByRole[role]
    if (base) {
      router.push(`${base}${reminder.targetPeriodId}`)
    }
    return
  }
  // 个人考核待办：targetTableId 必须为正整数（后端 DB 主键），非白名单角色不跳转
  if (reminder.targetTableId == null || !Number.isInteger(reminder.targetTableId) || reminder.targetTableId <= 0) {
    return
  }
  const base = detailPathByRole[role]
  if (base) {
    router.push(`${base}${reminder.targetTableId}`)
  }
}
</script>

<template>
  <div class="home-page page-container">
    <!-- 欢迎卡片 -->
    <div class="card home-welcome">
      <h2 class="home-welcome__title">
        {{ greeting }}，{{ authStore.user?.realName || authStore.user?.username }}
      </h2>
      <p class="home-welcome__desc">
        <template v-if="authStore.role !== Role.ADMIN">今日日期：{{ dayjs().format('YYYY-MM-DD') }}（{{ roleLabel }}）</template>
        <template v-else>您正在使用系统管理员账号，无法访问业务数据</template>
      </p>
    </div>

    <!-- 管理员不展示提醒卡片 -->
    <template v-if="authStore.role !== Role.ADMIN">
      <div class="home-stats">
        <div class="card home-stat">
          <div class="home-stat__num">{{ reminders.todos.length }}</div>
          <div class="home-stat__label">待办事项</div>
        </div>
        <div class="card home-stat">
          <div class="home-stat__num">{{ reminders.upcomingSuspends.length }}</div>
          <div class="home-stat__label">挂起预警</div>
        </div>
        <div class="card home-stat">
          <div class="home-stat__num">{{ reminders.systemNotices.length }}</div>
          <div class="home-stat__label">公示提醒</div>
        </div>
      </div>

      <el-skeleton :loading="loading" animated :rows="4">
        <template #default>
          <div class="card home-reminder">
            <h3 class="home-reminder__title">提醒</h3>
            <el-empty
              v-if="
                reminders.todos.length === 0 &&
                reminders.upcomingSuspends.length === 0 &&
                reminders.systemNotices.length === 0
              "
              description="暂无提醒"
            />
            <template v-else>
              <div class="home-reminder__group">
                <div class="home-reminder__group-title">待办事项</div>
                <el-empty v-if="reminders.todos.length === 0" description="无待办" :image-size="60" />
                <div v-for="(item, i) in reminders.todos" :key="item.targetTableId ?? `todo-${i}`" class="home-reminder__item">
                  <el-tag :type="severityType(item.severity)" size="small" effect="plain">
                    {{ severityLabel(item.severity) }}
                  </el-tag>
                  <span class="home-reminder__item-title" @click="goToTarget(item)">
                    {{ item.title }}
                  </span>
                </div>
              </div>
              <el-divider />
              <div class="home-reminder__group">
                <div class="home-reminder__group-title">挂起预警</div>
                <el-empty v-if="reminders.upcomingSuspends.length === 0" description="无预警" :image-size="60" />
                <div v-for="(item, i) in reminders.upcomingSuspends" :key="`suspend-${i}`" class="home-reminder__item">
                  <el-tag :type="severityType(item.severity)" size="small" effect="plain">
                    {{ severityLabel(item.severity) }}
                  </el-tag>
                  <span class="home-reminder__item-title" @click="goToTarget(item)">
                    {{ item.title }}
                  </span>
                  <span class="home-reminder__item-desc">{{ item.description }}</span>
                </div>
              </div>
              <el-divider />
              <div class="home-reminder__group">
                <div class="home-reminder__group-title">公示提醒</div>
                <el-empty v-if="reminders.systemNotices.length === 0" description="无公示" :image-size="60" />
                <div v-for="(item, i) in reminders.systemNotices" :key="`notice-${i}`" class="home-reminder__item">
                  <el-tag :type="severityType(item.severity)" size="small" effect="plain">
                    {{ severityLabel(item.severity) }}
                  </el-tag>
                  <span class="home-reminder__item-title">{{ item.title }}</span>
                  <span class="home-reminder__item-desc">{{ item.description }}</span>
                </div>
              </div>
            </template>
          </div>
        </template>
      </el-skeleton>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.home-page {
  max-width: 1080px;
}

.home-welcome {
  &__title {
    font-size: $font-size-2xl;
    font-weight: 600;
    color: $color-text-main;
  }

  &__desc {
    margin-top: $space-8;
    font-size: $font-size-base;
    color: $color-text-secondary;
  }
}

.home-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-16;
  margin: $space-16 0;
}

.home-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $space-20;

  &__num {
    font-size: 28px;
    font-weight: 700;
    color: $color-primary;
  }

  &__label {
    margin-top: $space-8;
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }
}

.home-reminder {
  &__title {
    font-size: $font-size-lg;
    font-weight: 600;
    margin-bottom: $space-16;
  }

  &__group-title {
    font-size: $font-size-base;
    font-weight: 600;
    color: $color-text-main;
    margin-bottom: $space-8;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: $space-8;
    padding: $space-8 0;

    &-title {
      font-size: $font-size-base;
      color: $color-text-main;
      cursor: pointer;

      &:hover {
        color: $color-primary;
      }
    }

    &-desc {
      font-size: $font-size-sm;
      color: $color-text-secondary;
    }
  }
}
</style>
