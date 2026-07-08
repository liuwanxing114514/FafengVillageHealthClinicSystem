<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAiStatus } from '@/api/ai'
import type { AiStatus } from '@/types/ai'

const router = useRouter()
const loading = ref(false)
const status = ref<AiStatus | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    status.value = await getAiStatus()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span>AI 助手</span>
          <el-button @click="router.push('/')">返回首页</el-button>
        </div>
      </template>

      <el-result icon="info" title="功能开发中">
        <template #sub-title>
          <p>AI 助手将在后续版本接入，当前不影响日常业务使用。</p>
          <p v-if="status" class="status-line">
            当前状态：{{ status.enabled ? '已启用' : '未启用' }} · Provider：{{ status.provider }}
          </p>
        </template>
        <template #extra>
          <el-button type="primary" @click="router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
.status-line { margin-top: 8px; color: #909399; font-size: 13px; }
</style>
