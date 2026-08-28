<script setup lang="ts">
/**
 * 进度看板（OPERATION/COMMITTEE）：汇总卡片 + ECharts 柱状图 + 部门明细
 */
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboardApi } from '@/api/monitor.api'
import type { ProgressResp } from '@/types/dto'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const dashboard = ref<ProgressResp | null>(null)
const chartRef = ref<HTMLDivElement>()

const load = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getDashboardApi()
    dashboard.value = res.data
    renderChart()
  } finally {
    loading.value = false
  }
}

const renderChart = (): void => {
  if (!dashboard.value || !chartRef.value) {
    return
  }
  const chart = echarts.init(chartRef.value)
  const depts = dashboard.value.deptDetails
  chart.setOption({
    tooltip: {},
    legend: { data: ['总人数', '已填报', '已审'] },
    xAxis: { type: 'category', data: depts.map((d) => d.deptName) },
    yAxis: { type: 'value' },
    series: [
      { name: '总人数', type: 'bar', data: depts.map((d) => d.total) },
      { name: '已填报', type: 'bar', data: depts.map((d) => d.filled) },
      { name: '已审', type: 'bar', data: depts.map((d) => d.reviewed) }
    ]
  })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="dashboard page-container">
    <PageHeader title="考核进度看板" description="全公司考核填报/审核进度实时统计" />

    <div v-loading="loading" class="card">
      <div class="stat-cards">
        <div class="stat-card"><div class="num">{{ dashboard?.total ?? 0 }}</div><div class="label">总考核人数</div></div>
        <div class="stat-card"><div class="num">{{ dashboard?.filled ?? 0 }}</div><div class="label">已填报</div></div>
        <div class="stat-card"><div class="num">{{ dashboard?.unfilled ?? 0 }}</div><div class="label">未填报</div></div>
        <div class="stat-card"><div class="num">{{ dashboard?.reviewed ?? 0 }}</div><div class="label">已审</div></div>
        <div class="stat-card danger"><div class="num">{{ dashboard?.overdue ?? 0 }}</div><div class="label">逾期</div></div>
      </div>

      <div ref="chartRef" style="height: 320px; margin-top: 16px" />

      <el-table :data="dashboard?.deptDetails ?? []" border stripe style="margin-top: 16px">
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column prop="total" label="总人数" width="80" align="center" />
        <el-table-column prop="filled" label="已填" width="80" align="center" />
        <el-table-column prop="unfilled" label="未填" width="80" align="center" />
        <el-table-column prop="reviewed" label="已审" width="80" align="center" />
        <el-table-column prop="overdue" label="逾期" width="80" align="center" />
        <el-table-column label="完成率" width="140" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.completionRate" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.stat-cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.stat-card {
  flex: 1;
  min-width: 120px;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  text-align: center;
  .num { font-size: 28px; font-weight: 700; }
  .label { color: #888; margin-top: 4px; }
  &.danger .num { color: #f56c6c; }
}
</style>
