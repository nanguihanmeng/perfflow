<script setup lang="ts">
/**
 * 修改密码页（首次登录强制改密场景）
 * 说明：需填写原密码（即当前登录所用密码），并确认两次新密码一致。改密成功后后端返回新令牌，
 * 前端直接覆盖本地令牌并进入工作台，无需重新登录（原 JWT 中 mustChangePwd=true 已被拒绝）。
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { changePasswordApi } from '@/api/auth.api'
import { useAuthStore } from '@/store/auth'
import { toastSuccess, toastError } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (_rule: unknown, value: string, callback: (error?: Error) => void): void => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度为 8-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleSubmit = async (): Promise<void> => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    // 后端返回新令牌（mustChangePassword=false），直接覆盖本地登录态
    const res = await changePasswordApi({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    authStore.setLogin(res.data)
    toastSuccess('密码修改成功')
    router.push('/home')
  } catch (err) {
    // silent 请求不弹全局提示，这里展示后端具体原因（如原密码错误）
    toastError(err instanceof Error && err.message ? err.message : '密码修改失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleCancel = (): void => {
  router.back()
}
</script>

<template>
  <div class="change-password-page">
    <div class="change-password-card">
      <h2 class="change-password-card__title">修改密码</h2>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        size="large"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="8-32 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">确认修改</el-button>
          <el-button @click="handleCancel">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.change-password-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f7fafc 0%, #ebf4ff 100%);
}

.change-password-card {
  width: 420px;
  padding: $space-24;
  background-color: $color-bg-card;
  border: 1px solid $color-border;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;

  &__title {
    margin-bottom: $space-20;
    text-align: center;
    font-size: $font-size-xl;
    font-weight: 600;
    color: $color-text-main;
  }
}
</style>
