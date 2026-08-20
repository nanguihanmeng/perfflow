<script setup lang="ts">
/**
 * 侧边菜单：按角色显隐
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useAppStore } from '@/store/app'
import { storeToRefs } from 'pinia'
import { Role } from '@/types/enums'
import { RoleLabel } from '@/types/role'
import { Menu as MenuIcon } from '@element-plus/icons-vue'

interface MenuItem {
  title: string
  path: string
  roles: string[]
}

const MENUS: MenuItem[] = [
  { title: '工作台', path: '/home', roles: [Role.EMP, Role.DEPT_LEAD, Role.LEAD, Role.HR, Role.ADMIN] },
  { title: '个人中心', path: '/profile', roles: [Role.EMP, Role.DEPT_LEAD, Role.LEAD, Role.HR, Role.ADMIN] },
  { title: '我的考核表', path: '/me/assessment', roles: [Role.EMP] },
  { title: '部门审核', path: '/dept/review', roles: [Role.DEPT_LEAD] },
  { title: '领导评分', path: '/lead/score', roles: [Role.LEAD] },
  { title: '周期管理', path: '/hr/period', roles: [Role.HR] },
  { title: '考核列表', path: '/hr/list', roles: [Role.LEAD, Role.HR] },
  { title: '用户管理', path: '/admin/users', roles: [Role.ADMIN] },
  { title: '部门管理', path: '/admin/depts', roles: [Role.ADMIN] }
]

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const { sidebarCollapsed } = storeToRefs(appStore)

const visibleMenus = computed(() =>
  MENUS.filter((menu) => authStore.role !== null && menu.roles.includes(authStore.role))
)

const activePath = computed(() => route.path)

const handleSelect = (index: string): void => {
  router.push(index)
}
</script>

<template>
  <aside class="app-sidebar" :class="{ 'app-sidebar--collapsed': sidebarCollapsed }">
    <div class="app-sidebar__logo">
      <el-icon :size="22"><MenuIcon /></el-icon>
      <span v-show="!sidebarCollapsed" class="app-sidebar__title">PerfFlow</span>
    </div>

    <el-menu
      class="app-sidebar__menu"
      :default-active="activePath"
      :collapse="sidebarCollapsed"
      :collapse-transition="false"
      @select="handleSelect"
    >
      <el-menu-item v-for="menu in visibleMenus" :key="menu.path" :index="menu.path">
        <span>{{ menu.title }}</span>
      </el-menu-item>
    </el-menu>

    <div v-show="!sidebarCollapsed" class="app-sidebar__footer">
      <el-tag size="small" type="info" effect="plain">
        {{ authStore.role ? RoleLabel[authStore.role] : '' }}
      </el-tag>
    </div>
  </aside>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-sidebar {
  display: flex;
  flex-direction: column;
  width: 220px;
  background-color: $color-bg-card;
  border-right: 1px solid $color-border;
  transition: width 0.2s ease;
  flex-shrink: 0;

  &--collapsed {
    width: 64px;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: $space-8;
    height: 56px;
    padding: 0 $space-16;
    color: $color-primary;
    font-weight: 600;
    font-size: $font-size-lg;
    border-bottom: 1px solid $color-border;
    white-space: nowrap;
  }

  &__menu {
    flex: 1;
    border-right: none;
    overflow-y: auto;
    --el-menu-item-height: 44px;
  }

  &__footer {
    padding: $space-12 $space-16;
    border-top: 1px solid $color-border;
  }
}
</style>
