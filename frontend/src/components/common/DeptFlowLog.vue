<script setup lang="ts">
/**
 * 部门考核流程记录时间线：展示各环节经办人、状态流转与意见
 */
import { onMounted, ref } from 'vue'
import { getDeptFlowLogsApi } from '@/api/dept.api'
import type { DeptFlowLogResp } from '@/types/dto'
import { RoleLabel } from '@/types/role'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{ assessmentId: number }>()

const loading = ref(false)
const logs = ref<DeptFlowLogResp[]>([])

/** 部门考核状态编码 → 中文 */
const DeptStateLabel: Record<number, string> = {
  0: '未开始',
  1: '自评中',
  2: '待复核',
  3: '待初审',
  4: '待审批',
  5: '已完成'
}

/** 部门考核动作 → 中文 */
const DeptActionLabel: Record<string, string> = {
  SUBMIT: '专员提交',
  REVIEW_PASS: '负责人复核通过',
  REVIEW_REJECT: '负责人退回',
  AUDIT_PASS: '运营初审通过',
  AUDIT_REJECT: '运营退回整改',
  APPROVE: '委员会审批'
}

const load = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getDeptFlowLogsApi(props.assessmentId)
    logs.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

defineExpose({ reload: load })
</script>

<template>
  <div v-loading="loading" class="dept-flow-log">
    <el-timeline v-if="logs.length > 0">
      <el-timeline-item
        v-for="log in logs"
        :key="log.id"
        :timestamp="formatDateTime(log.createdAt)"
        placement="top"
      >
        <div class="dept-flow-log__item">
          <span class="dept-flow-log__action">{{ DeptActionLabel[log.action] || '状态变更' }}</span>
          <span class="dept-flow-log__states">
            {{ DeptStateLabel[log.fromStatus] }} → {{ DeptStateLabel[log.toStatus] }}
          </span>
          <span class="dept-flow-log__operator">
            {{ log.operatorName }}（{{ (RoleLabel as Record<string, string>)[log.operatorRole ?? ''] || log.operatorRole }}）
          </span>
        </div>
        <div v-if="log.comment" class="dept-flow-log__comment">{{ log.comment }}</div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else-if="!loading" description="暂无流程记录" :image-size="60" />
  </div>
</template>

<style scoped lang="scss">
.dept-flow-log {
  &__item {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }

  &__action {
    font-weight: 600;
    color: #182030;
  }

  &__states {
    font-size: 13px;
    color: #506070;
  }

  &__operator {
    font-size: 13px;
    color: #506070;
  }

  &__comment {
    margin-top: 4px;
    font-size: 13px;
    color: #909aa6;
  }
}
</style>
