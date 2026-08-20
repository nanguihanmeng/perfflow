<script setup lang="ts">
/**
 * 考核列表（HR / LEAD）
 * - LEAD：只读查看全部考核表
 * - HR：额外支持导出 Excel / 打印 HTML
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTable } from '@/composables/useTable'
import { deleteTableApi, exportExcelApi, getPrintHtmlApi, getTablePageApi, type TablePageQuery } from '@/api/assessment.api'
import { getPeriodListApi } from '@/api/period.api'
import type { AssessmentTableResp, PeriodResp } from '@/types/dto'
import { AssessmentState, Role } from '@/types/enums'
import { downloadBlob } from '@/utils/download'
import { confirmAction, toastSuccess } from '@/utils/message'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/store/auth'
import StateTag from '@/components/common/StateTag.vue'
import ScoreDisplay from '@/components/common/ScoreDisplay.vue'
import GradeBadge from '@/components/common/GradeBadge.vue'
import PageHeader from '@/components/common/PageHeader.vue'

const router = useRouter()
const authStore = useAuthStore()

const isHr = computed(() => authStore.role === Role.HR)
const periods = ref<PeriodResp[]>([])

const { loading, list, pagination, query, search } = useTable<AssessmentTableResp, TablePageQuery>({
  fetcher: (q) => getTablePageApi(q).then((res) => res.data),
  defaultQuery: () => ({ periodId: undefined, state: undefined })
})

const stateOptions = Object.values(AssessmentState)

onMounted(async () => {
  const res = await getPeriodListApi()
  periods.value = res.data
})

const goDetail = (row: AssessmentTableResp): void => {
  router.push(`/hr/table/${row.id}`)
}

const handleDelete = async (row: AssessmentTableResp): Promise<void> => {
  await confirmAction(`确认删除「${row.realName}」的「${row.periodName}」考核表？将同时删除其行数据与流程记录，且不可恢复。`)
  await deleteTableApi(row.id)
  toastSuccess('考核表已删除')
  await search()
}

const exportLoading = ref(false)
const handleExport = async (): Promise<void> => {
  exportLoading.value = true
  try {
    const res = await exportExcelApi((query.periodId as number | undefined) ?? undefined)
    downloadBlob(res, `assessment_${(query.periodId as number | undefined) ?? 'all'}.xlsx`)
    toastSuccess('导出成功')
  } finally {
    exportLoading.value = false
  }
}

const printLoading = ref(false)
const handlePrint = async (): Promise<void> => {
  printLoading.value = true
  try {
    const html = await getPrintHtmlApi((query.periodId as number | undefined) ?? undefined)
    const win = window.open('', '_blank')
    if (win) {
      win.document.open()
      win.document.write(html)
      win.document.close()
      win.focus()
      win.print()
    }
  } finally {
    printLoading.value = false
  }
}
</script>

<template>
  <div class="assessment-list page-container">
    <PageHeader title="考核列表" description="查看各周期员工考核情况">
      <template #actions>
        <template v-if="isHr">
          <el-button :loading="exportLoading" @click="handleExport">导出 Excel</el-button>
          <el-button :loading="printLoading" @click="handlePrint">打印</el-button>
        </template>
      </template>
    </PageHeader>

    <div class="card">
      <div class="filter-bar">
        <el-select
          v-model="query.periodId"
          placeholder="全部周期"
          clearable
          style="width: 200px"
          @change="search"
        >
          <el-option v-for="p in periods" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select
          v-model="query.state"
          placeholder="全部状态"
          clearable
          style="width: 180px"
          @change="search"
        >
          <el-option v-for="s in stateOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="periodName" label="考核周期" min-width="150" />
        <el-table-column prop="realName" label="员工" width="110" />
        <el-table-column prop="deptName" label="部门" width="130">
          <template #default="{ row }">{{ row.deptName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <StateTag :state="row.state" />
          </template>
        </el-table-column>
        <el-table-column label="自评总分" width="100" align="center">
          <template #default="{ row }">
            <ScoreDisplay :value="row.selfTotalScore" />
          </template>
        </el-table-column>
        <el-table-column label="领导评分" width="100" align="center">
          <template #default="{ row }">
            <ScoreDisplay :value="row.leaderScore" />
          </template>
        </el-table-column>
        <el-table-column label="最终得分" width="100" align="center">
          <template #default="{ row }">
            <ScoreDisplay :value="row.finalScore" />
          </template>
        </el-table-column>
        <el-table-column label="等级" width="70" align="center">
          <template #default="{ row }">
            <GradeBadge :grade="row.grade" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150" align="center">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row as AssessmentTableResp)">详情</el-button>
            <el-button
              v-if="isHr"
              link
              type="danger"
              size="small"
              @click="handleDelete(row as AssessmentTableResp)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.pageNo"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.filter-bar {
  display: flex;
  gap: $space-12;
  margin-bottom: $space-16;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: $space-16;
}
</style>
