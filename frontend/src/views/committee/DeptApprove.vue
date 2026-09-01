<script setup lang="ts">
/**
 * 部门考核最终审批（COMMITTEE）：对待审批的部门考核最终审批，查看 KPI 明细
 */
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { approveDeptAssessmentApi, listDeptAssessmentApi } from '@/api/dept.api'
import type { DeptAssessmentResp } from '@/types/dto'
import { confirmAction, toastSuccess } from '@/utils/message'
import PageHeader from '@/components/common/PageHeader.vue'
import DeptFlowLog from '@/components/common/DeptFlowLog.vue'


const route = useRoute()
const loading = ref(false)
const list = ref<DeptAssessmentResp[]>([])
const detailVisible = ref(false)
const detail = ref<DeptAssessmentResp | null>(null)

const statusLabel = (status: number): string => {
  const map: Record<number, string> = {
    0: '未开始', 1: '自评中', 2: '待复核', 3: '待初审', 4: '待审批', 5: '已完成'
  }
  return map[status] ?? String(status)
}

const load = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await listDeptAssessmentApi()
    list.value = res.data
    // 支持从首页待办直达：?assessmentId= 打开对应考核明细
    const fromQuery = Number(route.query.assessmentId)
    if (Number.isInteger(fromQuery) && fromQuery > 0) {
      const target = list.value.find((d) => d.id === fromQuery)
      if (target) {
        detail.value = target
        detailVisible.value = true
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

const showDetail = (row: DeptAssessmentResp): void => {
  detail.value = row
  detailVisible.value = true
}

const handleApprove = async (row: DeptAssessmentResp): Promise<void> => {
  await confirmAction(`确认最终审批「${row.deptName}」的部门考核？审批后自动计算部门等级。`)
  await approveDeptAssessmentApi(row.id)
  toastSuccess('审批完成')
  detailVisible.value = false
  await load()
}
</script>

<template>
  <div class="dept-approve page-container">
    <PageHeader title="部门考核审批" description="绩效委员会最终审批部门考核" />

    <div class="card">
      <el-table v-loading="loading" :data="list.filter((d) => d.status === 4)" border stripe>
        <el-table-column prop="deptName" label="部门" min-width="130" />
        <el-table-column label="周期" min-width="140">
          <template #default="{ row }">{{ row.periodId ? `周期#${row.periodId}` : '—' }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="90" align="center">
          <template #default="{ row }">{{ row.totalScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170" align="center">
          <template #default="{ row }">{{ row.submittedAt ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row as DeptAssessmentResp)">明细</el-button>
            <el-button link type="success" size="small" @click="handleApprove(row as DeptAssessmentResp)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.filter((d) => d.status === 4).length === 0" description="暂无待审批的部门考核" />
    </div>

    <el-dialog v-model="detailVisible" title="部门考核明细" width="860px">
      <template v-if="detail">
        <div class="dept-detail-summary">
          <span>部门：{{ detail.deptName }}</span>
          <span>状态：{{ statusLabel(detail.status) }}</span>
          <span>总分：{{ detail.totalScore ?? '—' }}</span>
          <span>等级：{{ detail.deptGrade ?? '—' }}</span>
        </div>
        <el-table :data="detail.rows" border stripe max-height="480">
          <el-table-column prop="rowType" label="行类型" width="100" align="center" />
          <el-table-column prop="seqNo" label="序号" width="60" align="center" />
          <el-table-column prop="indicatorName" label="指标名称" min-width="140">
            <template #default="{ row }">{{ row.indicatorName ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="targetValue" label="目标值" min-width="110">
            <template #default="{ row }">{{ row.targetValue ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="actualValue" label="实际完成值" min-width="110">
            <template #default="{ row }">{{ row.actualValue ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="scoringStandard" label="评分标准" min-width="140">
            <template #default="{ row }">{{ row.scoringStandard ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="80" align="center">
            <template #default="{ row }">{{ row.score ?? '—' }}</template>
          </el-table-column>
        </el-table>
        <div class="dept-detail-flow">
          <DeptFlowLog v-if="detailVisible && detail" :assessment-id="detail.id" />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.dept-detail-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  font-weight: 600;
}

.dept-detail-flow {
  margin-top: 16px;
}
</style>
