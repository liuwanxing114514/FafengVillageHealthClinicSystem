<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'

const backendStatus = ref<string>('检查中…')

onMounted(async () => {
  try {
    const { data } = await axios.get<{ status: string }>('/api/health')
    backendStatus.value = data.status === 'UP' ? '正常' : data.status
  } catch {
    backendStatus.value = '未连接（开发模式请确认后端已启动）'
  }
})
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="hover">
      <template #header>
        <span class="title">发凤村卫生室诊所系统</span>
      </template>
      <p class="version">v0.1 项目骨架</p>
      <p class="hint">后端健康检查：{{ backendStatus }}</p>
      <p class="note">业务功能将在 v0.2 起逐步实装。</p>
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
