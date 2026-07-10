<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword } from '@/api/auth'
import {
  createChatChannel,
  createEmbeddingChannel,
  deleteChatChannel,
  deleteEmbeddingChannel,
  getEmbeddingStatus,
  getExternalServices,
  importAiChannelsFromEnv,
  listChatChannels,
  listEmbeddingChannels,
  reorderChatChannels,
  reorderEmbeddingChannels,
  syncEmbeddingsFull,
  syncEmbeddingsIncremental,
  testChatChannel,
  testEmbeddingChannel,
  updateChatChannel,
  updateEmbeddingChannel,
  updateExternalService,
} from '@/api/ai'
import { fetchSettings, updateSetting } from '@/api/settings'
import type { SettingItem } from '@/types/api'
import type {
  ChatChannel,
  EmbeddingChannel,
  ExternalServicesOverview,
  SaveChatChannelPayload,
  SaveEmbeddingChannelPayload,
  VisitEmbeddingStatus,
} from '@/types/ai'
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

const externalServices = ref<ExternalServicesOverview | null>(null)
const externalServicesLoading = ref(false)
const serviceSaving = ref<string | null>(null)

const chatChannels = ref<ChatChannel[]>([])
const embeddingChannels = ref<EmbeddingChannel[]>([])
const channelsLoading = ref(false)
const channelActionLoading = ref(false)

const chatDialogVisible = ref(false)
const embeddingDialogVisible = ref(false)
const editingChatId = ref<string | null>(null)
const editingEmbeddingId = ref<string | null>(null)

const chatForm = reactive<SaveChatChannelPayload>({
  channelId: '',
  displayName: '',
  priority: 1,
  enabled: true,
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
  model: 'deepseek-chat',
  temperature: 0.2,
})

const embeddingForm = reactive<SaveEmbeddingChannelPayload>({
  channelId: '',
  displayName: '',
  priority: 1,
  enabled: true,
  baseUrl: 'https://api.siliconflow.cn',
  apiKey: '',
  model: 'BAAI/bge-m3',
  dimensions: 1024,
})

const whisperUrl = ref('')
const ocrUrl = ref('')
const ocrMode = ref<'local' | 'vision'>('vision')
const ocrVisionModel = ref('Pro/Qwen/Qwen2.5-VL-7B-Instruct')

const OCR_VISION_MODEL_OPTIONS = [
  { label: 'Pro/Qwen/Qwen2.5-VL-7B-Instruct（推荐，4GB NAS）', value: 'Pro/Qwen/Qwen2.5-VL-7B-Instruct' },
  { label: 'Qwen/Qwen2.5-VL-7B-Instruct', value: 'Qwen/Qwen2.5-VL-7B-Instruct' },
  { label: 'Qwen/Qwen2.5-VL-32B-Instruct（更准，更贵）', value: 'Qwen/Qwen2.5-VL-32B-Instruct' },
]

const canSyncEmbedding = computed(
  () => Boolean(embeddingStatus.value?.enabled && embeddingStatus.value?.configured),
)

const showImportFromEnv = computed(
  () =>
    externalServices.value &&
    !externalServices.value.dbBacked &&
    !externalServices.value.dbChatChannels &&
    !externalServices.value.dbEmbeddingChannels,
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
    return '病历向量化未启用。可在下方「AI 外部服务」中开启，或配置向量接口。关闭时不影响日常业务。'
  }
  if (!embeddingStatus.value.configured) {
    return '向量接口配置不完整，请新增向量接口或从环境导入。'
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

function serviceItem(code: string) {
  return externalServices.value?.services?.[code]
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

async function loadExternalServices() {
  externalServicesLoading.value = true
  try {
    externalServices.value = await getExternalServices()
    whisperUrl.value = serviceItem('whisper')?.endpointUrl ?? ''
    ocrUrl.value = serviceItem('ocr')?.endpointUrl ?? ''
    ocrMode.value = serviceItem('ocr')?.ocrMode === 'local' ? 'local' : 'vision'
    ocrVisionModel.value = serviceItem('ocr')?.visionModel || 'Pro/Qwen/Qwen2.5-VL-7B-Instruct'
  } catch {
    externalServices.value = null
  } finally {
    externalServicesLoading.value = false
  }
}

async function loadChannels() {
  channelsLoading.value = true
  try {
    const [chat, embedding] = await Promise.all([listChatChannels(), listEmbeddingChannels()])
    chatChannels.value = chat
    embeddingChannels.value = embedding
  } catch {
    chatChannels.value = []
    embeddingChannels.value = []
  } finally {
    channelsLoading.value = false
  }
}

async function onToggleService(code: 'chat' | 'embedding' | 'whisper' | 'ocr', enabled: boolean) {
  serviceSaving.value = code
  try {
    const payload: {
      enabled: boolean
      endpointUrl?: string
      ocrMode?: string
      visionModel?: string
    } = { enabled }
    if (code === 'whisper') payload.endpointUrl = whisperUrl.value
    if (code === 'ocr') {
      payload.endpointUrl = ocrUrl.value
      payload.ocrMode = ocrMode.value
      payload.visionModel = ocrVisionModel.value
    }
    await updateExternalService(code, payload)
    ElMessage.success('已保存，立即生效')
    await Promise.all([loadExternalServices(), loadEmbeddingStatus()])
  } finally {
    serviceSaving.value = null
  }
}

async function onSaveServiceUrl(code: 'whisper' | 'ocr') {
  const item = serviceItem(code)
  if (!item?.enabled) {
    ElMessage.warning('请先开启对应服务')
    return
  }
  serviceSaving.value = code
  try {
    const payload: {
      enabled: boolean
      endpointUrl?: string
      ocrMode?: string
      visionModel?: string
    } = {
      enabled: true,
      endpointUrl: code === 'whisper' ? whisperUrl.value : ocrUrl.value,
    }
    if (code === 'ocr') {
      payload.ocrMode = ocrMode.value
      payload.visionModel = ocrVisionModel.value
    }
    await updateExternalService(code, payload)
    ElMessage.success('服务地址已保存')
    await loadExternalServices()
  } finally {
    serviceSaving.value = null
  }
}

function resetChatForm() {
  editingChatId.value = null
  chatForm.channelId = ''
  chatForm.displayName = ''
  chatForm.priority = chatChannels.value.length + 1
  chatForm.enabled = true
  chatForm.baseUrl = 'https://api.deepseek.com'
  chatForm.apiKey = ''
  chatForm.model = 'deepseek-chat'
  chatForm.temperature = 0.2
}

function openCreateChatDialog() {
  resetChatForm()
  chatDialogVisible.value = true
}

function openEditChatDialog(row: ChatChannel) {
  editingChatId.value = row.channelId
  chatForm.channelId = row.channelId
  chatForm.displayName = row.displayName
  chatForm.priority = row.priority
  chatForm.enabled = row.enabled
  chatForm.baseUrl = row.baseUrl
  chatForm.apiKey = ''
  chatForm.model = row.model
  chatForm.temperature = Number(row.temperature ?? 0.2)
  chatDialogVisible.value = true
}

async function onSaveChatChannel() {
  if (!chatForm.displayName.trim() || !chatForm.baseUrl.trim() || !chatForm.model.trim()) {
    ElMessage.warning('请填写名称、接口地址和模型')
    return
  }
  if (!editingChatId.value && !chatForm.apiKey?.trim()) {
    ElMessage.warning('请填写 API 密钥')
    return
  }
  channelActionLoading.value = true
  try {
    if (editingChatId.value) {
      await updateChatChannel(editingChatId.value, { ...chatForm })
      ElMessage.success('对话接口已更新')
    } else {
      await createChatChannel({ ...chatForm })
      ElMessage.success('对话接口已新增')
    }
    chatDialogVisible.value = false
    await Promise.all([loadChannels(), loadExternalServices(), loadEmbeddingStatus()])
  } finally {
    channelActionLoading.value = false
  }
}

async function onDeleteChat(row: ChatChannel) {
  try {
    await ElMessageBox.confirm(`确定删除接口「${row.displayName}」？`, '删除对话接口', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  channelActionLoading.value = true
  try {
    await deleteChatChannel(row.channelId)
    ElMessage.success('已删除')
    await Promise.all([loadChannels(), loadExternalServices()])
  } finally {
    channelActionLoading.value = false
  }
}

async function onTestChat(row: ChatChannel) {
  channelActionLoading.value = true
  try {
    const result = await testChatChannel(row.channelId)
    if (result.success) ElMessage.success(result.message)
    else ElMessage.error(result.message)
  } finally {
    channelActionLoading.value = false
  }
}

async function moveChatChannel(index: number, direction: -1 | 1) {
  const target = index + direction
  if (target < 0 || target >= chatChannels.value.length) return
  const ids = chatChannels.value.map((c) => c.channelId)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  channelActionLoading.value = true
  try {
    await reorderChatChannels(ids)
    await loadChannels()
  } finally {
    channelActionLoading.value = false
  }
}

function resetEmbeddingForm() {
  editingEmbeddingId.value = null
  embeddingForm.channelId = ''
  embeddingForm.displayName = ''
  embeddingForm.priority = embeddingChannels.value.length + 1
  embeddingForm.enabled = true
  embeddingForm.baseUrl = 'https://api.siliconflow.cn'
  embeddingForm.apiKey = ''
  embeddingForm.model = 'BAAI/bge-m3'
  embeddingForm.dimensions = embeddingChannels.value[0]?.dimensions ?? 1024
}

function openCreateEmbeddingDialog() {
  resetEmbeddingForm()
  embeddingDialogVisible.value = true
}

function openEditEmbeddingDialog(row: EmbeddingChannel) {
  editingEmbeddingId.value = row.channelId
  embeddingForm.channelId = row.channelId
  embeddingForm.displayName = row.displayName
  embeddingForm.priority = row.priority
  embeddingForm.enabled = row.enabled
  embeddingForm.baseUrl = row.baseUrl
  embeddingForm.apiKey = ''
  embeddingForm.model = row.model
  embeddingForm.dimensions = row.dimensions
  embeddingDialogVisible.value = true
}

async function onSaveEmbeddingChannel() {
  if (!embeddingForm.displayName.trim() || !embeddingForm.baseUrl.trim() || !embeddingForm.model.trim()) {
    ElMessage.warning('请填写名称、接口地址和模型')
    return
  }
  if (!editingEmbeddingId.value && !embeddingForm.apiKey?.trim()) {
    ElMessage.warning('请填写 API 密钥')
    return
  }
  channelActionLoading.value = true
  try {
    if (editingEmbeddingId.value) {
      await updateEmbeddingChannel(editingEmbeddingId.value, { ...embeddingForm })
      ElMessage.success('向量接口已更新')
    } else {
      await createEmbeddingChannel({ ...embeddingForm })
      ElMessage.success('向量接口已新增')
    }
    embeddingDialogVisible.value = false
    await Promise.all([loadChannels(), loadExternalServices(), loadEmbeddingStatus()])
  } finally {
    channelActionLoading.value = false
  }
}

async function onDeleteEmbedding(row: EmbeddingChannel) {
  try {
    await ElMessageBox.confirm(`确定删除接口「${row.displayName}」？`, '删除向量接口', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  channelActionLoading.value = true
  try {
    await deleteEmbeddingChannel(row.channelId)
    ElMessage.success('已删除')
    await Promise.all([loadChannels(), loadExternalServices(), loadEmbeddingStatus()])
  } finally {
    channelActionLoading.value = false
  }
}

async function onTestEmbedding(row: EmbeddingChannel) {
  channelActionLoading.value = true
  try {
    const result = await testEmbeddingChannel(row.channelId)
    if (result.success) ElMessage.success(result.message)
    else ElMessage.error(result.message)
  } finally {
    channelActionLoading.value = false
  }
}

async function moveEmbeddingChannel(index: number, direction: -1 | 1) {
  const target = index + direction
  if (target < 0 || target >= embeddingChannels.value.length) return
  const ids = embeddingChannels.value.map((c) => c.channelId)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  channelActionLoading.value = true
  try {
    await reorderEmbeddingChannels(ids)
    await loadChannels()
  } finally {
    channelActionLoading.value = false
  }
}

async function onImportFromEnv() {
  try {
    await ElMessageBox.confirm(
      '将把当前服务器环境变量中的 AI 配置写入数据库，之后以数据库为准。是否继续？',
      '从当前环境导入',
      { type: 'info', confirmButtonText: '导入', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  channelActionLoading.value = true
  try {
    await importAiChannelsFromEnv()
    ElMessage.success('导入成功，配置已生效')
    await Promise.all([loadExternalServices(), loadChannels(), loadEmbeddingStatus()])
  } finally {
    channelActionLoading.value = false
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
  void loadExternalServices()
  void loadChannels()
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

      <h3 class="section">AI 外部服务</h3>
      <p class="hint">在此统一管理 AI 能力开关，保存后立即生效，一般无需修改服务器 .env 文件。</p>

      <el-alert
        v-if="showImportFromEnv"
        type="info"
        :closable="false"
        show-icon
        class="embedding-alert"
        title="尚未在系统中配置接口，当前仍使用服务器环境变量。可点击「从当前环境导入」一键写入，或手动新增。"
      />

      <div v-if="showImportFromEnv" class="import-row">
        <el-button type="primary" plain :loading="channelActionLoading" @click="onImportFromEnv">
          从当前环境导入
        </el-button>
      </div>

      <div v-loading="externalServicesLoading" class="service-panel">
        <div class="service-row">
          <div class="service-info">
            <strong>AI 对话与助手</strong>
            <span class="hint-inline">病历整理、AI 助手</span>
            <span v-if="serviceItem('chat')" class="hint-inline">
              · 已配置 {{ serviceItem('chat')?.channelCount ?? 0 }} 个对话接口
            </span>
          </div>
          <el-switch
            :model-value="serviceItem('chat')?.enabled ?? false"
            :loading="serviceSaving === 'chat'"
            @change="(v: boolean) => onToggleService('chat', v)"
          />
        </div>

        <div class="service-row">
          <div class="service-info">
            <strong>病历向量化（相似病例）</strong>
            <span v-if="serviceItem('embedding')" class="hint-inline">
              · 已配置 {{ serviceItem('embedding')?.channelCount ?? 0 }} 个向量接口
            </span>
          </div>
          <el-switch
            :model-value="serviceItem('embedding')?.enabled ?? false"
            :loading="serviceSaving === 'embedding'"
            @change="(v: boolean) => onToggleService('embedding', v)"
          />
        </div>

        <div class="service-row service-row-url">
          <div class="service-info">
            <strong>语音转写</strong>
            <el-input
              v-model="whisperUrl"
              placeholder="http://whisper-service:9000"
              class="url-input"
            />
            <span class="hint">Whisper 服务地址，留空表示不启用</span>
          </div>
          <div class="service-actions">
            <el-switch
              :model-value="serviceItem('whisper')?.enabled ?? false"
              :loading="serviceSaving === 'whisper'"
              @change="(v: boolean) => onToggleService('whisper', v)"
            />
            <el-button size="small" :loading="serviceSaving === 'whisper'" @click="onSaveServiceUrl('whisper')">
              保存地址
            </el-button>
          </div>
        </div>

        <div class="service-row service-row-url">
          <div class="service-info">
            <strong>进货单识别</strong>
            <el-radio-group v-model="ocrMode" style="margin: 8px 0">
              <el-radio value="vision">云端 Vision（推荐 4GB NAS，复用对话 API Key）</el-radio>
              <el-radio value="local">本地 PaddleOCR（需 ocr-service 容器）</el-radio>
            </el-radio-group>
            <el-select
              v-if="ocrMode === 'vision'"
              v-model="ocrVisionModel"
              placeholder="选择视觉模型"
              class="url-input"
              style="margin-bottom: 8px"
            >
              <el-option
                v-for="opt in OCR_VISION_MODEL_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span v-if="ocrMode === 'vision'" class="hint">
              图片识别用 VL 模型；整理 JSON 仍用上方「对话模型」的 DeepSeek-V4-Pro，勿把对话通道改成 VL。
            </span>
            <el-input
              v-if="ocrMode === 'local'"
              v-model="ocrUrl"
              placeholder="http://ocr-service:8000"
              class="url-input"
            />
            <span v-if="ocrMode === 'local'" class="hint">PaddleOCR 服务地址，留空表示不启用</span>
          </div>
          <div class="service-actions">
            <el-switch
              :model-value="serviceItem('ocr')?.enabled ?? false"
              :loading="serviceSaving === 'ocr'"
              @change="(v: boolean) => onToggleService('ocr', v)"
            />
            <el-button size="small" :loading="serviceSaving === 'ocr'" @click="onSaveServiceUrl('ocr')">
              保存配置
            </el-button>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="section-header">
        <h3 class="section">对话模型接口</h3>
        <el-button type="primary" plain size="small" @click="openCreateChatDialog">新增对话接口</el-button>
      </div>
      <p class="hint">数字越小越优先；主接口填 1，备用填 2。限流时自动切换下一接口。</p>

      <el-table v-loading="channelsLoading" :data="chatChannels" size="small" border class="channel-table">
        <el-table-column prop="displayName" label="名称" min-width="100" />
        <el-table-column prop="baseUrl" label="接口地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" min-width="100" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="72" align="center" />
        <el-table-column label="状态" width="72" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row, $index }">
            <el-button link type="primary" size="small" @click="openEditChatDialog(row)">编辑</el-button>
            <el-button link type="primary" size="small" :loading="channelActionLoading" @click="onTestChat(row)">
              测试连接
            </el-button>
            <el-button link type="danger" size="small" @click="onDeleteChat(row)">删除</el-button>
            <el-button link size="small" :disabled="$index === 0" @click="moveChatChannel($index, -1)">上移</el-button>
            <el-button
              link
              size="small"
              :disabled="$index === chatChannels.length - 1"
              @click="moveChatChannel($index, 1)"
            >
              下移
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <div class="section-header">
        <h3 class="section">向量模型接口</h3>
        <el-button type="primary" plain size="small" @click="openCreateEmbeddingDialog">新增向量接口</el-button>
      </div>
      <p class="hint">向量维度须与已有接口一致（默认 1024），否则无法切换备用接口。</p>

      <el-table v-loading="channelsLoading" :data="embeddingChannels" size="small" border class="channel-table">
        <el-table-column prop="displayName" label="名称" min-width="100" />
        <el-table-column prop="baseUrl" label="接口地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" min-width="100" show-overflow-tooltip />
        <el-table-column prop="dimensions" label="向量维度" width="88" align="center" />
        <el-table-column prop="priority" label="优先级" width="72" align="center" />
        <el-table-column label="状态" width="72" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row, $index }">
            <el-button link type="primary" size="small" @click="openEditEmbeddingDialog(row)">编辑</el-button>
            <el-button link type="primary" size="small" :loading="channelActionLoading" @click="onTestEmbedding(row)">
              测试连接
            </el-button>
            <el-button link type="danger" size="small" @click="onDeleteEmbedding(row)">删除</el-button>
            <el-button link size="small" :disabled="$index === 0" @click="moveEmbeddingChannel($index, -1)">
              上移
            </el-button>
            <el-button
              link
              size="small"
              :disabled="$index === embeddingChannels.length - 1"
              @click="moveEmbeddingChannel($index, 1)"
            >
              下移
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <h3 class="section">病历向量化同步</h3>
      <p class="hint">在本页配置向量接口即可，一般无需修改服务器 .env 文件。首次启用 RAG 后请执行一次全量同步。</p>

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

    <el-dialog
      v-model="chatDialogVisible"
      :title="editingChatId ? '编辑对话接口' : '新增对话接口'"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="接口标识" required>
          <el-input v-model="chatForm.channelId" :disabled="!!editingChatId" placeholder="如 siliconflow-main" />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="chatForm.displayName" placeholder="如 硅基流动主通道" />
        </el-form-item>
        <el-form-item label="接口地址" required>
          <el-input v-model="chatForm.baseUrl" placeholder="https://api.deepseek.com" />
          <p class="field-hint">OpenAI 兼容地址，一般填到域名即可，不要重复加 /v1</p>
        </el-form-item>
        <el-form-item label="API 密钥" :required="!editingChatId">
          <el-input v-model="chatForm.apiKey" type="password" show-password placeholder="留空表示不修改已有密钥" />
          <p class="field-hint">密钥加密保存，页面仅显示末四位</p>
        </el-form-item>
        <el-form-item label="模型名称" required>
          <el-input
            v-model="chatForm.model"
            placeholder="官方 deepseek-chat；硅基 deepseek-ai/DeepSeek-V4-Pro"
          />
          <p class="field-hint">硅基流动 Chat/Agent 请用 deepseek-ai/DeepSeek-V4-Pro（支持工具调用）</p>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="chatForm.priority" :min="1" :max="99" />
          <p class="field-hint">数字越小越优先；主接口填 1，备用填 2</p>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="chatForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chatDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="channelActionLoading" @click="onSaveChatChannel">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="embeddingDialogVisible"
      :title="editingEmbeddingId ? '编辑向量接口' : '新增向量接口'"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="接口标识" required>
          <el-input
            v-model="embeddingForm.channelId"
            :disabled="!!editingEmbeddingId"
            placeholder="如 siliconflow-embed"
          />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="embeddingForm.displayName" placeholder="如 硅基流动向量" />
        </el-form-item>
        <el-form-item label="接口地址" required>
          <el-input v-model="embeddingForm.baseUrl" placeholder="https://api.siliconflow.cn" />
          <p class="field-hint">OpenAI 兼容地址，一般填到域名即可，不要重复加 /v1</p>
        </el-form-item>
        <el-form-item label="API 密钥" :required="!editingEmbeddingId">
          <el-input v-model="embeddingForm.apiKey" type="password" show-password placeholder="留空表示不修改已有密钥" />
          <p class="field-hint">密钥加密保存，页面仅显示末四位</p>
        </el-form-item>
        <el-form-item label="模型名称" required>
          <el-input v-model="embeddingForm.model" placeholder="BAAI/bge-m3" />
        </el-form-item>
        <el-form-item label="向量维度" required>
          <el-input-number v-model="embeddingForm.dimensions" :min="1" :max="4096" />
          <p class="field-hint">须与已有向量通道一致（默认 1024），否则无法切换备用接口</p>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="embeddingForm.priority" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="embeddingForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="embeddingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="channelActionLoading" @click="onSaveEmbeddingChannel">保存</el-button>
      </template>
    </el-dialog>
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
  width: min(860px, 100%);
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

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.section-header .section {
  margin: 0;
}

.hint {
  margin: 0 0 12px;
  color: #909399;
  font-size: 14px;
}

.hint-inline {
  display: block;
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.field-hint {
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.embedding-alert {
  margin-bottom: 12px;
}

.import-row {
  margin-bottom: 12px;
}

.service-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.service-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.service-row-url {
  align-items: center;
}

.service-info {
  flex: 1;
  min-width: 0;
}

.service-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.url-input {
  margin-top: 8px;
  max-width: 420px;
}

.channel-table {
  width: 100%;
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

  .service-row {
    flex-direction: column;
  }

  .service-actions {
    flex-direction: row;
    width: 100%;
    justify-content: space-between;
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
