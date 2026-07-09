<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword } from '@/api/auth'
import {
  getEmbeddingStatus,
  syncEmbeddingsFull,
  syncEmbeddingsIncremental,
} from '@/api/ai'
import { fetchSettings, updateSetting } from '@/api/settings'
import type { SettingItem } from '@/types/api'
import type { VisitEmbeddingStatus } from '@/types/ai'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const settings = ref<SettingItem[]>([])
const pwdForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const clinicName = ref('')
const printTemplate = ref('default-a4')

const embeddingStatus = ref<VisitEmbeddingStatus | null>(null)
const embeddingStatusLoading = ref(false)
const embeddingSyncLoading = ref(false)

const canSyncEmbedding = computed(
  () => Boolean(embeddingStatus.value?.enabled && embeddingStatus.value?.configured),
)

const embeddingServiceLabel = computed(() => {
  if (!embeddingStatus.value) return '加载中…'
  if (embeddingStatus.value.enabled && embeddingStatus.value.configured) return '已启用'
  if (!embeddingStatus.value.enabled) return '未启用'
  return '配置不完整'
})

const embeddingAlertMessage = computed(() => {
  if (!embeddingStatus.value) return ''
  if (!embeddingStatus.value.enabled) {
    return '病历向量化未启用。请在 .env 中设置 CLINIC_EMBEDDING_ENABLED=true 并配置 API Key。关闭时不影响日常业务。'
  }
  if (!embeddingStatus.value.configured) {
    return 'Embedding 配置不完整，请检查 CLINIC_EMBEDDING_API_KEY 与 CLINIC_EMBEDDING_BASE_URL。'
  }
  return ''
})

function formatSyncedAt(value: string | null | undefined) {
  if (!value) return '尚未同步'
  return value.replace('T', ' ').slice(0, 19)
}

function formatSyncResult(modeLabel: string, result: {
  synced: number
  skipped: number
  failed: number
  durationMs: number
}) {
  const seconds = (result.durationMs / 1000).toFixed(1)
  return `${modeLabel}完成：成功 ${result.synced} 条，跳过 ${result.skipped} 条，失败 ${result.failed} 条，耗时 ${seconds} 秒`
}

async function loadSettings() {
  settings.value = await fetchSettings()
  clinicName.value = settings.value.find((s) => s.key === 'clinic_name')?.value ?? ''
  printTemplate.value =
    settings.value.find((s) => s.key === 'prescription_print_active_template')?.value ?? 'default-a4'
}

async function loadEmbeddingStatus() {
  embeddingStatusLoading.value = true
  try {
    embeddingStatus.value = await getEmbeddingStatus()
  } catch {
    embeddingStatus.value = null
  } finally {
    embeddingStatusLoading.value = false
  }
}

async function onSyncIncremental() {
  if (!canSyncEmbedding.value || embeddingSyncLoading.value) return
  embeddingSyncLoading.value = true
  try {
    const result = await syncEmbeddingsIncremental()
    ElMessage.success(formatSyncResult('增量同步', result))
    await loadEmbeddingStatus()
  } finally {
    embeddingSyncLoading.value = false
  }
}

async function onSyncFull() {
  if (!canSyncEmbedding.value || embeddingSyncLoading.value) return
  try {
    await ElMessageBox.confirm(
      '全量同步将重新处理全部有效病历，病历较多时可能耗时数分钟。是否继续？',
      '全量向量化同步',
      { type: 'warning', confirmButtonText: '开始同步', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  embeddingSyncLoading.value = true
  try {
    const result = await syncEmbeddingsFull()
    ElMessage.success(formatSyncResult('全量同步', result))
    await loadEmbeddingStatus()
  } finally {
    embeddingSyncLoading.value = false
  }
}

async function onSavePrintTemplate() {
  loading.value = true
  try {
    await updateSetting('prescription_print_active_template', printTemplate.value)
    ElMessage.success('处方打印模板已保存')
    await loadSettings()
  } finally {
    loading.value = false
  }
}

async function onChangePassword() {
  loading.value = true
  try {
    await changePassword(
      pwdForm.currentPassword,
      pwdForm.newPassword,
      pwdForm.confirmPassword,
    )
    ElMessage.success('密码已修改')
    pwdForm.currentPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    loading.value = false
  }
}

async function onSaveClinicName() {
  loading.value = true
  try {
    await updateSetting('clinic_name', clinicName.value)
    ElMessage.success('诊所名称已保存')
    await loadSettings()
  } finally {
    loading.value = false
  }
}

async function onLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}

onMounted(() => {
  void loadSettings()
  void loadEmbeddingStatus()
})
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="hover">
      <template #header>
        <span class="title">系统设置</span>
      </template>

      <h3 class="section">修改密码</h3>
      <el-form label-width="100px" @submit.prevent="onChangePassword">
        <el-form-item label="当前密码">
          <el-input v-model="pwdForm.currentPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">保存密码</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 class="section">基础设置</h3>
      <el-form label-width="100px" @submit.prevent="onSaveClinicName">
        <el-form-item label="诊所名称">
          <el-input v-model="clinicName" placeholder="发凤村卫生室" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">保存设置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 class="section">处方打印</h3>
      <el-form label-width="120px" @submit.prevent="onSavePrintTemplate">
        <el-form-item label="打印模板">
          <el-select v-model="printTemplate" style="width: 100%">
            <el-option label="A4 通用模板（默认）" value="default-a4" />
            <el-option label="预印纸对齐（仅打印数据）" value="preprinted-fafeng" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">保存打印模板</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 class="section">病历向量化</h3>
      <p class="hint">管理历史病历脱敏向量，供相似病例参考。首次启用 RAG 后请执行一次全量同步。</p>

      <el-alert
        v-if="embeddingAlertMessage"
        type="info"
        :closable="false"
        show-icon
        class="embedding-alert"
        :title="embeddingAlertMessage"
      />

      <div v-loading="embeddingStatusLoading" class="embedding-panel">
        <el-descriptions v-if="embeddingStatus" :column="1" border size="small" class="embedding-desc">
          <el-descriptions-item label="服务状态">
            <el-tag
              :type="embeddingStatus.enabled && embeddingStatus.configured ? 'success' : 'info'"
              size="small"
            >
              {{ embeddingServiceLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模型">
            {{ embeddingStatus.provider }} / {{ embeddingStatus.model }}（{{ embeddingStatus.dimensions }} 维）
          </el-descriptions-item>
          <el-descriptions-item label="有效病历">{{ embeddingStatus.activeVisitCount }}</el-descriptions-item>
          <el-descriptions-item label="已同步">{{ embeddingStatus.syncedCount }}</el-descriptions-item>
          <el-descriptions-item label="待同步">
            <span :class="{ 'pending-warn': embeddingStatus.pendingCount > 0 }">
              {{ embeddingStatus.pendingCount }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="最近同步">
            {{ formatSyncedAt(embeddingStatus.latestSyncedAt) }}
          </el-descriptions-item>
        </el-descriptions>
        <p v-else-if="!embeddingStatusLoading" class="hint">暂时无法读取向量化状态，请稍后重试。</p>

        <div class="embedding-actions">
          <el-button
            :loading="embeddingStatusLoading"
            :disabled="embeddingSyncLoading"
            @click="loadEmbeddingStatus"
          >
            刷新状态
          </el-button>
          <el-button
            type="primary"
            plain
            :loading="embeddingSyncLoading"
            :disabled="!canSyncEmbedding || embeddingStatusLoading"
            @click="onSyncIncremental"
          >
            增量同步
          </el-button>
          <el-button
            type="warning"
            plain
            :loading="embeddingSyncLoading"
            :disabled="!canSyncEmbedding || embeddingStatusLoading"
            @click="onSyncFull"
          >
            全量同步
          </el-button>
        </div>
      </div>

      <el-divider />

      <h3 class="section">快捷语</h3>
      <p class="hint">管理病历录入常用文本，也可在录入页点击候选语快速填入。</p>
      <el-button type="primary" plain @click="router.push('/settings/quick-phrases')">
        管理快捷语
      </el-button>

      <el-divider />

      <el-button type="danger" plain @click="onLogout">退出登录</el-button>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
  display: flex;
  justify-content: center;
}

.card {
  width: min(560px, 100%);
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
}

.section {
  margin: 0 0 12px;
  font-size: 1rem;
  color: #303133;
}

.hint {
  margin: 0 0 12px;
  color: #909399;
  font-size: 14px;
}

.embedding-alert {
  margin-bottom: 12px;
}

.embedding-panel {
  min-height: 80px;
}

.embedding-desc {
  margin-bottom: 12px;
}

.pending-warn {
  color: #e6a23c;
  font-weight: 600;
}

.embedding-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 768px) {
  .page {
    padding: 12px;
  }

  .embedding-actions {
    flex-direction: column;
  }

  .embedding-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
