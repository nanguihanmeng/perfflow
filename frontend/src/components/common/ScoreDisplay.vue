<script setup lang="ts">
/**
 * 分数展示组件：处理字符串分数与脱敏（masked → 「**」）
 */
import { computed } from 'vue'
import { maskScore } from '@/utils/format'

interface Props {
  /** 分数值（字符串 BigDecimal） */
  value: string | null | undefined
  /** 是否脱敏（后端 RowResp.masked） */
  masked?: boolean
  /** 空值占位符 */
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  masked: false,
  placeholder: '—'
})

const display = computed(() => maskScore(props.masked, props.value ?? ''))
</script>

<template>
  <span class="score-display" :class="{ 'score-display--masked': masked }">
    {{ display }}
  </span>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.score-display {
  font-variant-numeric: tabular-nums;
  color: $color-text-main;

  &--masked {
    color: $color-text-placeholder;
    letter-spacing: 2px;
  }
}
</style>
