<script setup lang="ts">
/**
 * 部门考核填报（DEPT_STAFF）：本部门 KPI 填报 + 提交
 */
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/store/auth'
import { getDeptAssessmentApi, submitDeptAssessmentApi } from '@/api/dept.api'
import type { DeptAssessmentResp } from '@/types/dto'
import { toastSuccess } from '@/utils/message'
import PageHeader from '@/components/common/PageHeader.vue'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const assessment = ref<DeptAssessmentResp | null>(null)
const deptId = ref<number>(authStore.user?.deptId ?? 0)

const statusLabel = (status: number): string => {
  const map: Record<number, string> = {
    0: '未开始', 1: '自评中', 2: '待复核', 3: '待初审', 4: '待审批', 5: '已完成'
  }
  return map[status] ?? String(status)
}

const load = async (): Promise<void> => {
  if (!deptId.value) {
    return
  }
  loading.value = true
  try {
    const res = await getDeptAssessmentApi(deptId.value)
    assessment.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

const submit = async (): Promise<void> => {
  if (!assessment.value) {
    return
  }
  saving.value = true
  try {
    await submitDeptAssessmentApi(deptId.value, {
      deptId: deptId.value,
      rows: assessment.value.rows.map((r) => ({
        rowType: r.rowType,
        seqNo: r.seqNo,
        indicatorName: r.indicatorName,
        targetValue: r.targetValue,
        actualValue: r.actualValue,
        scoringStandard: r.scoringStandard,
        score: r.score,
        weight: r.weight
      }))
    })
    toastSuccess('提交成功')
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="dept-assessment page-container">
    <PageHeader title="部门考核填报" description="填报本部门经营业绩 / 运营指标 / 重点工作 KPI">
      <template #actions>
        <el-tag v-if="assessment" :type="assessment.status === 1 ? 'warning' : 'info'" effect="plain">
          {{ statusLabel(assessment.status) }}
        </el-tag>
        <el-button v-if="assessment && assessment.status === 1" type="primary" :loading="saving" @click="submit">
          提交
        </el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="card">
      <template v-if="assessment">
        <div class="dept-summary">
          <span>部门：{{ assessment.deptName }}</span>
          <span>总分：{{ assessment.totalScore ?? '—' }}</span>
          <span>部门等级：{{ assessment.deptGrade ?? '—' }}</span>
        </div>
        <el-table :data="assessment.rows" border stripe>
          <el-table-column prop="rowType" label="行类型" width="110" align="center" />
          <el-table-column prop="seqNo" label="序号" width="60" align="center" />
          <el-table-column label="指标名称" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.indicatorName" :disabled="assessment.status !== 1" maxlength="200" />
            </template>
          </el-table-column>
          <el-table-column label="目标值" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.targetValue" :disabled="assessment.status !== 1" maxlength="200" />
            </template>
          </el-table-column>
          <el-table-column label="实际完成值" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.actualValue" :disabled="assessment.status !== 1" maxlength="200" />
            </template>
          </el-table-column>
          <el-table-column label="评分标准" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.scoringStandard" :disabled="assessment.status !== 1" maxlength="500" />
            </template>
          </el-table-column>
          <el-table-column label="得分" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :disabled="assessment.status !== 1" :min="0" :max="100" :precision="2" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="权重(%)" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.weight" :disabled="assessment.status !== 1" :min="0" :max="100" :precision="2" style="width: 100%" />
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else-if="!loading" description="暂无部门考核数据" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.dept-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  font-weight: 600;
}
</style>
