<script setup lang="ts">
/**
 * 部门考核最终审批（COMMITTEE）：对待审批的部门考核最终审批
 */
import { onMounted, ref } from 'vue'
import { approveDeptAssessmentApi, listDeptAssessmentApi } from '@/api/dept.api'
import type { DeptAssessmentResp } from '@/types/dto'
import { confirmAction, toastSuccess } from '@/utils/message'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const list = ref<DeptAssessmentResp[]>([])

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

const handleApprove = async (row: DeptAssessmentResp): Promise<void> => {
  await confirmAction(`确认最终审批「${row.deptName}」的部门考核？审批后自动计算部门等级。`)
  await approveDeptAssessmentApi(row.deptId)
  toastSuccess('审批完成')
  await load()
}
</script>

<template>
  <div class="dept-approve page-container">
    <PageHeader title="部门考核审批" description="绩效委员会最终审批部门考核" />

    <div class="card">
      <el-table v-loading="loading" :data="list.filter((d) => d.status === 4)" border stripe>
        <el-table-column prop="deptName" label="部门" min-width="130" />
        <el-table-column prop="totalScore" label="总分" width="100" align="center">
          <template #default="{ row }">{{ row.totalScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170" align="center">
          <template #default="{ row }">{{ row.submittedAt ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="handleApprove(row as DeptAssessmentResp)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.filter((d) => d.status === 4).length === 0" description="暂无待审批的部门考核" />
    </div>
  </div>
</template>
