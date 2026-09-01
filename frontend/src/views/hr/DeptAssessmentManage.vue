<script setup lang="ts">
/**
 * 部门考核管理（PERFORMANCE_HR）：全部门/全状态进度 + KPI 明细查看 + 指标 Excel 维护
 */
import { onMounted, ref } from 'vue'
import { downloadDeptTemplateApi, importDeptApi, importDeptBatchApi, listDeptAssessmentApi } from '@/api/dept.api'
import type { DeptAssessmentResp } from '@/types/dto'
import { downloadBlob } from '@/utils/download'
import { ElMessageBox } from 'element-plus'
import { toastSuccess } from '@/utils/message'
import PageHeader from '@/components/common/PageHeader.vue'
import DeptFlowLog from '@/components/common/DeptFlowLog.vue'

const loading = ref(false)
const list = ref<DeptAssessmentResp[]>([])
const detailVisible = ref(false)
const detail = ref<DeptAssessmentResp | null>(null)
const importingId = ref<number | null>(null)
const batchImporting = ref(false)
const fileInputs = ref<Record<number, HTMLInputElement | null>>({})
const batchInput = ref<HTMLInputElement | null>(null)

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

const downloadTemplate = async (id?: number | null): Promise<void> => {
  const res = await downloadDeptTemplateApi(id ?? null)
  downloadBlob(res, `dept-template-${id ?? 'blank'}.xlsx`)
}

const pickFile = (id: number): void => {
  fileInputs.value[id]?.click()
}

const upload = async (id: number, event: Event): Promise<void> => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  importingId.value = id
  try {
    await importDeptApi(id, file)
    toastSuccess('指标导入成功')
    await load()
  } finally {
    importingId.value = null
  }
}

/** 按周期批量导入：先输入周期ID，再选择文件 */
const openBatchImport = async (): Promise<void> => {
  const result = await ElMessageBox.prompt('请输入要导入的周期 ID', '批量导入部门考核指标', {
    inputPattern: /^\d+$/,
    inputErrorMessage: '周期 ID 必须为正整数'
  })
  batchPeriodId.value = Number(result.value.trim())
  batchInput.value?.click()
}

const batchPeriodId = ref<number | null>(null)

const batchUpload = async (event: Event): Promise<void> => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || batchPeriodId.value == null) {
    return
  }
  batchImporting.value = true
  try {
    await importDeptBatchApi(batchPeriodId.value, file)
    toastSuccess('批量导入成功')
    await load()
  } finally {
    batchImporting.value = false
    batchPeriodId.value = null
  }
}
</script>

<template>
  <div class="dept-manage page-container">
    <PageHeader title="部门考核管理" description="跟进各部门考核进度，并维护各部门 KPI 指标（指标名称 / 目标值 / 评分标准 / 权重）">
      <template #actions>
        <el-button type="primary" plain :loading="batchImporting" @click="openBatchImport">批量导入</el-button>
        <el-button type="primary" plain @click="downloadTemplate()">下载空模板</el-button>
        <input
          ref="batchInput"
          type="file"
          accept=".xls,.xlsx"
          style="display: none"
          @change="(e: Event) => void batchUpload(e)"
        />
      </template>
    </PageHeader>

    <div class="card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column label="周期" min-width="140">
          <template #default="{ row }">{{ row.periodId ? `周期#${row.periodId}` : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="90" align="center">
          <template #default="{ row }">{{ row.totalScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="deptGrade" label="等级" width="80" align="center">
          <template #default="{ row }">{{ row.deptGrade ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170" align="center">
          <template #default="{ row }">{{ row.submittedAt ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row as DeptAssessmentResp)">明细</el-button>
            <el-button link type="info" size="small" @click="downloadTemplate((row as DeptAssessmentResp).id)">
              下载模板
            </el-button>
            <el-button
              link
              type="success"
              size="small"
              :loading="importingId === (row as DeptAssessmentResp).id"
              :disabled="(row as DeptAssessmentResp).status !== 1"
              @click="pickFile((row as DeptAssessmentResp).id)"
            >
              上传指标
            </el-button>
            <input
              :ref="(el) => { fileInputs[(row as DeptAssessmentResp).id] = el as HTMLInputElement }"
              type="file"
              accept=".xls,.xlsx"
              style="display: none"
              @change="(e: Event) => void upload((row as DeptAssessmentResp).id, e)"
            />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无部门考核数据" />
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
          <el-table-column prop="weight" label="权重(%)" width="90" align="center">
            <template #default="{ row }">{{ row.weight ?? '—' }}</template>
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
