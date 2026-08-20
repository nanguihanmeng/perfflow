<script setup lang="ts">
/**
 * 等级徽章（A/B/C/D → 商务配色）
 */
import { computed } from 'vue'

interface Props {
  /** 等级：A/B/C/D 或空 */
  grade?: string | null
}

const props = defineProps<Props>()

const GRADE_COLOR: Record<string, string> = {
  A: '#38a169',
  B: '#2b6cb0',
  C: '#d69e2e',
  D: '#e53e3e'
}

const color = computed(() => (props.grade ? GRADE_COLOR[props.grade.toUpperCase()] : '#a0aec0'))
</script>

<template>
  <span
    v-if="grade"
    class="grade-badge"
    :style="{ backgroundColor: color }"
  >
    {{ grade.toUpperCase() }}
  </span>
  <span v-else class="grade-badge grade-badge--empty">—</span>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.grade-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 22px;
  padding: 0 $space-8;
  border-radius: 3px;
  color: #fff;
  font-size: $font-size-sm;
  font-weight: 600;

  &--empty {
    background-color: $color-border;
    color: $color-text-secondary;
  }
}
</style>
