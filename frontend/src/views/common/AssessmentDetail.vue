<script setup lang="ts">
/**
 * 考核主表详情（多角色复用）
 * - 员工(EMP)：SELF_DRAFTING 内联编辑行 + 提交
 * - 部门领导(DEPT_LEAD)：DEPT_REVIEW 通过 / 打回 / 行调分
 * - 领导(LEAD)：LEAD_SCORING 评分
 * - 人事(HR)：SELF_SUSPENDED 推送 / 延长挂起 / 导出；HR/LEAD/DEPT_LEAD 查看流程日志
 */
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTableDetailApi, submitTableApi, pushTableApi, approveTableApi, rejectTableApi, leadScoreTableApi, extendSuspendApi, getFlowLogsApi, importRowsApi } from '@/api/assessment.api'
import type { AssessmentTableResp, FlowLogResp, LeadScoreReq } from '@/types/dto'
import { AssessmentState, Role } from '@/types/enums'
import { useAuthStore } from '@/store/auth'
import { confirmAction, toastError, toastSuccess } from '@/utils/message'
import { formatDateTime } from '@/utils/format'
import { isNotBlank, isScoreInRange } from '@/utils/validate'
import AssessmentRows from '@/views/common/AssessmentRows.vue'
import StateTag from '@/components/common/StateTag.vue'
import ScoreDisplay from '@/components/common/ScoreDisplay.vue'
import GradeBadge from '@/components/common/GradeBadge.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const tableId = Number(route.params.id)
const loading = ref(false)
const table = ref<AssessmentTableResp | null>(null)
const logs = ref<FlowLogResp[]>([])

const role = computed(() => authStore.role)
const state = computed(() => table.value?.state ?? null)

/** 各角色可执行动作（与状态机严格对齐） */
const canEditRows = computed(
  () =>
    (role.value === Role.EMP && state.value === AssessmentState.SELF_DRAFTING) ||
    (role.value === Role.DEPT_LEAD && state.value === AssessmentState.DEPT_REVIEW)
)
const canSubmit = computed(() => role.value === Role.EMP && state.value === AssessmentState.SELF_DRAFTING)
const canApprove = computed(() => role.value === Role.DEPT_LEAD && state.value === AssessmentState.DEPT_REVIEW)
const canLeadScore = computed(() => role.value === Role.LEAD && state.value === AssessmentState.LEAD_SCORING)
const canPush = computed(() => role.value === Role.HR && state.value === AssessmentState.SELF_SUSPENDED)
const canExtend = computed(() => role.value === Role.HR && state.value === AssessmentState.SELF_SUSPENDED)
const canImport = computed(() => role.value === Role.HR && state.value === AssessmentState.SELF_DRAFTING)
const canViewLogs = computed(
  () => role.value === Role.HR || role.value === Role.LEAD || role.value === Role.DEPT_LEAD
)

const loadTable = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getTableDetailApi(tableId)
    table.value = res.data
    if (canViewLogs.value) {
      const logRes = await getFlowLogsApi(tableId)
      logs.value = logRes.data
    }
  } finally {
    loading.value = false
  }
}

void loadTable()

/* ------------------------- 操作 ------------------------- */

const handleSubmit = async (): Promise<void> => {
  await confirmAction('确认提交考核表？提交后不可修改。', {
    title: '提交确认'
  })
  await submitTableApi(tableId)
  toastSuccess('提交成功')
  await loadTable()
}

const handlePush = async (): Promise<void> => {
  await confirmAction('确认推送该考核表进入部门审核？')
  await pushTableApi(tableId)
  toastSuccess('推送成功')
  await loadTable()
}

/* 打回对话框 */
const rejectVisible = ref(false)
const rejectForm = reactive({ comment: '' })
const rejectSaving = ref(false)

const openReject = (): void => {
  rejectForm.comment = ''
  rejectVisible.value = true
}

const submitReject = async (): Promise<void> => {
  if (!isNotBlank(rejectForm.comment)) {
    toastError('打回必须填写原因')
    return
  }
  rejectSaving.value = true
  try {
    await rejectTableApi(tableId, rejectForm.comment)
    rejectVisible.value = false
    toastSuccess('已打回')
    await loadTable()
  } finally {
    rejectSaving.value = false
  }
}

const handleApprove = async (): Promise<void> => {
  await confirmAction('确认通过该考核表并进入领导评分？')
  await approveTableApi(tableId)
  toastSuccess('已通过')
  await loadTable()
}

/* HR 导入考核明细 */
const importVisible = ref(false)
const importFile = ref<File | null>(null)
const importSaving = ref(false)

const openImport = (): void => {
  importFile.value = null
  importVisible.value = true
}

const onImportFileChange = (file: File): void => {
  importFile.value = file
}

const submitImport = async (): Promise<void> => {
  if (!importFile.value) {
    toastError('请选择要导入的 xlsx 文件')
    return
  }
  importSaving.value = true
  try {
    await importRowsApi(tableId, importFile.value)
    importVisible.value = false
    toastSuccess('导入成功')
    await loadTable()
  } finally {
    importSaving.value = false
  }
}

/* 领导评分对话框 */
const leadVisible = ref(false)
const leadForm = reactive<LeadScoreReq>({ leaderScore: '', comment: '' })
const leadSaving = ref(false)

const openLeadScore = (): void => {
  leadForm.leaderScore = ''
  leadForm.comment = ''
  leadVisible.value = true
}

const submitLeadScore = async (): Promise<void> => {
  if (!isScoreInRange(leadForm.leaderScore, 0, 100)) {
    toastError('领导评分必须在 0-100 之间')
    return
  }
  leadSaving.value = true
  try {
    await leadScoreTableApi(tableId, {
      leaderScore: leadForm.leaderScore,
      comment: leadForm.comment || undefined
    })
    leadVisible.value = false
    toastSuccess('评分成功')
    await loadTable()
  } finally {
    leadSaving.value = false
  }
}

/* 延长挂起对话框 */
const extendVisible = ref(false)
const extendForm = reactive({ days: 1, reason: '' })
const extendSaving = ref(false)

const openExtend = (): void => {
  extendForm.days = 1
  extendForm.reason = ''
  extendVisible.value = true
}

const submitExtend = async (): Promise<void> => {
  if (extendForm.days < 1 || extendForm.days > 30) {
    toastError('延长天数必须在 1-30 之间')
    return
  }
  extendSaving.value = true
  try {
    await extendSuspendApi(tableId, {
      days: extendForm.days,
      reason: extendForm.reason || undefined
    })
    extendVisible.value = false
    toastSuccess('延长成功')
    await loadTable()
  } finally {
    extendSaving.value = false
  }
}

const goBack = (): void => {
  router.back()
}
</script>

<template>
  <div v-loading="loading" class="detail-page page-container">
    <div v-if="table" class="detail-page__body">
      <!-- 信息卡 -->
      <div class="card detail-info">
        <div class="detail-info__header">
          <h2 class="detail-info__title">{{ table.periodName }}</h2>
          <StateTag :state="table.state" />
        </div>
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="员工">{{ table.realName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ table.deptName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="自评总分">
            <ScoreDisplay :value="table.selfTotalScore" />
          </el-descriptions-item>
          <el-descriptions-item label="领导评分">
            <ScoreDisplay :value="table.leaderScore" />
          </el-descriptions-item>
          <el-descriptions-item label="最终得分">
            <ScoreDisplay :value="table.finalScore" />
          </el-descriptions-item>
          <el-descriptions-item label="考核结果">
            <GradeBadge :grade="table.grade" />
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(table.submittedAt) }}</el-descriptions-item>
          <el-descriptions-item label="挂起延长">
            {{ table.suspendExtendedDays ? `${table.suspendExtendedDays} 天` : '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 操作区 -->
        <div class="detail-info__actions">
          <el-button v-if="canSubmit" type="primary" @click="handleSubmit">提交</el-button>
          <el-button v-if="canPush" type="primary" @click="handlePush">推送到部门审核</el-button>
          <el-button v-if="canApprove" type="success" @click="handleApprove">通过</el-button>
          <el-button v-if="canApprove" type="danger" plain @click="openReject">打回</el-button>
          <el-button v-if="canLeadScore" type="primary" @click="openLeadScore">领导评分</el-button>
          <el-button v-if="canExtend" plain @click="openExtend">延长挂起</el-button>
          <el-button v-if="canImport" plain @click="openImport">导入考核明细</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>

      <!-- 考核行 -->
      <div class="card detail-rows">
        <h3 class="detail-rows__title">考核明细（{{ table.rows?.length ?? 0 }} 行）</h3>
        <AssessmentRows
          :table-id="table.id"
          :rows="table.rows ?? []"
          :editable="canEditRows"
          @refresh="loadTable"
        />
      </div>

      <!-- 流程日志 -->
      <div v-if="canViewLogs" class="card detail-logs">
        <h3 class="detail-logs__title">流程记录</h3>
        <el-timeline v-if="logs.length > 0">
          <el-timeline-item
            v-for="log in logs"
            :key="log.id"
            :timestamp="formatDateTime(log.createdAt)"
            placement="top"
          >
            <div class="detail-logs__item">
              <span class="detail-logs__action">{{ log.action || '状态变更' }}</span>
              <span class="detail-logs__states">{{ log.fromState }} → {{ log.toState }}</span>
              <span class="detail-logs__operator">{{ log.operatorName }}（{{ log.operatorRole }}）</span>
            </div>
            <div v-if="log.comment" class="detail-logs__comment">{{ log.comment }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无流程记录" :image-size="60" />
      </div>
    </div>
    <el-empty v-else-if="!loading" description="考核表不存在或已删除" />

    <!-- 打回对话框 -->
    <el-dialog v-model="rejectVisible" title="打回考核表" width="460px">
      <el-form label-width="90px">
        <el-form-item label="打回原因" required>
          <el-input v-model="rejectForm.comment" type="textarea" :rows="3" maxlength="500" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSaving" @click="submitReject">确认打回</el-button>
      </template>
    </el-dialog>

    <!-- 领导评分对话框 -->
    <el-dialog v-model="leadVisible" title="领导评分" width="460px">
      <el-form label-width="90px">
        <el-form-item label="评分" required>
          <el-input v-model="leadForm.leaderScore" placeholder="0-100" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="leadForm.comment" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leadVisible = false">取消</el-button>
        <el-button type="primary" :loading="leadSaving" @click="submitLeadScore">提交评分</el-button>
      </template>
    </el-dialog>

    <!-- 延长挂起对话框 -->
    <el-dialog v-model="extendVisible" title="延长挂起" width="460px">
      <el-form label-width="90px">
        <el-form-item label="延长天数" required>
          <el-input-number v-model="extendForm.days" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="extendForm.reason" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extendVisible = false">取消</el-button>
        <el-button type="primary" :loading="extendSaving" @click="submitExtend">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入考核明细对话框 -->
    <el-dialog v-model="importVisible" title="导入考核明细" width="560px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="请上传与《岗位季度绩效考核表》A1:G16 一致格式的 xlsx 文件（第4行表头，第5-14行为序号1-10的指标明细）。"
      />
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="(file: any) => onImportFileChange(file.raw)"
        :on-remove="() => (importFile = null)"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls，导入后覆盖该考核表 10 行指标明细。</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importSaving" @click="submitImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.detail-page {
  max-width: 1280px;
}

.detail-info {
  margin-bottom: $space-16;

  &__header {
    display: flex;
    align-items: center;
    gap: $space-12;
    margin-bottom: $space-16;
  }

  &__title {
    font-size: $font-size-xl;
    font-weight: 600;
    color: $color-text-main;
  }

  &__actions {
    display: flex;
    gap: $space-8;
    margin-top: $space-16;
    justify-content: flex-end;
  }
}

.detail-rows {
  margin-bottom: $space-16;

  &__title {
    font-size: $font-size-lg;
    font-weight: 600;
    margin-bottom: $space-16;
  }
}

.detail-logs {
  &__title {
    font-size: $font-size-lg;
    font-weight: 600;
    margin-bottom: $space-16;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: $space-8;
    font-size: $font-size-base;
  }

  &__action {
    font-weight: 600;
    color: $color-text-main;
  }

  &__states {
    color: $color-text-secondary;
  }

  &__operator {
    color: $color-text-secondary;
    font-size: $font-size-sm;
  }

  &__comment {
    margin-top: $space-4;
    font-size: $font-size-sm;
    color: $color-warning;
  }
}
</style>
