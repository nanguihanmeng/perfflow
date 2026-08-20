<script setup lang="ts">
/**
 * 领导评分（LEAD）：全部进入领导评分状态的考核表
 */
import { useRouter } from 'vue-router'
import { useTable } from '@/composables/useTable'
import { getTablePageApi, type TablePageQuery } from '@/api/assessment.api'
import type { AssessmentTableResp } from '@/types/dto'
import { AssessmentState } from '@/types/enums'
import { formatDateTime } from '@/utils/format'
import StateTag from '@/components/common/StateTag.vue'
import ScoreDisplay from '@/components/common/ScoreDisplay.vue'
import PageHeader from '@/components/common/PageHeader.vue'

const router = useRouter()

const { loading, list, pagination, query, search } = useTable<AssessmentTableResp, TablePageQuery>({
  fetcher: (q) => getTablePageApi(q).then((res) => res.data),
  defaultQuery: () => ({ state: AssessmentState.LEAD_SCORING })
})

const stateOptions = Object.values(AssessmentState)

const goDetail = (row: AssessmentTableResp): void => {
  router.push(`/lead/score/${row.id}`)
}
</script>

<template>
  <div class="lead-score page-container">
    <PageHeader title="领导评分" description="对进入评分阶段的考核表进行评分" />

    <div class="card">
      <div class="filter-bar">
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
        <el-table-column prop="periodName" label="考核周期" min-width="160" />
        <el-table-column prop="realName" label="员工" width="120" />
        <el-table-column prop="deptName" label="部门" width="140">
          <template #default="{ row }">{{ row.deptName || '—' }}</template>
        </el-table-column>
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
        <el-table-column label="领导评分" width="110" align="center">
          <template #default="{ row }">
            <ScoreDisplay :value="row.leaderScore" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row as AssessmentTableResp)">评分</el-button>
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
