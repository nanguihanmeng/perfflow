<script setup lang="ts">
/**
 * 个人中心：查看当前用户信息 + 修改个人资料（姓名/邮箱/电话）
 * - 绩效考核管理员账号信息变更的唯一渠道（admin 不可管理）
 */
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { RoleLabel } from '@/types/role'
import { EMPTY_PLACEHOLDER } from '@/utils/format'
import { updateProfileApi } from '@/api/auth.api'
import { isNotBlank } from '@/utils/validate'
import { toastError, toastSuccess } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const roleLabel = computed(() => (user.value?.role ? RoleLabel[user.value.role] : EMPTY_PLACEHOLDER))

const goChangePassword = (): void => {
  router.push('/change-password')
}

/** 个人中心信息项（只读展示） */
const items = computed(() => [
  { label: '登录名', value: user.value?.username ?? EMPTY_PLACEHOLDER },
  { label: '角色', value: roleLabel.value },
  { label: '部门', value: user.value?.deptName ?? EMPTY_PLACEHOLDER },
  { label: '部门负责人', value: user.value?.deptLead ? '是' : '否' },
  { label: '令牌有效期(秒)', value: user.value?.expiresIn != null ? String(user.value.expiresIn) : EMPTY_PLACEHOLDER }
])

/* ---------------- 修改资料 ---------------- */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({
  realName: '',
  email: '',
  phone: ''
})

const openEdit = (): void => {
  editForm.realName = user.value?.realName ?? ''
  editForm.email = ''
  editForm.phone = ''
  editVisible.value = true
}

const submitEdit = async (): Promise<void> => {
  if (!isNotBlank(editForm.realName)) {
    toastError('真实姓名不能为空')
    return
  }
  editSaving.value = true
  try {
    await updateProfileApi({
      realName: editForm.realName.trim(),
      email: editForm.email || null,
      phone: editForm.phone || null
    })
    authStore.applyProfile(editForm.realName.trim())
    editVisible.value = false
    toastSuccess('资料已更新')
  } finally {
    editSaving.value = false
  }
}
</script>

<template>
  <div class="profile-page page-container">
    <div class="card profile-card">
      <div class="profile-card__header">
        <el-avatar :size="56" class="profile-card__avatar">
          {{ user?.realName?.charAt(0) ?? 'U' }}
        </el-avatar>
        <div class="profile-card__identity">
          <div class="profile-card__name">{{ user?.realName || user?.username }}</div>
          <el-tag size="small" type="info" effect="plain">{{ roleLabel }}</el-tag>
        </div>
        <el-button type="primary" plain @click="goChangePassword">修改密码</el-button>
        <el-button type="primary" @click="openEdit">修改资料</el-button>
      </div>

      <el-descriptions :column="2" border class="profile-card__body">
        <el-descriptions-item v-for="item in items" :key="item.label" :label="item.label">
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 修改资料对话框 -->
    <el-dialog v-model="editVisible" title="修改个人资料" width="460px">
      <el-form label-width="90px">
        <el-form-item label="真实姓名" required>
          <el-input v-model="editForm.realName" maxlength="32" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" maxlength="64" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="editForm.phone" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.profile-page {
  max-width: 760px;
}

.profile-card {
  &__header {
    display: flex;
    align-items: center;
    gap: $space-16;
    margin-bottom: $space-20;
  }

  &__avatar {
    background-color: $color-primary;
    color: #fff;
    font-size: $font-size-2xl;
  }

  &__identity {
    flex: 1;
    display: flex;
    align-items: center;
    gap: $space-8;
  }

  &__name {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $color-text-main;
  }

  &__body {
    margin-top: $space-16;
  }
}
</style>
