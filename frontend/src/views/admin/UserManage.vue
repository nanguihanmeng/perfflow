<script setup lang="ts">
/**
 * 用户管理（ADMIN）：分页查询 + 新建 / 编辑 / 删除 / 重置密码 / 启禁用
 */
import { onMounted, reactive, ref } from 'vue'
import { useTable } from '@/composables/useTable'
import {
  createUserApi,
  deleteUserApi,
  getDeptListApi,
  getUserPageApi,
  resetUserPasswordApi,
  toggleUserStatusApi,
  updateUserApi,
  type UserPageQuery
} from '@/api/system.api'
import type { DeptResp, UserCreateReq, UserResp, UserUpdateReq } from '@/types/dto'
import { Role, UserStatus } from '@/types/enums'
import { RoleLabel } from '@/types/role'
import { confirmAction, toastSuccess } from '@/utils/message'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'

const { loading, list, pagination, query, search } = useTable<UserResp, UserPageQuery>({
  fetcher: (q) => getUserPageApi(q).then((res) => res.data),
  defaultQuery: () => ({ username: undefined, role: undefined, deptId: undefined, status: undefined })
})

const deptOptions = ref<DeptResp[]>([])
onMounted(async () => {
  const res = await getDeptListApi()
  deptOptions.value = res.data
})

/** 角色下拉：排除绩效考核管理员（admin 不可创建）与 ADMIN 本身 */
const roleOptions = Object.values(Role).filter(
  (r) => r !== Role.PERFORMANCE_HR && r !== Role.ADMIN
)

/** 用户状态文案 */
const statusLabel = (status: number): string => (status === UserStatus.ENABLED ? '启用' : '禁用')

/* ------------------------- 新建 / 编辑 ------------------------- */

const dialogVisible = ref(false)
const dialogSaving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<{
  username: string
  realName: string
  role: Role
  deptId: number | null
  email: string
  phone: string
  status: UserStatus
}>({
  username: '',
  realName: '',
  role: Role.EMP,
  deptId: null,
  email: '',
  phone: '',
  status: UserStatus.ENABLED
})

const openCreate = (): void => {
  editingId.value = null
  form.username = ''
  form.realName = ''
  form.role = Role.EMP
  form.deptId = null
  form.email = ''
  form.phone = ''
  form.status = UserStatus.ENABLED
  dialogVisible.value = true
}

const openEdit = (row: UserResp): void => {
  editingId.value = row.id
  form.username = row.username
  form.realName = row.realName
  form.role = row.role
  form.deptId = row.deptId
  form.email = row.email ?? ''
  form.phone = row.phone ?? ''
  form.status = row.status
  dialogVisible.value = true
}

const submitForm = async (): Promise<void> => {
  if (!form.username.trim() || !form.realName.trim()) {
    return
  }
  dialogSaving.value = true
  try {
    if (editingId.value === null) {
      const payload: UserCreateReq = {
        username: form.username.trim(),
        realName: form.realName.trim(),
        role: form.role,
        deptId: form.deptId,
        email: form.email || null,
        phone: form.phone || null
      }
      await createUserApi(payload)
      toastSuccess('用户创建成功')
    } else {
      const payload: UserUpdateReq = {
        realName: form.realName.trim(),
        role: form.role,
        deptId: form.deptId,
        email: form.email || null,
        phone: form.phone || null,
        status: form.status
      }
      await updateUserApi(editingId.value, payload)
      toastSuccess('用户修改成功')
    }
    dialogVisible.value = false
    await search()
  } finally {
    dialogSaving.value = false
  }
}

/* ------------------------- 操作 ------------------------- */

const handleResetPassword = async (row: UserResp): Promise<void> => {
  const res = await resetUserPasswordApi(row.id)
  await confirmAction(`用户「${row.realName}」的临时密码为：${res.data.initialPassword}`, {
    title: '重置密码成功',
    type: 'success',
    confirmButtonText: '知道了',
    showCancelButton: false
  })
}

const handleToggleStatus = async (row: UserResp): Promise<void> => {
  const target = row.status === UserStatus.ENABLED ? '禁用' : '启用'
  await confirmAction(`确认${target}用户「${row.realName}」？`)
  await toggleUserStatusApi(row.id)
  toastSuccess(`已${target}`)
  await search()
}

const handleDelete = async (row: UserResp): Promise<void> => {
  await confirmAction(`确认删除用户「${row.realName}」？删除后不可恢复。`, { type: 'error' })
  await deleteUserApi(row.id)
  toastSuccess('删除成功')
  await search()
}

const handleSearch = (): void => {
  search()
}
</script>

<template>
  <div class="user-manage page-container">
    <PageHeader title="用户管理" description="管理登录用户、角色与部门归属">
      <template #actions>
        <el-button type="primary" @click="openCreate">新建用户</el-button>
      </template>
    </PageHeader>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.username"
          placeholder="登录名"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 150px" @change="search">
          <el-option v-for="r in roleOptions" :key="r" :label="RoleLabel[r]" :value="r" />
        </el-select>
        <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 160px" @change="search">
          <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="search">
          <el-option label="启用" :value="UserStatus.ENABLED" />
          <el-option label="禁用" :value="UserStatus.DISABLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="username" label="登录名" width="120" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">{{ RoleLabel[row.role as Role] }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" width="130">
          <template #default="{ row }">{{ row.deptName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="140">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="120">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === UserStatus.ENABLED ? 'success' : 'danger'" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="150" align="center">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row as UserResp)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row as UserResp)">重置密码</el-button>
            <el-button link :type="row.status === UserStatus.ENABLED ? 'warning' : 'success'" size="small" @click="handleToggleStatus(row as UserResp)">
              {{ row.status === UserStatus.ENABLED ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as UserResp)">删除</el-button>
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

    <!-- 新建 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId === null ? '新建用户' : '编辑用户'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="登录名" required>
          <el-input v-model="form.username" :disabled="editingId !== null" maxlength="64" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" maxlength="64" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r" :label="RoleLabel[r]" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" clearable placeholder="无部门" style="width: 100%">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="128" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" maxlength="32" />
        </el-form-item>
        <el-form-item v-if="editingId !== null" label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="UserStatus.ENABLED">启用</el-radio>
            <el-radio :value="UserStatus.DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSaving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
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
