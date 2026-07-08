<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ password: '' })

async function onSubmit() {
  loading.value = true
  try {
    await auth.login(form.password)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="hover">
      <template #header>
        <span class="title">登录</span>
      </template>
      <p class="hint">发凤村卫生室诊所系统</p>
      <el-form label-width="80px" @submit.prevent="onSubmit">
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">登录</el-button>
        </el-form-item>
      </el-form>
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
</style>
