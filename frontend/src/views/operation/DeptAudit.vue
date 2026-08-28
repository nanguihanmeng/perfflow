<script setup lang="ts">
/**
 * 部门考核初审（OPERATION）：对待初审的部门考核审核/退回整改
 */
import { onMounted, ref } from 'vue'
import { auditDeptAssessmentApi, listDeptAssessmentApi } from '@/api/dept.api'
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

const handleAudit = async (row: DeptAssessmentResp, approve: boolean): Promise<void> => {
  const msg = approve ? `确认初审通过「${row.deptName}」的部门考核？` : `确认退回「${row.deptName}」的部门考核整改？`
  await confirmAction(msg)
  await auditDeptAssessmentApi(row.deptId, approve, approve ? '初审通过' : '退回整改')
  toastSuccess(approve ? '初审通过' : '已退回')
  await load()
}
</script>

<template>
  <div class="dept-audit page-container">
    <PageHeader title="部门考核初审" description="运营管理部对部门考核逐项审核" />

    <div class="card">
      <el-table v-loading="loading" :data="list.filter((d) => d.status === 3)" border stripe>
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
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="handleAudit(row as DeptAssessmentResp, true)">通过</el-button>
            <el-button link type="danger" size="small" @click="handleAudit(row as DeptAssessmentResp, false)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.filter((d) => d.status === 3).length === 0" description="暂无待初审的部门考核" />
    </div>
  </div>
</template>
