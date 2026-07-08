<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Microphone } from '@element-plus/icons-vue'
import { transcribeVoice } from '@/api/ai'

const props = defineProps<{
  modelValue: string
  disabled?: boolean
  available?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const recording = ref(false)
const transcribing = ref(false)
const mediaRecorder = ref<MediaRecorder | null>(null)
const mediaStream = ref<MediaStream | null>(null)
const chunks = ref<BlobPart[]>([])

const buttonType = computed(() => (recording.value ? 'danger' : 'default'))
const buttonLabel = computed(() => {
  if (transcribing.value) return '转写中'
  if (recording.value) return '停止'
  return '语音'
})

async function toggleRecording() {
  if (props.disabled || transcribing.value) return
  if (!props.available) {
    ElMessage.warning('语音转写服务未配置，请手动输入')
    return
  }
  if (recording.value) {
    stopRecording()
    return
  }
  await startRecording()
}

async function startRecording() {
  if (!navigator.mediaDevices?.getUserMedia) {
    ElMessage.error('当前浏览器不支持录音')
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaStream.value = stream
    const recorder = new MediaRecorder(stream)
    chunks.value = []
    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunks.value.push(event.data)
      }
    }
    recorder.onstop = () => {
      void handleRecorded(new Blob(chunks.value, { type: recorder.mimeType || 'audio/webm' }))
    }
    recorder.start()
    mediaRecorder.value = recorder
    recording.value = true
  } catch {
    ElMessage.error('无法访问麦克风，请检查浏览器权限')
  }
}

function stopRecording() {
  mediaRecorder.value?.stop()
  mediaStream.value?.getTracks().forEach((track) => track.stop())
  mediaRecorder.value = null
  mediaStream.value = null
  recording.value = false
}

async function handleRecorded(blob: Blob) {
  if (!blob.size) {
    ElMessage.warning('未录到有效音频')
    return
  }
  transcribing.value = true
  try {
    const text = await transcribeVoice(blob)
    if (!text) {
      ElMessage.warning('未识别到有效语音内容')
      return
    }
    const current = props.modelValue.trim()
    emit('update:modelValue', current ? `${current} ${text}` : text)
    ElMessage.success('转写完成，请核对后保存')
  } finally {
    transcribing.value = false
  }
}

onBeforeUnmount(() => {
  if (recording.value) {
    stopRecording()
  }
})
</script>

<template>
  <el-button
    class="voice-btn"
    :type="buttonType"
    :icon="Microphone"
    :disabled="disabled || transcribing"
    :loading="transcribing"
    @click="toggleRecording"
  >
    {{ buttonLabel }}
  </el-button>
</template>

<style scoped>
.voice-btn {
  flex-shrink: 0;
}
</style>
