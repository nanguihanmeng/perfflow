<script setup lang="ts">
/**
 * 修改密码页
 * 说明：不校验旧密码，仅确认两次新密码一致（首次登录强制改密场景）。
 * 后端暂无改密接口，提交走预留契约 PUT /auth/password（错误码 1105/1106）。
 * 后端未实现时调用失败，前端优雅提示，不阻塞其余功能。
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { changePasswordApi } from '@/api/auth.api'
import { useAuthStore } from '@/store/auth'
import { toastSuccess, toastWarning } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
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
    // 预留接口：后端未实现时此处会抛错，由 catch 统一提示
    await changePasswordApi({ newPassword: form.newPassword })
    toastSuccess('密码修改成功，请重新登录')
    await authStore.logout()
    router.push('/login')
  } catch {
    toastWarning('改密接口尚未就绪，请稍后重试或联系管理员')
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
