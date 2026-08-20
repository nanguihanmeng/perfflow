<script setup lang="ts">
/**
 * 我的考核表（EMP）：分页列表 + 状态筛选
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

const { loading, list, pagination, query, search } = useTable<AssessmentTableResp, TablePageQuery>({
  fetcher: (q) => getTablePageApi(q).then((res) => res.data),
  defaultQuery: () => ({ state: undefined })
})

/** 状态筛选选项 */
const stateOptions = Object.values(AssessmentState)

const goDetail = (row: AssessmentTableResp): void => {
  router.push(`/me/assessment/${row.id}`)
}
</script>

<template>
  <div class="my-assessment page-container">
    <PageHeader title="我的考核表" description="查看与填报本人的季度考核表" />

    <div class="card">
      <!-- 筛选栏 -->
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
            <el-button link type="primary" size="small" @click="goDetail(row as AssessmentTableResp)">详情</el-button>
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
