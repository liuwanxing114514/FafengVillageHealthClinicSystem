<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const backendStatus = ref<string>('检查中…')

onMounted(async () => {
  try {
    const { data } = await axios.get<{ status: string }>('/api/health')
    backendStatus.value = data.status === 'UP' ? '正常' : data.status
  } catch {
    backendStatus.value = '未连接（请确认后端已启动）'
  }
})
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="hover">
      <template #header>
        <div class="header-row">
          <span class="title">发凤村卫生室诊所系统</span>
          <el-button link type="primary" @click="router.push('/settings')">系统设置</el-button>
        </div>
      </template>
      <p class="version">v0.2 登录与基础模块</p>
      <p class="hint">当前用户：{{ auth.operator ?? '—' }}</p>
      <p class="hint">后端健康检查：{{ backendStatus }}</p>
      <p class="note">药品、患者等业务功能将在后续版本逐步开放。</p>
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
  width: min(520px, 100%);
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
}

.version {
  margin: 0 0 12px;
  color: #409eff;
  font-weight: 500;
}

.hint {
  margin: 0 0 8px;
  color: #606266;
}

.note {
  margin: 0;
  color: #909399;
  font-size: 0.9rem;
}
</style>
