<script setup lang="ts">
/**
 * 主表状态标签（状态 → 色值映射，商务简约）
 */
import { computed } from 'vue'
import { AssessmentState } from '@/types/enums'
import { StateLabel } from '@/types/role'

interface Props {
  /** 状态枚举名（如 SELF_DRAFTING） */
  state: string
}

const props = defineProps<Props>()

/** 状态 → Element Plus tag 类型 */
const STATE_TAG_TYPE: Record<string, 'info' | 'primary' | 'success' | 'warning' | 'danger'> = {
  [AssessmentState.SELF_DRAFTING]: 'primary',
  [AssessmentState.SELF_SUSPENDED]: 'warning',
  [AssessmentState.DEPT_REVIEW]: 'warning',
  [AssessmentState.LEAD_SCORING]: 'primary',
  [AssessmentState.FINISHED]: 'success'
}

const tagType = computed(
  () => STATE_TAG_TYPE[props.state] ?? 'info'
)

const label = computed(() => StateLabel[props.state] ?? props.state)
</script>

<template>
  <el-tag :type="tagType" size="small" effect="light" disable-transitions>
    {{ label }}
  </el-tag>
</template>
