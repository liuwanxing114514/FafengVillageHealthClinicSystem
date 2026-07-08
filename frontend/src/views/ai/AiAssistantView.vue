<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchAiDrafts,
  getAiStatus,
  structureVisitDraft,
} from '@/api/ai'
import type { AiDraft, AiStatus } from '@/types/ai'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const structuring = ref(false)
const status = ref<AiStatus | null>(null)
const drafts = ref<AiDraft[]>([])
const inputText = ref('')

const patientId = computed(() => {
  const value = Number(route.query.patientId)
  return Number.isFinite(value) && value > 0 ? value : undefined
})

const visitId = computed(() => {
  const value = Number(route.query.visitId)
  return Number.isFinite(value) && value > 0 ? value : undefined
})

const aiReady = computed(() => status.value?.enabled && status.value?.providerAvailable)

async function loadPage() {
  loading.value = true
  try {
    status.value = await getAiStatus()
    drafts.value = await fetchAiDrafts('VISIT', 'PENDING')
  } finally {
    loading.value = false
  }
}

async function onStructure() {
  const text = inputText.value.trim()
  if (!text) {
    ElMessage.warning('请输入待整理文本')
    return
  }
  if (!aiReady.value) {
    ElMessage.warning('DeepSeek 未启用或未配置，请在 .env 中设置 CLINIC_AI_ENABLED 与 DEEPSEEK_API_KEY')
    return
  }
  structuring.value = true
  try {
    const draft = await structureVisitDraft(text, patientId.value, visitId.value)
    ElMessage.success('已生成病历草稿，请核对后确认')
    inputText.value = ''
    await loadPage()
    router.push(`/ai/drafts/${draft.id}`)
  } finally {
    structuring.value = false
  }
}

onMounted(loadPage)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">AI 病历整理</span>
          <el-tag v-if="status" :type="aiReady ? 'success' : 'info'">
            {{ aiReady ? `DeepSeek 可用 · ${status.provider}` : 'AI 未启用' }}
          </el-tag>
        </div>
      </template>

      <p class="hint">
        将自由文本或语音转写内容整理为主诉、诊断等字段草稿。确认前不会写入正式病历。
      </p>
      <p v-if="patientId" class="context-line">关联患者 ID：{{ patientId }}</p>
      <p v-if="visitId" class="context-line">关联病历 ID：{{ visitId }}</p>

      <el-input
        v-model="inputText"
        type="textarea"
        :rows="8"
        placeholder="粘贴口述记录、语音转写文本或随手记下的病情描述…"
      />
      <div class="actions">
        <el-button type="primary" :loading="structuring" :disabled="!aiReady" @click="onStructure">
          整理为病历草稿
        </el-button>
      </div>

      <el-divider />

      <h3 class="section">待确认草稿</h3>
      <el-table v-if="drafts.length" :data="drafts" stripe>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="source" label="来源" width="96" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/ai/drafts/${row.id}`)">
              核对
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无待确认草稿" />
    </el-card>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 1.1rem;
  font-weight: 600;
}

.hint,
.context-line {
  margin: 0 0 12px;
  color: #606266;
  font-size: 14px;
}

.actions {
  margin-top: 12px;
}

.section {
  margin: 0 0 12px;
  font-size: 1rem;
}
</style>
