<script setup lang="ts">
/**
 * 部门审核（DEPT_LEAD）：本部门「部门审核中」的考核表，仅此环节可见
 */
import { useRouter } from 'vue-router'
import { useTable } from '@/composables/useTable'
import { getTablePageApi, type TablePageQuery } from '@/api/assessment.api'
import type { AssessmentTableResp } from '@/types/dto'
import { AssessmentState } from '@/types/enums'
import { formatDateTime } from '@/utils/format'
import StateTag from '@/components/common/StateTag.vue'
import ScoreDisplay from '@/components/common/ScoreDisplay.vue'
import GradeBadge from '@/components/common/GradeBadge.vue'
import PageHeader from '@/components/common/PageHeader.vue'

const router = useRouter()

const { loading, list, pagination } = useTable<AssessmentTableResp, TablePageQuery>({
  fetcher: (q) => getTablePageApi(q).then((res) => res.data),
  defaultQuery: () => ({ state: AssessmentState.DEPT_REVIEW })
})

const goDetail = (row: AssessmentTableResp): void => {
  router.push(`/dept/review/${row.id}`)
}
</script>

<template>
  <div class="dept-review page-container">
    <PageHeader title="部门审核" description="审核本部门员工的考核表，可改自评得分、提交给领导或打回" />

    <div class="card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="periodName" label="考核周期" min-width="160" />
        <el-table-column prop="realName" label="员工" width="120" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <StateTag :state="row.state" />
          </template>
        </el-table-column>
        <el-table-column label="自评总分" width="110" align="center">
          <template #default="{ row }">
            <ScoreDisplay :value="row.selfTotalScore" />
          </template>
        </el-table-column>
        <el-table-column label="最终得分" width="110" align="center">
          <template #default="{ row }">
            <ScoreDisplay :value="row.finalScore" />
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80" align="center">
          <template #default="{ row }">
            <GradeBadge :grade="row.grade" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row as AssessmentTableResp)">审核</el-button>
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

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: $space-16;
}
</style>
