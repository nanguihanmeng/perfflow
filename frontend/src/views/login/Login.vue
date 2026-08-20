<script setup lang="ts">
/**
 * 登录页
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { loginApi } from '@/api/auth.api'
import { useAuthStore } from '@/store/auth'
import { toastSuccess } from '@/utils/message'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async (): Promise<void> => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    const res = await loginApi({ username: form.username, password: form.password })
    authStore.setLogin(res.data)
    toastSuccess('登录成功')
    // 强制改密
    if (res.data.mustChangePassword) {
      router.push('/change-password')
      return
    }
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-card__title">PerfFlow</h1>
      <p class="login-card__subtitle">绩效考核管理系统</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="登录名" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button
            class="login-card__submit"
            type="primary"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #f7fafc 0%, #ebf4ff 100%);
}

.login-card {
  width: 360px;
  padding: $space-24;
  background-color: $color-bg-card;
  border: 1px solid $color-border;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;

  &__title {
    text-align: center;
    font-size: 26px;
    font-weight: 600;
    color: $color-primary;
  }

  &__subtitle {
    margin: $space-8 0 $space-24;
    text-align: center;
    font-size: $font-size-base;
    color: $color-text-secondary;
  }

  &__submit {
    width: 100%;
  }
}
</style>
