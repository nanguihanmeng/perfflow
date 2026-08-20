<script setup lang="ts">
/**
 * 默认布局：侧边菜单 + 顶部栏 + 内容区
 */
import AppSidebar from '@/layout/AppSidebar.vue'
import AppTopBar from '@/layout/AppTopBar.vue'
import { useAppStore } from '@/store/app'
import { storeToRefs } from 'pinia'

const appStore = useAppStore()
const { sidebarCollapsed } = storeToRefs(appStore)
</script>

<template>
  <div class="app-layout">
    <AppSidebar />
    <div class="app-layout__main" :class="{ 'app-layout__main--collapsed': sidebarCollapsed }">
      <AppTopBar />
      <main class="app-layout__content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;

  &__main {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    transition: margin-left 0.2s ease;
  }

  &__content {
    flex: 1;
    overflow-y: auto;
    background-color: $color-bg-page;
  }
}
</style>
