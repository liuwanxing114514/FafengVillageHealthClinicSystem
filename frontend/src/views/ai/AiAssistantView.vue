<script setup lang="ts">
/**
 * AI 助手：分会话、多轮记忆、药品/患者跳转、A+B 档 UX。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DocumentCopy,
  FirstAidKit,
  Microphone,
  Promotion,
  User,
  Document,
  Delete,
  Plus,
} from '@element-plus/icons-vue'
import { getAiStatus, transcribeVoice } from '@/api/ai'
import {
  deleteAgentConversation,
  getAgentConversations,
  getAgentLogs,
  getAgentMessages,
  postAgentChat,
} from '@/api/agent'
import AgentToolResultCard from '@/components/agent/AgentToolResultCard.vue'
import type { AiStatus } from '@/types/ai'
import type { AgentConversation, AgentReference, ChatMessage } from '@/types/agent'
import { DRAFT_CONVERSATION_ID, DRAFT_STORAGE_KEY, EXAMPLE_PROMPTS } from '@/types/agent'
import { buildFollowUpChips, type FollowUpChip } from '@/utils/agentFollowUpChips'
import {
  formatDisplayToolArgs,
  providerLabel,
  toolLabel,
} from '@/utils/agentLabels'
import { formatRelativeTime } from '@/utils/formatRelativeTime'
import { renderMarkdownLite } from '@/utils/renderMarkdown'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const sending = ref(false)
const recording = ref(false)
const status = ref<AiStatus | null>(null)
const input = ref('')
const sessionId = ref<string>()
const messages = ref<ChatMessage[]>([])
const conversations = ref<AgentConversation[]>([])
const showLogs = ref(false)
const recentLogs = ref<Awaited<ReturnType<typeof getAgentLogs>>>([])
const messagesEl = ref<HTMLElement | null>(null)
const inputRef = ref<{ focus: () => void } | null>(null)
const sidebarOpen = ref(false)
/** 本地草稿会话（点「新建」后出现，首条消息成功发送或切走即消失） */
const draftActive = ref(false)
/** 防止「新建」清 URL 时 route watch 误拉回旧会话 */
const skipRouteConversationLoad = ref(false)

interface SidebarConversation extends AgentConversation {
  isDraft?: boolean
}

const sidebarItems = computed((): SidebarConversation[] => {
  const items: SidebarConversation[] = []
  if (draftActive.value) {
    items.push({
      id: DRAFT_CONVERSATION_ID,
      title: '新对话',
      messageCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      isDraft: true,
    })
  }
  for (const c of conversations.value) {
    items.push({ ...c, isDraft: false })
  }
  return items
})

const aiReady = computed(() => status.value?.enabled && status.value?.providerAvailable)
const statusLabel = computed(() => {
  if (!status.value) return '加载中'
  return providerLabel(status.value.provider, status.value.enabled && status.value.providerAvailable)
})
const showCharCount = computed(() => input.value.length > 1800)
const emptyHint = computed(() => {
  if (draftActive.value && messages.value.length === 0) {
    return '输入问题开始新对话，或点击下方示例'
  }
  if (conversations.value.length > 0 && !sessionId.value && !draftActive.value && messages.value.length === 0) {
    return '从左侧选择会话，或点击「新建对话」'
  }
  return '点击下方示例开始提问，或直接输入问题'
})

function isConversationActive(item: SidebarConversation): boolean {
  if (item.isDraft) {
    return draftActive.value && !sessionId.value
  }
  return sessionId.value === item.id
}

function onSidebarItemClick(item: SidebarConversation) {
  if (item.isDraft) {
    selectDraft()
  } else {
    selectConversation(item.id)
  }
}

let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let chatAbortController: AbortController | null = null

function focusInput() {
  nextTick(() => inputRef.value?.focus())
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

function syncUrlConversation() {
  const c = route.query.c
  if (typeof c === 'string' && c) {
    sessionId.value = c
  }
}

async function loadConversations() {
  try {
    conversations.value = await getAgentConversations()
  } catch {
    conversations.value = []
  }
}

function recordToChatMessages(
  records: Awaited<ReturnType<typeof getAgentMessages>>,
): ChatMessage[] {
  return records.map((r) => ({
    role: r.role,
    content: r.content,
    toolCalls: r.toolCalls?.length ? r.toolCalls : undefined,
    references: r.references?.length ? r.references : undefined,
    pendingActions: r.pendingActions?.length ? r.pendingActions : undefined,
  }))
}

async function selectConversation(id: string) {
  draftActive.value = false
  sessionId.value = id
  skipRouteConversationLoad.value = true
  try {
    await router.replace({ path: '/ai', query: { c: id } })
  } finally {
    skipRouteConversationLoad.value = false
  }
  loading.value = true
  try {
    const records = await getAgentMessages(id)
    messages.value = recordToChatMessages(records)
    scrollToBottom()
  } catch (err: unknown) {
    const msg = (err as { message?: string })?.message ?? '加载会话失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
    sidebarOpen.value = false
  }
}

async function selectDraft() {
  if (draftActive.value && !sessionId.value) {
    sidebarOpen.value = false
    focusInput()
    return
  }
  draftActive.value = true
  sessionId.value = undefined
  messages.value = []
  skipRouteConversationLoad.value = true
  try {
    await router.replace({ path: '/ai', query: {} })
  } finally {
    skipRouteConversationLoad.value = false
  }
  sidebarOpen.value = false
  focusInput()
}

async function newConversation() {
  draftActive.value = true
  sessionId.value = undefined
  messages.value = []
  skipRouteConversationLoad.value = true
  try {
    await router.replace({ path: '/ai', query: {} })
  } finally {
    skipRouteConversationLoad.value = false
  }
  sidebarOpen.value = false
  focusInput()
}

async function confirmDeleteConversation(id: string, e: Event) {
  e.stopPropagation()
  try {
    await ElMessageBox.confirm('确定删除该会话？删除后无法恢复。', '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteAgentConversation(id)
    if (sessionId.value === id) {
      newConversation()
    }
    await loadConversations()
    ElMessage.success('已删除')
  } catch {
    /* cancelled */
  }
}

async function sendText(text: string, isRetry = false) {
  const trimmed = text.trim()
  if (!trimmed || sending.value) return
  if (!aiReady.value) {
    ElMessage.warning('AI 未启用或不可用，请在系统设置中配置')
    return
  }

  if (!isRetry) {
    messages.value.push({ role: 'user', content: trimmed })
  } else {
    const last = messages.value[messages.value.length - 1]
    if (last?.role === 'user') {
      last.failed = false
    }
  }

  input.value = ''
  localStorage.removeItem(DRAFT_STORAGE_KEY)
  chatAbortController?.abort()
  chatAbortController = new AbortController()
  sending.value = true
  scrollToBottom()

  try {
    const response = await postAgentChat(trimmed, sessionId.value, chatAbortController.signal)
    draftActive.value = false
    sessionId.value = response.sessionId
    skipRouteConversationLoad.value = true
    try {
      await router.replace({ path: '/ai', query: { c: response.sessionId } })
    } finally {
      skipRouteConversationLoad.value = false
    }
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      toolCalls: response.toolCalls,
      pendingActions: response.pendingActions,
      references: response.references ?? [],
    })
    await loadConversations()
  } catch (err: unknown) {
    const canceled = (err as { code?: string; name?: string }).code === 'ERR_CANCELED'
      || (err as { name?: string }).name === 'CanceledError'
    if (!canceled) {
      const last = messages.value[messages.value.length - 1]
      if (last?.role === 'user') {
        last.failed = true
      }
    }
  } finally {
    chatAbortController = null
    sending.value = false
    scrollToBottom()
    focusInput()
  }
}

function cancelSend() {
  chatAbortController?.abort()
}

function sendMessage() {
  sendText(input.value)
}

function retryLast() {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'user' && last.failed) {
    sendText(last.content, true)
  }
}

function sendExample(text: string) {
  sendText(text)
}

function followUpChips(msg: ChatMessage): FollowUpChip[] {
  if (msg.role !== 'assistant') return []
  return buildFollowUpChips(msg.toolCalls)
}

function onFollowUp(chip: FollowUpChip) {
  if (chip.route) {
    router.push(chip.route)
    return
  }
  if (chip.message) {
    sendText(chip.message)
  }
}

async function copyContent(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function toggleRecord() {
  if (recording.value) {
    mediaRecorder?.stop()
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    ElMessage.warning('当前浏览器不支持录音')
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      recording.value = false
      stream.getTracks().forEach((t) => t.stop())
      if (audioChunks.length === 0) return
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      try {
        input.value = await transcribeVoice(blob)
      } catch {
        ElMessage.error('语音转写失败')
      }
    }
    mediaRecorder.start()
    recording.value = true
  } catch {
    ElMessage.error('无法访问麦克风')
  }
}

async function loadLogs() {
  showLogs.value = true
  recentLogs.value = await getAgentLogs(sessionId.value, 30)
}

function openDraft(draftId: number) {
  router.push({ name: 'outbound-draft', params: { id: draftId } })
}

function openReference(ref: AgentReference) {
  if (ref.refType === 'patient') {
    router.push({ name: 'patient-detail', params: { id: String(ref.refId) } })
    return
  }
  if (ref.refType === 'visit') {
    router.push({ name: 'visit-form', params: { id: String(ref.refId) } })
    return
  }
  if (ref.refType === 'medicine') {
    router.push({ name: 'medicine-edit', params: { id: String(ref.refId) } })
  }
}

function refIcon(refType: string) {
  if (refType === 'patient') return User
  if (refType === 'visit') return Document
  return FirstAidKit
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function goSettings() {
  router.push('/settings')
}

watch(input, (val) => {
  if (val) {
    localStorage.setItem(DRAFT_STORAGE_KEY, val)
  } else {
    localStorage.removeItem(DRAFT_STORAGE_KEY)
  }
})

watch(
  () => route.query.c,
  async (c) => {
    if (skipRouteConversationLoad.value) return
    if (typeof c === 'string' && c && c !== sessionId.value) {
      await selectConversation(c)
    }
  },
)

watch(messages, scrollToBottom, { deep: true })

onMounted(async () => {
  loading.value = true
  const draft = localStorage.getItem(DRAFT_STORAGE_KEY)
  if (draft) input.value = draft

  try {
    status.value = await getAiStatus()
    await loadConversations()
    syncUrlConversation()
    if (typeof route.query.c === 'string' && route.query.c) {
      await selectConversation(route.query.c)
    }
  } finally {
    loading.value = false
    focusInput()
  }
})
</script>

<template>
  <main class="page">
    <div class="layout">
      <!-- 侧栏：桌面 -->
      <aside class="sidebar desktop-only">
        <el-button type="primary" :icon="Plus" class="new-btn" @click="newConversation">
          新建对话
        </el-button>
        <div class="conv-list">
          <div
            v-for="conv in sidebarItems"
            :key="conv.id"
            class="conv-item"
            :class="{ active: isConversationActive(conv), draft: conv.isDraft }"
            @click="onSidebarItemClick(conv)"
          >
            <div class="conv-title">{{ conv.title }}</div>
            <div class="conv-meta">
              <span>{{ conv.isDraft ? '草稿' : formatRelativeTime(conv.updatedAt) }}</span>
              <el-button
                v-if="!conv.isDraft"
                link
                type="danger"
                :icon="Delete"
                size="small"
                @click="confirmDeleteConversation(conv.id, $event)"
              />
            </div>
          </div>
          <div v-if="sidebarItems.length === 0" class="conv-empty">暂无历史会话</div>
        </div>
      </aside>

      <!-- 主聊天区 -->
      <el-card v-loading="loading" shadow="never" class="chat-card">
        <template #header>
          <div class="header-row">
            <div class="header-left">
              <el-button class="mobile-only" size="small" @click="sidebarOpen = true">会话</el-button>
              <span>AI 助手</span>
            </div>
            <div class="header-actions">
              <el-tag v-if="status" :type="aiReady ? 'success' : 'info'" size="small">
                {{ statusLabel }}
              </el-tag>
              <el-button link type="primary" @click="loadLogs">调用记录</el-button>
            </div>
          </div>
        </template>

        <div v-if="!aiReady" class="notice">
          AI 功能未启用或不可用。日常业务不受影响；启用后可用自然语言查询库存、患者等。
          <el-button type="primary" link @click="goSettings">去系统设置</el-button>
        </div>

        <div ref="messagesEl" class="messages">
          <div v-if="messages.length === 0" class="empty-block">
            <p class="empty-hint">{{ emptyHint }}</p>
            <div v-if="draftActive || !sessionId || conversations.length === 0" class="example-chips">
              <el-tag
                v-for="ex in EXAMPLE_PROMPTS"
                :key="ex"
                class="chip"
                effect="plain"
                :disable-transitions="true"
                @click="sendExample(ex)"
              >
                {{ ex }}
              </el-tag>
            </div>
          </div>

          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message"
            :class="msg.role"
          >
            <div v-if="msg.role === 'user'" class="user-row">
              <div class="bubble">{{ msg.content }}</div>
              <el-button
                v-if="msg.failed"
                link
                type="primary"
                size="small"
                @click="retryLast"
              >
                重试
              </el-button>
            </div>

            <template v-else>
              <div class="assistant-row">
                <div class="bubble markdown" v-html="renderMarkdownLite(msg.content)" />
                <el-button
                  link
                  :icon="DocumentCopy"
                  class="copy-btn"
                  title="复制"
                  @click="copyContent(msg.content)"
                />
              </div>

              <AgentToolResultCard v-if="msg.toolCalls?.length" :tool-calls="msg.toolCalls" />

              <div v-if="msg.references?.length" class="references">
                <div
                  v-for="ref in msg.references"
                  :key="`${ref.refType}-${ref.refId}`"
                  class="ref-card"
                  @click="openReference(ref)"
                >
                  <el-icon><component :is="refIcon(ref.refType)" /></el-icon>
                  <div class="ref-body">
                    <div class="ref-label">{{ ref.label }}</div>
                    <div v-if="ref.hint" class="ref-hint">{{ ref.hint }}</div>
                  </div>
                </div>
              </div>

              <div v-if="followUpChips(msg).length" class="follow-ups">
                <el-tag
                  v-for="chip in followUpChips(msg)"
                  :key="chip.label"
                  class="chip"
                  effect="plain"
                  @click="onFollowUp(chip)"
                >
                  {{ chip.label }}
                </el-tag>
              </div>

              <details v-if="msg.toolCalls?.length" class="tool-details">
                <summary>查看调用详情</summary>
                <div class="tool-calls">
                  <div v-for="(call, i) in msg.toolCalls" :key="i" class="tool-call">
                    <el-tag size="small" :type="call.success ? 'success' : 'danger'">
                      {{ toolLabel(call.toolName) }}
                    </el-tag>
                    <span class="tool-meta">
                      {{ formatDisplayToolArgs(call) || '无参数' }} → {{ call.resultSummary }}
                    </span>
                    <span class="tool-duration">{{ call.durationMs }} 毫秒</span>
                  </div>
                </div>
              </details>

              <div v-if="msg.pendingActions?.length" class="pending-actions">
                <el-alert
                  v-for="action in msg.pendingActions"
                  :key="action.draftId"
                  type="warning"
                  :closable="false"
                  show-icon
                  :title="action.summary"
                >
                  <el-button type="primary" size="small" @click="openDraft(action.draftId)">
                    查看待确认清单
                  </el-button>
                </el-alert>
              </div>
            </template>
          </div>

          <div v-if="sending" class="sending-hint">
            正在查询…
            <el-button link type="primary" size="small" @click="cancelSend">取消</el-button>
          </div>
        </div>

        <div class="input-area">
          <div v-if="showCharCount" class="char-count">{{ input.length }}/2000</div>
          <el-input
            ref="inputRef"
            v-model="input"
            type="textarea"
            :rows="2"
            placeholder="Enter 发送，Shift+Enter 换行"
            :disabled="sending"
            @keydown="onKeydown"
          />
          <div class="input-actions">
            <el-button
              :icon="Microphone"
              :type="recording ? 'danger' : 'default'"
              circle
              :disabled="sending"
              @click="toggleRecord"
            />
            <el-button
              type="primary"
              :icon="Promotion"
              :loading="sending"
              :disabled="sending"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 移动端侧栏 -->
    <el-drawer v-model="sidebarOpen" title="会话列表" direction="ltr" size="280px">
      <el-button type="primary" :icon="Plus" class="new-btn" @click="newConversation">
        新建对话
      </el-button>
      <div class="conv-list">
        <div
          v-for="conv in sidebarItems"
          :key="conv.id"
          class="conv-item"
          :class="{ active: isConversationActive(conv), draft: conv.isDraft }"
          @click="onSidebarItemClick(conv)"
        >
          <div class="conv-title">{{ conv.title }}</div>
          <div class="conv-meta">
            <span>{{ conv.isDraft ? '草稿' : formatRelativeTime(conv.updatedAt) }}</span>
            <el-button
              v-if="!conv.isDraft"
              link
              type="danger"
              :icon="Delete"
              size="small"
              @click="confirmDeleteConversation(conv.id, $event)"
            />
          </div>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="showLogs" title="工具调用记录" size="480px">
      <el-table :data="recentLogs" size="small" stripe>
        <el-table-column label="工具" width="120">
          <template #default="{ row }">{{ toolLabel(row.toolName) }}</template>
        </el-table-column>
        <el-table-column label="参数" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDisplayToolArgs(row) || row.argsSummary }}</template>
        </el-table-column>
        <el-table-column prop="resultSummary" label="结果" show-overflow-tooltip />
        <el-table-column label="耗时" width="80">
          <template #default="{ row }">{{ row.durationMs }} 毫秒</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </main>
</template>

<style scoped>
.page {
  height: calc(100vh - 120px);
}

.layout {
  display: flex;
  gap: 12px;
  height: 100%;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.new-btn {
  width: 100%;
  margin-bottom: 12px;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
}

.conv-item:hover,
.conv-item.active {
  background: #ecf5ff;
}

.conv-item.draft .conv-title {
  color: #606266;
  font-style: italic;
}

.conv-title {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.conv-empty {
  font-size: 13px;
  color: #909399;
  text-align: center;
  padding: 24px 8px;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100%;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 6px;
  color: #e6a23c;
  font-size: 13px;
}

.messages {
  flex: 1;
  min-height: 280px;
  max-height: calc(100vh - 320px);
  overflow-y: auto;
  padding: 8px 0;
}

.empty-block {
  text-align: center;
  padding: 32px 16px;
}

.empty-hint {
  color: #909399;
  font-size: 14px;
  margin-bottom: 16px;
}

.example-chips,
.follow-ups {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.follow-ups {
  justify-content: flex-start;
  margin-top: 10px;
}

.chip {
  cursor: pointer;
}

.chip:hover {
  opacity: 0.85;
}

.message {
  margin-bottom: 16px;
}

.message.user {
  text-align: right;
}

.user-row {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  max-width: 85%;
}

.message.user .bubble {
  background: #409eff;
  color: #fff;
}

.assistant-row {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  max-width: 90%;
}

.message.assistant .bubble {
  background: #f4f4f5;
  color: #303133;
  flex: 1;
}

.bubble {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 10px;
  text-align: left;
  line-height: 1.5;
}

.bubble.markdown :deep(ul) {
  margin: 4px 0;
  padding-left: 20px;
}

.copy-btn {
  flex-shrink: 0;
  margin-top: 4px;
}

.references {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.ref-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  min-width: 160px;
}

.ref-card:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.ref-label {
  font-size: 14px;
  font-weight: 500;
}

.ref-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.tool-details {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}

.tool-details summary {
  cursor: pointer;
  color: #909399;
  user-select: none;
}

.tool-call {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.tool-meta {
  flex: 1;
  min-width: 120px;
}

.tool-duration {
  color: #909399;
}

.pending-actions {
  margin-top: 8px;
}

.pending-actions .el-alert {
  margin-bottom: 8px;
}

.sending-hint {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 8px;
}

.input-area {
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.desktop-only {
  display: flex;
}

.mobile-only {
  display: none;
}

@media (max-width: 768px) {
  .desktop-only {
    display: none;
  }
  .mobile-only {
    display: inline-flex;
  }
  .layout {
    flex-direction: column;
  }
}
</style>
