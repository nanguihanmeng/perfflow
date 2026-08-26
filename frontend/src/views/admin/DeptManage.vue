<script setup lang="ts">
/**
 * 部门管理（ADMIN）：部门列表 + 新建 / 编辑 / 删除
 */
import { onMounted, reactive, ref } from 'vue'
import { createDeptApi, deleteDeptApi, getDeptListApi, updateDeptApi } from '@/api/system.api'
import type { DeptReq, DeptResp } from '@/types/dto'
import { confirmAction, toastSuccess } from '@/utils/message'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const list = ref<DeptResp[]>([])

const loadList = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getDeptListApi()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadList()
})

/* ------------------------- 新建 / 编辑 ------------------------- */

const dialogVisible = ref(false)
const dialogSaving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<DeptReq>({
  name: '',
  parentId: null,
  remark: ''
})

const openCreate = (): void => {
  editingId.value = null
  form.name = ''
  form.parentId = null
  form.remark = ''
  dialogVisible.value = true
}

const openEdit = (row: DeptResp): void => {
  editingId.value = row.id
  form.name = row.name
  form.parentId = row.parentId
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

const submitForm = async (): Promise<void> => {
  if (!form.name.trim()) {
    return
  }
  dialogSaving.value = true
  try {
    if (editingId.value === null) {
      await createDeptApi({ ...form })
      toastSuccess('部门创建成功')
    } else {
      await updateDeptApi(editingId.value, { ...form })
      toastSuccess('部门修改成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    dialogSaving.value = false
  }
}

/* ------------------------- 删除 ------------------------- */

const handleDelete = async (row: DeptResp): Promise<void> => {
  await confirmAction(`确认删除部门「${row.name}」？`, { type: 'error' })
  await deleteDeptApi(row.id)
  toastSuccess('删除成功')
  await loadList()
}

/** 父部门选项（排除自身） */
const parentOptions = (): DeptResp[] => list.value.filter((d) => d.id !== editingId.value)
</script>

<template>
  <div class="dept-manage page-container">
    <PageHeader title="部门管理" description="维护组织架构与部门信息">
      <template #actions>
        <el-button type="primary" @click="openCreate">新建部门</el-button>
      </template>
    </PageHeader>

    <div class="card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="部门名称" min-width="160" />
        <el-table-column label="父部门" width="140">
          <template #default="{ row }">
            {{ list.find((d) => d.id === row.parentId)?.name ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160">
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row as DeptResp)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as DeptResp)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId === null ? '新建部门' : '编辑部门'" width="480px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="部门名称" required>
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="父部门">
          <el-select v-model="form.parentId" clearable placeholder="无" style="width: 100%">
            <el-option v-for="d in parentOptions()" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSaving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
