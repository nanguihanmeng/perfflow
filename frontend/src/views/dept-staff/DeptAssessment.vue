<script setup lang="ts">
/**
 * 部门考核填报（DEPT_STAFF）：
 * - 进入先展示 HR 已开启的部门考核选项，选择后才进入填报表单（绝不自动进入填报）
 * - HR 未开启时展示"暂未开启部门填报"空态
 * - 绩效专员仅可编辑"实际完成值"，指标/目标值/评分标准/权重由 HR 维护，得分由系统按完成率自动计算
 */
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import {
  getDeptAssessmentApi,
  getDeptAssessmentOptionsApi,
  submitDeptAssessmentApi
} from '@/api/dept.api'
import type { DeptAssessmentOptionResp, DeptAssessmentResp } from '@/types/dto'
import { confirmAction, toastSuccess } from '@/utils/message'
import PageHeader from '@/components/common/PageHeader.vue'

const authStore = useAuthStore()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const options = ref<DeptAssessmentOptionResp[]>([])
const selectedId = ref<number | null>(null)
const assessment = ref<DeptAssessmentResp | null>(null)
const deptId = ref<number>(authStore.user?.deptId ?? 0)

const statusLabel = (status: number): string => {
  const map: Record<number, string> = {
    0: '未开始', 1: '自评中', 2: '待复核', 3: '待初审', 4: '待审批', 5: '已完成'
  }
  return map[status] ?? String(status)
}

const periodLabel = (item: DeptAssessmentOptionResp): string => {
  const quarter = item.quarter > 0 ? `Q${item.quarter}` : '年度'
  return `${item.year} ${quarter} ${item.periodTypeLabel}`
}

const loadOptions = async (): Promise<void> => {
  if (!deptId.value) {
    return
  }
  loading.value = true
  try {
    const res = await getDeptAssessmentOptionsApi(deptId.value)
    options.value = res.data
    // 支持从首页待办直达：?assessmentId= 直接选中对应考核
    const fromQuery = Number(route.query.assessmentId)
    if (Number.isInteger(fromQuery) && fromQuery > 0) {
      await select(fromQuery)
    } else if (options.value.length > 0) {
      await select(options.value[0].assessmentId)
    }
  } finally {
    loading.value = false
  }
}

const select = async (id: number): Promise<void> => {
  if (selectedId.value === id && assessment.value) {
    return
  }
  selectedId.value = id
  loading.value = true
  try {
    const res = await getDeptAssessmentApi(id)
    assessment.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadOptions()
})

const submit = async (): Promise<void> => {
  if (!assessment.value) {
    return
  }
  await confirmAction('确认提交本部门考核？提交后将进入部门负责人复核。')
  saving.value = true
  try {
    await submitDeptAssessmentApi(assessment.value.id, {
      rows: assessment.value.rows.map((r) => ({
        seqNo: r.seqNo,
        actualValue: r.actualValue
      }))
    })
    toastSuccess('提交成功')
    await loadOptions()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="dept-assessment page-container">
    <PageHeader title="部门考核填报" description="选择 HR 已开启的部门考核，填报本部门经营业绩 / 运营指标 / 重点工作实际完成值" />

    <div class="card dept-select">
      <span class="dept-select__label">选择考核：</span>
      <el-select
        v-model="selectedId"
        placeholder="请选择 HR 已开启的部门考核"
        style="width: 360px"
        @change="(id: number) => void select(id)"
      >
        <el-option
          v-for="item in options"
          :key="item.assessmentId"
          :label="`${periodLabel(item)}（${statusLabel(item.status)}）`"
          :value="item.assessmentId"
        />
      </el-select>
    </div>

    <div v-loading="loading" class="card">
      <template v-if="assessment">
        <div class="dept-summary">
          <span>部门：{{ assessment.deptName }}</span>
          <span>状态：{{ statusLabel(assessment.status) }}</span>
          <span>总分：{{ assessment.totalScore ?? '—' }}</span>
          <span>部门等级：{{ assessment.deptGrade ?? '—' }}</span>
        </div>
        <el-alert
          v-if="assessment.status === 1"
          type="info"
          :closable="false"
          show-icon
          title="仅需填报『实际完成值』；指标名称、目标值、评分标准、权重由绩效考核管理员维护，得分将按完成率自动计算。"
        />
        <el-table :data="assessment.rows" border stripe>
          <el-table-column prop="rowType" label="行类型" width="110" align="center" />
          <el-table-column prop="seqNo" label="序号" width="60" align="center" />
          <el-table-column prop="indicatorName" label="指标名称" min-width="160">
            <template #default="{ row }">
              <span class="dept-cell">{{ row.indicatorName ?? '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="targetValue" label="目标值" min-width="120">
            <template #default="{ row }">
              <span class="dept-cell">{{ row.targetValue ?? '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="实际完成值" min-width="140">
            <template #default="{ row }">
              <el-input
                v-model="row.actualValue"
                :disabled="assessment.status !== 1"
                maxlength="200"
                placeholder="填写实际完成值"
              />
            </template>
          </el-table-column>
          <el-table-column prop="scoringStandard" label="评分标准" min-width="160">
            <template #default="{ row }">
              <span class="dept-cell">{{ row.scoringStandard ?? '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="100" align="center">
            <template #default="{ row }">
              <span class="dept-cell">{{ row.score ?? '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="权重(%)" width="100" align="center">
            <template #default="{ row }">
              <span class="dept-cell">{{ row.weight ?? '—' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="assessment.status === 1" class="dept-submit">
          <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="暂未开启部门填报，请联系绩效考核管理员开启" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.dept-select {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  &__label {
    font-weight: 600;
    flex-shrink: 0;
  }
}

.dept-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  font-weight: 600;
}

.dept-cell {
  white-space: pre-wrap;
}

.dept-submit {
  margin-top: 16px;
  text-align: right;
}
</style>
