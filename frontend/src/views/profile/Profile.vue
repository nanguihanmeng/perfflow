<script setup lang="ts">
/**
 * 个人中心：展示当前用户信息（登录快照）
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { RoleLabel } from '@/types/role'
import { EMPTY_PLACEHOLDER } from '@/utils/format'

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const roleLabel = computed(() => (user.value?.role ? RoleLabel[user.value.role] : EMPTY_PLACEHOLDER))

const goChangePassword = (): void => {
  router.push('/change-password')
}

/** 个人中心信息项 */
const items = computed(() => [
  { label: '登录名', value: user.value?.username ?? EMPTY_PLACEHOLDER },
  { label: '真实姓名', value: user.value?.realName ?? EMPTY_PLACEHOLDER },
  { label: '角色', value: roleLabel.value },
  { label: '部门', value: user.value?.deptName ?? EMPTY_PLACEHOLDER },
  { label: '部门负责人', value: user.value?.deptLead ? '是' : '否' },
  { label: '令牌有效期(秒)', value: user.value?.expiresIn != null ? String(user.value.expiresIn) : EMPTY_PLACEHOLDER }
])
</script>

<template>
  <div class="profile-page page-container">
    <div class="card profile-card">
      <div class="profile-card__header">
        <el-avatar :size="56" class="profile-card__avatar">
          {{ user?.realName?.charAt(0) ?? 'U' }}
        </el-avatar>
        <div class="profile-card__identity">
          <div class="profile-card__name">{{ user?.realName || user?.username }}</div>
          <el-tag size="small" type="info" effect="plain">{{ roleLabel }}</el-tag>
        </div>
        <el-button type="primary" plain @click="goChangePassword">修改密码</el-button>
      </div>

      <el-descriptions :column="2" border class="profile-card__body">
        <el-descriptions-item v-for="item in items" :key="item.label" :label="item.label">
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.profile-page {
  max-width: 760px;
}

.profile-card {
  &__header {
    display: flex;
    align-items: center;
    gap: $space-16;
    margin-bottom: $space-20;
  }

  &__avatar {
    background-color: $color-primary;
    color: #fff;
    font-size: $font-size-2xl;
  }

  &__identity {
    flex: 1;
    display: flex;
    align-items: center;
    gap: $space-8;
  }

  &__name {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $color-text-main;
  }

  &__body {
    margin-top: $space-16;
  }
}
</style>
