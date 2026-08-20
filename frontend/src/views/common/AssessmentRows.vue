<script setup lang="ts">
/**
 * 考核行表格组件（模板 A1:G16 样式）
 * - 信息栏：部门 / 被考核人 / 岗位(可填) / 部门负责人
 * - 10 行数据：序号 / 指标类别 / 指标名称 / 指标分数 / 工作目标 / 评分标准 / 完成率 / 自评得分
 * - 编辑模式（EMP 自评 / DEPT_LEAD 部门审核）：仅可填完成率（0-100），自评得分由后端自动算
 * - 行结果（考核结果）只与总分相关，行级不展示、不可写
 */
import { reactive, ref } from 'vue'
import type { RowResp } from '@/types/dto'
import { RowCategoryLabel } from '@/types/role'
import { updateRowApi } from '@/api/assessment.api'
import { isCompletionRateValid } from '@/utils/validate'
import { toastError, toastSuccess } from '@/utils/message'
import ScoreDisplay from '@/components/common/ScoreDisplay.vue'

interface Props {
  /** 主表 ID */
  tableId: number
  /** 考核行列表 */
  rows: RowResp[]
  /** 是否允许编辑完成率（EMP 自评 / DEPT_LEAD 部门审核） */
  editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  editable: false
})

const emit = defineEmits<{
  refresh: []
}>()

const categoryLabel = (cat: string): string => RowCategoryLabel[cat] ?? cat

/** 正在编辑的行 ID */
const editingRowId = ref<number | null>(null)

/** 保存中的行 ID */
const savingRowId = ref<number | null>(null)

/** 编辑态草稿（仅完成率） */
const draft = reactive({
  completionRate: ''
})

const startEdit = (row: RowResp): void => {
  editingRowId.value = row.id
  draft.completionRate = row.completionRate ?? ''
}

const cancelEdit = (): void => {
  editingRowId.value = null
}

const saveRow = async (row: RowResp): Promise<void> => {
  // 完成率 0-100 校验
  if (draft.completionRate !== '' && !isCompletionRateValid(draft.completionRate)) {
    toastError('完成率必须在 0-100 之间')
    return
  }
  savingRowId.value = row.id
  try {
    await updateRowApi(props.tableId, row.id, {
      completionRate: draft.completionRate || undefined
    })
    editingRowId.value = null
    toastSuccess('保存成功')
    emit('refresh')
  } finally {
    savingRowId.value = null
  }
}

/** 行合并：按分类垂直合并「指标类别」列（PLAN 1-5 / OPEN 6-7 / BONUS 8-10） */
const spanMethod = ({ row, columnIndex }: { row: RowResp; columnIndex: number }): { rowspan: number; colspan: number } | undefined => {
  if (columnIndex !== 1) return undefined
  const seq = row.seq
  let groupSize = 0
  let isGroupStart = false
  if (seq >= 1 && seq <= 5) {
    groupSize = props.rows.filter((r) => r.category === 'PLAN').length
    isGroupStart = seq === 1
  } else if (seq >= 6 && seq <= 7) {
    groupSize = props.rows.filter((r) => r.category === 'OPEN').length
    isGroupStart = seq === 6
  } else {
    groupSize = props.rows.filter((r) => r.category === 'BONUS').length
    isGroupStart = seq === 8
  }
  if (isGroupStart) {
    return { rowspan: groupSize, colspan: 1 }
  }
  return { rowspan: 0, colspan: 0 }
}
</script>

<template>
  <div class="assessment-rows">
    <div class="assessment-rows__title">岗位季度绩效考核表</div>
    <el-table :data="props.rows" border size="small" class="assessment-rows__table" :span-method="spanMethod">
      <el-table-column prop="seq" label="序号" width="55" align="center" />
      <el-table-column label="指标类别" width="110">
        <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
      </el-table-column>
      <el-table-column label="指标名称" min-width="140">
        <template #default="{ row }">{{ row.indicatorName || '—' }}</template>
      </el-table-column>
      <el-table-column label="指标分数" width="80" align="center">
        <template #default="{ row }">{{ row.baseScore ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="工作目标" min-width="160">
        <template #default="{ row }">{{ row.workTarget || '—' }}</template>
      </el-table-column>
      <el-table-column label="评分标准" min-width="160">
        <template #default="{ row }">{{ row.scoreCriteria || '—' }}</template>
      </el-table-column>
      <el-table-column label="完成率" width="100" align="center">
        <template #default="{ row }">
          <template v-if="editable && editingRowId === row.id">
            <el-input v-model="draft.completionRate" placeholder="0-100" size="small" />
          </template>
          <template v-else>
            {{ row.completionRate ? `${row.completionRate}%` : '—' }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="自评得分" width="90" align="center">
        <template #default="{ row }">
          <ScoreDisplay :value="row.selfScore" :masked="row.masked" />
        </template>
      </el-table-column>
      <el-table-column v-if="editable" label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="editingRowId !== row.id"
            link
            type="primary"
            size="small"
            @click="startEdit(row as RowResp)"
          >
            编辑
          </el-button>
          <template v-else>
            <el-button link type="success" size="small" :loading="savingRowId === row.id" @click="saveRow(row as RowResp)">
              保存
            </el-button>
            <el-button link size="small" @click="cancelEdit">取消</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="props.rows.length === 0" description="暂无考核行" />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.assessment-rows {
  &__title {
    font-size: $font-size-lg;
    font-weight: 600;
    text-align: center;
    margin-bottom: $space-12;
  }

  &__table {
    width: 100%;
  }
}
</style>
