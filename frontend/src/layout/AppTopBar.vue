<script setup lang="ts">
/**
 * 顶部栏：折叠按钮 + 面包屑 + 用户下拉
 */
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useAppStore } from '@/store/app'
import { storeToRefs } from 'pinia'
import { RoleLabel } from '@/types/role'
import { Fold, Refresh, Expand } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const { sidebarCollapsed } = storeToRefs(appStore)

const handleToggleSidebar = (): void => {
  appStore.toggleSidebar()
}

const handleCommand = async (command: string): Promise<void> => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    router.push('/change-password')
  } else if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}

const refreshPage = (): void => {
  router.go(0)
}
</script>

<template>
  <header class="app-topbar">
    <div class="app-topbar__left">
      <el-icon class="app-topbar__collapse" :size="18" @click="handleToggleSidebar">
        <Fold v-if="!sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
      <el-icon class="app-topbar__refresh" :size="16" @click="refreshPage">
        <Refresh />
      </el-icon>
    </div>

    <div class="app-topbar__right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="app-topbar__user">
          <el-avatar :size="30" class="app-topbar__avatar">
            {{ authStore.user?.realName?.charAt(0) ?? 'U' }}
          </el-avatar>
          <span class="app-topbar__username">
            {{ authStore.user?.realName || authStore.user?.username || '' }}
          </span>
          <el-tag v-if="authStore.role" size="small" type="info" effect="plain">
            {{ RoleLabel[authStore.role] }}
          </el-tag>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 $space-16;
  background-color: $color-bg-card;
  border-bottom: 1px solid $color-border;
  flex-shrink: 0;

  &__left {
    display: flex;
    align-items: center;
    gap: $space-12;
  }

  &__collapse,
  &__refresh {
    color: $color-text-secondary;
    cursor: pointer;
    transition: color 0.2s;

    &:hover {
      color: $color-primary;
    }
  }

  &__right {
    display: flex;
    align-items: center;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: $space-8;
    cursor: pointer;
    outline: none;
  }

  &__avatar {
    background-color: $color-primary;
    color: #fff;
    font-size: $font-size-sm;
  }

  &__username {
    font-size: $font-size-base;
    color: $color-text-main;
  }
}
</style>
