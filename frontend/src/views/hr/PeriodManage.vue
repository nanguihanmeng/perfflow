<script setup lang="ts">
/**
 * 周期管理（HR）：周期列表 + 新建 / 开启 / 关闭
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import type { TableInstance } from 'element-plus'
import { createPeriodApi, getPeriodListApi, openPeriodApi, closePeriodApi } from '@/api/period.api'
import { getUserOptionsApi } from '@/api/system.api'
import type { PeriodCreateReq, PeriodResp, UserResp } from '@/types/dto'
import { PeriodStatus } from '@/types/enums'
import { confirmAction, toastSuccess } from '@/utils/message'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const list = ref<PeriodResp[]>([])

const loadList = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getPeriodListApi()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadList()
})

/** 状态文案映射 */
const statusLabel = (status: number): string => {
  if (status === PeriodStatus.ACTIVE) return '进行中'
  if (status === PeriodStatus.FINISHED) return '已结束'
  return '未开始'
}

const statusType = (status: number): 'info' | 'success' | 'warning' => {
  if (status === PeriodStatus.ACTIVE) return 'success'
  if (status === PeriodStatus.FINISHED) return 'info'
  return 'warning'
}

/** 新建周期对话框 */
const createVisible = ref(false)
const createSaving = ref(false)
const createForm = reactive<PeriodCreateReq>({
  name: '',
  year: new Date().getFullYear(),
  quarter: 1,
  startDate: '',
  suspendEndDate: '',
  deptReviewEndDate: '',
  leadScoreEndDate: '',
  autoPushOnExpire: false
})

const openCreate = (): void => {
  createForm.name = ''
  createForm.year = new Date().getFullYear()
  createForm.quarter = 1
  createForm.startDate = ''
  createForm.suspendEndDate = ''
  createForm.deptReviewEndDate = ''
  createForm.leadScoreEndDate = ''
  createForm.autoPushOnExpire = false
  createVisible.value = true
}

const submitCreate = async (): Promise<void> => {
  if (!createForm.name.trim()) {
    return
  }
  createSaving.value = true
  try {
    await createPeriodApi({ ...createForm })
    createVisible.value = false
    toastSuccess('周期创建成功')
    await loadList()
  } finally {
    createSaving.value = false
  }
}

/** 开启周期：员工勾选对话框 */
const openVisible = ref(false)
const openSaving = ref(false)
const openTarget = ref<PeriodResp | null>(null)
const empOptions = ref<UserResp[]>([])
const empLoading = ref(false)
const selectedIds = ref<number[]>([])
const empTableRef = ref<TableInstance>()

const roleLabel = (role: string): string => {
  const map: Record<string, string> = {
    EMP: '员工',
    DEPT_LEAD: '部门负责人',
    LEAD: '总监',
    HR: '人事'
  }
  return map[role] ?? role
}

const handleOpen = async (row: PeriodResp): Promise<void> => {
  openTarget.value = row
  selectedIds.value = []
  empOptions.value = []
  openVisible.value = true
  empLoading.value = true
  try {
    const res = await getUserOptionsApi()
    empOptions.value = res.data
    // 默认全选（等表格渲染完成后）
    await nextTick()
    empTableRef.value?.toggleAllSelection()
  } finally {
    empLoading.value = false
  }
}

const submitOpen = async (): Promise<void> => {
  if (!openTarget.value) {
    return
  }
  if (selectedIds.value.length === 0) {
    toastSuccess('请至少选择一名员工')
    return
  }
  openSaving.value = true
  try {
    await openPeriodApi(openTarget.value.id, selectedIds.value)
    openVisible.value = false
    toastSuccess('周期已开启')
    await loadList()
  } finally {
    openSaving.value = false
  }
}

const handleClose = async (row: PeriodResp): Promise<void> => {
  await confirmAction(`确认关闭周期「${row.name}」？`)
  await closePeriodApi(row.id)
  toastSuccess('周期已关闭')
  await loadList()
}
</script>

<template>
  <div class="period-manage page-container">
    <PageHeader title="周期管理" description="创建与管理考核周期">
      <template #actions>
        <el-button type="primary" @click="openCreate">新建周期</el-button>
      </template>
    </PageHeader>

    <div class="card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="name" label="周期名称" min-width="160" />
        <el-table-column label="年份" width="80" align="center">
          <template #default="{ row }">{{ row.year }}</template>
        </el-table-column>
        <el-table-column label="季度" width="80" align="center">
          <template #default="{ row }">Q{{ row.quarter }}</template>
        </el-table-column>
        <el-table-column label="开始日期" width="120" align="center">
          <template #default="{ row }">{{ formatDate(row.startDate) }}</template>
        </el-table-column>
        <el-table-column label="自评截止" width="120" align="center">
          <template #default="{ row }">{{ formatDate(row.suspendEndDate) }}</template>
        </el-table-column>
        <el-table-column label="部门审核截止" width="130" align="center">
          <template #default="{ row }">{{ formatDate(row.deptReviewEndDate) }}</template>
        </el-table-column>
        <el-table-column label="领导评分截止" width="130" align="center">
          <template #default="{ row }">{{ formatDate(row.leadScoreEndDate) }}</template>
        </el-table-column>
        <el-table-column label="自动推送" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.autoPushOnExpire ? 'success' : 'info'" effect="plain">
              {{ row.autoPushOnExpire ? '开启' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === PeriodStatus.NOT_STARTED">
              <el-button link type="success" size="small" @click="handleOpen(row as PeriodResp)">开启</el-button>
            </template>
            <template v-else-if="row.status === PeriodStatus.ACTIVE">
              <el-button link type="warning" size="small" @click="handleClose(row as PeriodResp)">关闭</el-button>
            </template>
            <span v-else class="text-disabled">—</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建周期对话框 -->
    <el-dialog v-model="createVisible" title="新建周期" width="520px" destroy-on-close>
      <el-form :model="createForm" label-width="110px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="如 2026 年第一季度" maxlength="64" />
        </el-form-item>
        <el-form-item label="年份" required>
          <el-input-number v-model="createForm.year" :min="2000" :max="2100" />
        </el-form-item>
        <el-form-item label="季度" required>
          <el-select v-model="createForm.quarter">
            <el-option :value="1" label="第一季度" />
            <el-option :value="2" label="第二季度" />
            <el-option :value="3" label="第三季度" />
            <el-option :value="4" label="第四季度" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="createForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="自评截止" required>
          <el-date-picker v-model="createForm.suspendEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门审核截止" required>
          <el-date-picker v-model="createForm.deptReviewEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="领导评分截止" required>
          <el-date-picker v-model="createForm.leadScoreEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期自动推送">
          <el-switch v-model="createForm.autoPushOnExpire" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSaving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 开启周期：员工勾选对话框 -->
    <el-dialog v-model="openVisible" title="选择参与考核的员工" width="640px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="仅勾选的员工需要填写该周期考核表；未勾选的不参与。"
        style="margin-bottom: 12px"
      />
      <el-table
        ref="empTableRef"
        v-loading="empLoading"
        :data="empOptions"
        border
        stripe
        max-height="360"
        @selection-change="(rows: UserResp[]) => { selectedIds = rows.map((r) => r.id) }"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="username" label="登录名" min-width="110" />
        <el-table-column label="部门" min-width="130">
          <template #default="{ row }">{{ row.deptName || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">{{ roleLabel(row.role) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="openVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="openSaving"
          :disabled="selectedIds.length === 0"
          @click="submitOpen"
        >
          确认开启（{{ selectedIds.length }}）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.text-disabled {
  color: $color-text-placeholder;
}
</style>
