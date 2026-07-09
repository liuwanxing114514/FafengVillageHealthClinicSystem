<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Microphone, Promotion } from '@element-plus/icons-vue'
import { getAiStatus, transcribeVoice } from '@/api/ai'
import { getAgentLogs, postAgentChat } from '@/api/agent'
import type { AiStatus } from '@/types/ai'
import type { ChatMessage } from '@/types/agent'

const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const recording = ref(false)
const status = ref<AiStatus | null>(null)
const input = ref('')
const sessionId = ref<string>()
const messages = ref<ChatMessage[]>([])
const showLogs = ref(false)
const recentLogs = ref<Awaited<ReturnType<typeof getAgentLogs>>>([])

const aiReady = computed(() => status.value?.enabled && status.value?.providerAvailable)

let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []

onMounted(async () => {
  loading.value = true
  try {
    status.value = await getAiStatus()
  } finally {
    loading.value = false
  }
})

async function sendMessage() {
  const text = input.value.trim()
  if (!text || sending.value) return
  if (!aiReady.value) {
    ElMessage.warning('AI 未启用或不可用，请在系统设置中配置')
    return
  }

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  sending.value = true
  try {
    const response = await postAgentChat(text, sessionId.value)
    sessionId.value = response.sessionId
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      toolCalls: response.toolCalls,
      pendingActions: response.pendingActions,
    })
  } catch (err: unknown) {
    const msg = (err as { message?: string })?.message ?? '请求失败'
    ElMessage.error(msg)
  } finally {
    sending.value = false
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

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never" class="chat-card">
      <template #header>
        <div class="header-row">
          <span>AI 助手</span>
          <div class="header-actions">
            <el-tag v-if="status" :type="aiReady ? 'success' : 'info'" size="small">
              {{ status.enabled ? status.provider : '未启用' }}
            </el-tag>
            <el-button link type="primary" @click="loadLogs">执行日志</el-button>
          </div>
        </div>
      </template>

      <div v-if="!aiReady" class="notice">
        AI 功能未启用或 Provider 不可用。日常业务不受影响；启用后可用自然语言查询库存、患者等。
      </div>

      <div class="messages">
        <div v-if="messages.length === 0" class="empty-hint">
          试试：「阿莫西林还有多少」「哪些药快过期」「帮阿莫西林出库 5 盒」
        </div>
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="message"
          :class="msg.role"
        >
          <div class="bubble">{{ msg.content }}</div>
          <div v-if="msg.toolCalls?.length" class="tool-calls">
            <div v-for="(call, i) in msg.toolCalls" :key="i" class="tool-call">
              <el-tag size="small" :type="call.success ? 'success' : 'danger'">
                {{ call.toolName }}
              </el-tag>
              <span class="tool-meta">{{ call.argsSummary }} → {{ call.resultSummary }}</span>
              <span class="tool-duration">{{ call.durationMs }}ms</span>
            </div>
          </div>
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
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          placeholder="输入问题，Enter 发送"
          :disabled="sending"
          @keydown="onKeydown"
        />
        <div class="input-actions">
          <el-button :icon="Microphone" :type="recording ? 'danger' : 'default'" circle @click="toggleRecord" />
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="sendMessage">
            发送
          </el-button>
        </div>
      </div>
    </el-card>

    <el-drawer v-model="showLogs" title="Agent 执行日志" size="480px">
      <el-table :data="recentLogs" size="small" stripe>
        <el-table-column prop="toolName" label="工具" width="140" />
        <el-table-column prop="argsSummary" label="参数" show-overflow-tooltip />
        <el-table-column prop="resultSummary" label="结果" show-overflow-tooltip />
        <el-table-column prop="durationMs" label="耗时" width="70" />
      </el-table>
    </el-drawer>
  </main>
</template>

<style scoped>
.chat-card {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 140px);
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  min-height: 320px;
  max-height: 55vh;
  overflow-y: auto;
  padding: 8px 0;
}

.empty-hint {
  color: #909399;
  font-size: 14px;
  text-align: center;
  padding: 48px 16px;
}

.message {
  margin-bottom: 16px;
}

.message.user {
  text-align: right;
}

.message.user .bubble {
  background: #409eff;
  color: #fff;
  margin-left: auto;
}

.message.assistant .bubble {
  background: #f4f4f5;
  color: #303133;
}

.bubble {
  display: inline-block;
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 10px;
  text-align: left;
  white-space: pre-wrap;
  line-height: 1.5;
}

.tool-calls {
  margin-top: 8px;
  text-align: left;
}

.tool-call {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
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
  text-align: left;
}

.pending-actions .el-alert {
  margin-bottom: 8px;
}

.input-area {
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
