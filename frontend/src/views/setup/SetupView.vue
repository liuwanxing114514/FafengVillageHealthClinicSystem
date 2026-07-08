<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { setupPassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  password: '',
  confirmPassword: '',
})

async function onSubmit() {
  loading.value = true
  try {
    await setupPassword(form.password, form.confirmPassword)
    ElMessage.success('密码设置成功，请登录')
    auth.needSetup = false
    await router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="hover">
      <template #header>
        <span class="title">首次设置密码</span>
      </template>
      <p class="hint">系统首次使用，请为管理员设置登录密码（至少 8 位，含字母和数字）。</p>
      <el-form label-width="100px" @submit.prevent="onSubmit">
        <el-form-item label="新密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">保存并继续</el-button>
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
  width: min(480px, 100%);
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
}

.hint {
  margin: 0 0 16px;
  color: #606266;
  font-size: 0.95rem;
}
</style>
