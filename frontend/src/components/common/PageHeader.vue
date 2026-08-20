<script setup lang="ts">
/**
 * 页面头部：标题 + 描述 + 右侧操作区
 */
interface Props {
  title: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  description: ''
})
</script>

<template>
  <div class="page-header">
    <div class="page-header__left">
      <h2 class="page-header__title">{{ title }}</h2>
      <p v-if="description" class="page-header__desc">{{ description }}</p>
    </div>
    <div class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $space-16;
  margin-bottom: $space-16;

  &__title {
    font-size: $font-size-xl;
    font-weight: 600;
    color: $color-text-main;
  }

  &__desc {
    margin-top: $space-4;
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: $space-8;
    flex-shrink: 0;
  }
}
</style>
