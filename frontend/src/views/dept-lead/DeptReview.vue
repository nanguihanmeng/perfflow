<script setup lang="ts">
/**
 * 部门考核管理（DEPT_LEAD）：本部门全部考核表（含历史周期与已完成结果）
 * - DEPT_REVIEW 状态可进入审核（通过/打回/调分）
 * - 其他状态（已提交领导/已完成等）可查看详情与留痕
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTable } from '@/composables/useTable'
import { getTablePageApi, type TablePageQuery } from '@/api/assessment.api'
import { getPeriodListApi } from '@/api/period.api'
import type { AssessmentTableResp, PeriodResp } from '@/types/dto'
import { AssessmentState } from '@/types/enums'
import { formatDateTime } from '@/utils/format'
import StateTag from '@/components/common/StateTag.vue'
import ScoreDisplay from '@/components/common/ScoreDisplay.vue'
import GradeBadge from '@/components/common/GradeBadge.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { StateLabel } from '@/types/role'

const router = useRouter()

const periods = ref<PeriodResp[]>([])

const { loading, list, pagination, query, search } = useTable<AssessmentTableResp, TablePageQuery>({
  fetcher: (q) => getTablePageApi(q).then((res) => res.data),
  defaultQuery: () => ({ periodId: undefined, state: undefined })
})

/** 状态筛选选项（全部状态，便于查历史） */
const stateOptions = Object.values(AssessmentState).map((s) => ({ value: s, label: StateLabel[s] ?? s }))

onMounted(async () => {
  const res = await getPeriodListApi()
  periods.value = res.data
})

const goDetail = (row: AssessmentTableResp): void => {
  // 详情页按状态区分：审核中可操作（通过/打回/调分），其余状态只读查看
  router.push(`/dept/review/${row.id}`)
}

/** 操作按钮文案：审核中 → 审核，其他状态 → 查看 */
const actionLabel = (state?: string): string =>
  state === AssessmentState.DEPT_REVIEW ? '审核' : '查看'
</script>

<template>
  <div class="dept-review page-container">
    <PageHeader title="部门考核管理" description="查询与审核本部门员工的考核表，含历史周期与已完成结果" />

    <div class="card">
      <!-- 筛选栏 -->
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
          <el-option v-for="s in stateOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </div>

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
            <el-button link type="primary" size="small" @click="goDetail(row as AssessmentTableResp)">
              {{ actionLabel(row.state) }}
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
