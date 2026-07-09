<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { BrowserMultiFormatReader } from '@zxing/library'
import { ElMessage } from 'element-plus'
import { findMedicineByBarcode } from '@/api/medicine'
import type { MedicineListItem } from '@/types/medicine'

const emit = defineEmits<{
  matched: [medicine: MedicineListItem]
}>()

const barcodeInput = ref('')
const inputRef = ref<HTMLInputElement>()
const lookingUp = ref(false)
const cameraOpen = ref(false)
const cameraError = ref('')
const videoRef = ref<HTMLVideoElement>()

let reader: BrowserMultiFormatReader | null = null

function canUseCamera() {
  return typeof window !== 'undefined'
    && window.isSecureContext
    && !!navigator.mediaDevices?.getUserMedia
}

function cameraUnavailableMessage() {
  if (typeof window !== 'undefined' && !window.isSecureContext) {
    return '手机扫码需要 HTTPS 或在本机 localhost 打开。局域网请用 https://电脑IP:5173 访问，并在浏览器中信任证书。'
  }
  return '当前浏览器不支持摄像头，请手动输入条码或使用扫码枪。'
}

async function lookup() {
  const code = barcodeInput.value.trim()
  if (!code || lookingUp.value) return
  lookingUp.value = true
  try {
    const medicine = await findMedicineByBarcode(code)
    emit('matched', medicine)
    barcodeInput.value = ''
  } catch {
    // 错误由 http 拦截器提示
  } finally {
    lookingUp.value = false
    await nextTick()
    inputRef.value?.focus()
  }
}

function stopCamera() {
  reader?.reset()
  reader = null
  const stream = videoRef.value?.srcObject
  if (stream instanceof MediaStream) {
    stream.getTracks().forEach((track) => track.stop())
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
}

async function startCamera() {
  if (!videoRef.value) return
  cameraError.value = ''
  if (!canUseCamera()) {
    cameraError.value = cameraUnavailableMessage()
    ElMessage.warning(cameraUnavailableMessage())
    return
  }
  reader = new BrowserMultiFormatReader()
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' } },
      audio: false,
    })
    videoRef.value.srcObject = stream
    await videoRef.value.play()
    reader.decodeFromStream(stream, videoRef.value, (result, error) => {
      if (result) {
        const text = result.getText()
        stopCamera()
        cameraOpen.value = false
        barcodeInput.value = text
        void lookup()
      }
      if (error && error.name !== 'NotFoundException') {
        // 持续扫描中 NotFoundException 可忽略
      }
    })
  } catch {
    cameraError.value = '无法打开摄像头，请检查浏览器权限或使用 HTTPS 访问。'
    ElMessage.error(cameraError.value)
    stopCamera()
  }
}

async function openCamera() {
  if (!canUseCamera()) {
    ElMessage.warning(cameraUnavailableMessage())
    return
  }
  cameraOpen.value = true
  await nextTick()
  await startCamera()
}

function onCameraClose() {
  stopCamera()
  cameraError.value = ''
}

watch(cameraOpen, (open) => {
  if (!open) stopCamera()
})

onMounted(() => {
  inputRef.value?.focus()
})

onBeforeUnmount(stopCamera)
</script>

<template>
  <div class="scan-panel">
    <el-input
      ref="inputRef"
      v-model="barcodeInput"
      placeholder="扫码枪扫描或手动输入条码后回车"
      clearable
      :disabled="lookingUp"
      @keyup.enter="lookup"
    >
      <template #append>
        <el-button :loading="lookingUp" @click="lookup">匹配</el-button>
      </template>
    </el-input>
    <el-button type="primary" plain @click="openCamera">手机扫码</el-button>
  </div>

  <el-dialog v-model="cameraOpen" title="扫描药盒条码" width="92%" @close="onCameraClose">
    <video ref="videoRef" class="camera-video" autoplay muted playsinline />
    <p v-if="cameraError" class="camera-error">{{ cameraError }}</p>
    <p v-else class="camera-hint">将条码对准摄像头，识别成功后自动关闭</p>
  </el-dialog>
</template>

<style scoped>
.scan-panel {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  max-width: 520px;
}
.scan-panel .el-input {
  flex: 1;
}
.camera-video {
  width: 100%;
  max-height: 320px;
  background: #000;
  border-radius: 4px;
}
.camera-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.camera-error {
  margin-top: 8px;
  color: #e6a23c;
  font-size: 13px;
  line-height: 1.5;
}
</style>
