<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const LOGIN_USERNAME = 'admin'
const REMEMBER_KEY = 'clinic-login-remember'
const isDev = import.meta.env.DEV

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const rememberPassword = ref(false)
const form = reactive({ password: '' })

function loadInitialPassword() {
  const remembered = localStorage.getItem(REMEMBER_KEY)
  if (remembered) {
    form.password = remembered
    rememberPassword.value = true
    return
  }
  if (!isDev) return
  const devDefault = import.meta.env.VITE_DEV_LOGIN_PASSWORD?.trim()
  if (devDefault) {
    form.password = devDefault
  }
}

function persistRememberedPassword() {
  if (rememberPassword.value && form.password) {
    localStorage.setItem(REMEMBER_KEY, form.password)
  } else {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

async function onSubmit() {
  loading.value = true
  try {
    await auth.login(form.password)
    persistRememberedPassword()
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}

onMounted(loadInitialPassword)
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="hover">
      <template #header>
        <span class="title">登录</span>
      </template>
      <p class="hint">发凤村卫生室诊所系统</p>
      <el-form
        label-width="80px"
        autocomplete="on"
        @submit.prevent="onSubmit"
      >
        <!-- 供浏览器密码管理器识别账号（后端固定 admin） -->
        <input
          class="sr-only"
          type="text"
          name="username"
          :value="LOGIN_USERNAME"
          autocomplete="username"
          tabindex="-1"
          aria-hidden="true"
        />
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            name="password"
            type="password"
            show-password
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="rememberPassword">记住密码（本机）</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">登录</el-button>
        </el-form-item>
      </el-form>
      <p v-if="isDev" class="dev-tip">开发环境已预填测试密码，也可使用浏览器保存的密码。</p>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.card {
  width: min(420px, 100%);
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
}

.hint {
  margin: 0 0 16px;
  color: #909399;
}

.dev-tip {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
