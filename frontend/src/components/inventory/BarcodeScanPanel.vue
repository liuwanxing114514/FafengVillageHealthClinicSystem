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
const videoRef = ref<HTMLVideoElement>()

let reader: BrowserMultiFormatReader | null = null

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
}

async function startCamera() {
  if (!videoRef.value) return
  reader = new BrowserMultiFormatReader()
  try {
    reader.decodeFromVideoDevice(undefined, videoRef.value, (result, error) => {
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
    ElMessage.error('无法打开摄像头，请检查浏览器权限')
    cameraOpen.value = false
    stopCamera()
  }
}

async function openCamera() {
  cameraOpen.value = true
  await nextTick()
  await startCamera()
}

function onCameraClose() {
  stopCamera()
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
    <p class="camera-hint">将条码对准摄像头，识别成功后自动关闭</p>
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
</style>
