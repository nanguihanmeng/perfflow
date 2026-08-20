<script setup lang="ts">
/**
 * 403 无权限页
 */
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = (): void => {
  router.push('/')
}
</script>

<template>
  <div class="error-page">
    <div class="error-page__code">403</div>
    <p class="error-page__message">抱歉，您没有权限访问该页面</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: $color-bg-page;

  &__code {
    font-size: 72px;
    font-weight: 700;
    color: $color-primary;
  }

  &__message {
    margin: $space-16 0 $space-24;
    color: $color-text-secondary;
  }
}
</style>
