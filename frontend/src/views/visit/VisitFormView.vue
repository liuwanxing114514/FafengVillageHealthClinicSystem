<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPatient } from '@/api/patient'
import { createVisit, deleteVisit, getVisit, updateVisit } from '@/api/visit'
import { getVoiceStatus } from '@/api/ai'
import VoiceInputButton from '@/components/visit/VoiceInputButton.vue'
import QuickPhraseChips from '@/components/visit/QuickPhraseChips.vue'
import { useTabTitle } from '@/composables/useTabTitle'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const patientName = ref('')
const voiceAvailable = ref(false)

const isNew = computed(() => route.params.id === 'new')
const visitId = computed(() => (isNew.value ? null : Number(route.params.id)))
const patientId = computed(() => {
  if (isNew.value) {
    const fromQuery = Number(route.query.patientId)
    return Number.isFinite(fromQuery) && fromQuery > 0 ? fromQuery : null
  }
  return form.patientId
})

const form = reactive({
  patientId: 0,
  visitTime: '',
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  temperature: null as number | null,
  bloodPressure: '',
  spo2: null as number | null,
  etco2: null as number | null,
  heartRate: null as number | null,
  pulse: '',
  allergyHistory: '',
  diagnosis: '',
  treatment: '',
  remark: '',
})

useTabTitle(computed(() => {
  if (isNew.value) {
    return patientName.value ? `新建病历 · ${patientName.value}` : '新建病历'
  }
  const label = patientName.value || (form.diagnosis ? form.diagnosis.slice(0, 12) : '')
  return label ? `病历 · ${label}` : `病历 #${visitId.value}`
}))

function formatDateTimeLocal(value: string) {
  if (!value) return ''
  const normalized = value.replace(' ', 'T')
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})/)
  return match?.[1] ?? normalized.slice(0, 16)
}

async function loadVisit() {
  if (isNew.value) {
    if (patientId.value) {
      form.patientId = patientId.value
      try {
        const patient = await getPatient(patientId.value)
        patientName.value = patient.name
      } catch {
        ElMessage.error('患者不存在')
        router.replace('/patient')
      }
    } else {
      ElMessage.warning('请从患者详情页新建病历')
      router.replace('/patient')
    }
    return
  }
  if (!visitId.value) return
  loading.value = true
  try {
    const detail = await getVisit(visitId.value)
    form.patientId = detail.patientId
    patientName.value = detail.patientName
    form.visitTime = formatDateTimeLocal(detail.visitTime)
    form.chiefComplaint = detail.chiefComplaint ?? ''
    form.presentIllness = detail.presentIllness ?? ''
    form.pastHistory = detail.pastHistory ?? ''
    form.temperature = detail.temperature
    form.bloodPressure = detail.bloodPressure ?? ''
    form.spo2 = detail.spo2
    form.etco2 = detail.etco2
    form.heartRate = detail.heartRate
    form.pulse = detail.pulse ?? ''
    form.allergyHistory = detail.allergyHistory ?? ''
    form.diagnosis = detail.diagnosis ?? ''
    form.treatment = detail.treatment ?? ''
    form.remark = detail.remark ?? ''
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.patientId) {
    ElMessage.warning('缺少患者信息')
    return
  }
  saving.value = true
  try {
    const payload = {
      patientId: form.patientId,
      visitTime: form.visitTime ? `${form.visitTime}:00+08:00` : undefined,
      chiefComplaint: form.chiefComplaint.trim() || undefined,
      presentIllness: form.presentIllness.trim() || undefined,
      pastHistory: form.pastHistory.trim() || undefined,
      temperature: form.temperature,
      bloodPressure: form.bloodPressure.trim() || undefined,
      spo2: form.spo2,
      etco2: form.etco2,
      heartRate: form.heartRate ?? undefined,
      pulse: form.pulse.trim() || undefined,
      allergyHistory: form.allergyHistory.trim() || undefined,
      diagnosis: form.diagnosis.trim() || undefined,
      treatment: form.treatment.trim() || undefined,
      remark: form.remark.trim() || undefined,
    }
    if (isNew.value) {
      const created = await createVisit(payload)
      ElMessage.success('病历已创建')
      router.replace(`/visit/${created.id}`)
    } else if (visitId.value) {
      await updateVisit(visitId.value, payload)
      ElMessage.success('已保存')
      await loadVisit()
    }
  } finally {
    saving.value = false
  }
}

function goAiStructure() {
  const query: Record<string, string> = {}
  if (form.patientId) query.patientId = String(form.patientId)
  if (visitId.value) query.visitId = String(visitId.value)
  router.push({ path: '/ai', query })
}

function goBack() {
  if (form.patientId) {
    router.push(`/patient/${form.patientId}`)
  } else {
    router.push('/patient')
  }
}

function goPrescription() {
  if (!visitId.value || !form.patientId) return
  router.push(`/prescription/new?visitId=${visitId.value}&patientId=${form.patientId}`)
}

async function onDelete() {
  if (!visitId.value) return
  try {
    await ElMessageBox.confirm('确定要删除这条病历吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteVisit(visitId.value)
    ElMessage.success('已删除')
    goBack()
  } catch {
    // cancelled or failed
  }
}

onMounted(async () => {
  await loadVisit()
  try {
    const status = await getVoiceStatus()
    voiceAvailable.value = status.available
  } catch {
    voiceAvailable.value = false
  }
})
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">{{ isNew ? '新建病历' : '病历详情' }}</span>
          <div class="actions">
            <el-button @click="goAiStructure">AI 整理</el-button>
            <el-button @click="goBack">返回患者</el-button>
            <el-button v-if="!isNew" @click="goPrescription">开处方</el-button>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          </div>
        </div>
      </template>

      <p v-if="patientName" class="patient-line">患者：{{ patientName }}</p>

      <el-form label-width="108px" class="form-grid">
        <el-form-item label="就诊时间">
          <el-date-picker
            v-model="form.visitTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm"
            placeholder="默认当前时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="主诉">
          <div class="field-stack">
            <div class="field-with-voice">
              <el-input v-model="form.chiefComplaint" type="textarea" :rows="2" />
              <VoiceInputButton v-model="form.chiefComplaint" :available="voiceAvailable" />
            </div>
            <QuickPhraseChips v-model="form.chiefComplaint" field-key="chief_complaint" />
          </div>
        </el-form-item>
        <el-form-item label="现病史">
          <div class="field-stack">
            <div class="field-with-voice">
              <el-input v-model="form.presentIllness" type="textarea" :rows="3" />
              <VoiceInputButton v-model="form.presentIllness" :available="voiceAvailable" />
            </div>
            <QuickPhraseChips v-model="form.presentIllness" field-key="present_illness" />
          </div>
        </el-form-item>
        <el-form-item label="既往史">
          <div class="field-stack">
            <div class="field-with-voice">
              <el-input v-model="form.pastHistory" type="textarea" :rows="2" />
              <VoiceInputButton v-model="form.pastHistory" :available="voiceAvailable" />
            </div>
            <QuickPhraseChips v-model="form.pastHistory" field-key="past_history" />
          </div>
        </el-form-item>
        <el-form-item label="体温(℃)">
          <el-input-number v-model="form.temperature" :min="30" :max="45" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="血压">
          <el-input v-model="form.bloodPressure" placeholder="如 120/80" maxlength="16" />
        </el-form-item>
        <el-form-item label="血氧(%)">
          <el-input-number v-model="form.spo2" :min="0" :max="100" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="呼末CO₂">
          <el-input-number v-model="form.etco2" :min="0" :max="100" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="心率">
          <el-input-number v-model="form.heartRate" :min="0" :max="300" />
        </el-form-item>
        <el-form-item label="脉象">
          <el-input v-model="form.pulse" maxlength="64" />
        </el-form-item>
        <el-form-item label="过敏史">
          <div class="field-stack">
            <div class="field-with-voice">
              <el-input v-model="form.allergyHistory" type="textarea" :rows="2" />
              <VoiceInputButton v-model="form.allergyHistory" :available="voiceAvailable" />
            </div>
            <QuickPhraseChips v-model="form.allergyHistory" field-key="allergy_history" />
          </div>
        </el-form-item>
        <el-form-item label="诊断">
          <div class="field-stack">
            <div class="field-with-voice">
              <el-input v-model="form.diagnosis" type="textarea" :rows="2" />
              <VoiceInputButton v-model="form.diagnosis" :available="voiceAvailable" />
            </div>
            <QuickPhraseChips v-model="form.diagnosis" field-key="diagnosis" />
          </div>
        </el-form-item>
        <el-form-item label="处理意见">
          <div class="field-stack">
            <div class="field-with-voice">
              <el-input v-model="form.treatment" type="textarea" :rows="3" />
              <VoiceInputButton v-model="form.treatment" :available="voiceAvailable" />
            </div>
            <QuickPhraseChips v-model="form.treatment" field-key="treatment" />
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <div class="field-stack">
            <div class="field-with-voice">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
              <VoiceInputButton v-model="form.remark" :available="voiceAvailable" />
            </div>
            <QuickPhraseChips v-model="form.remark" field-key="remark" />
          </div>
        </el-form-item>
        <el-form-item v-if="!isNew">
          <el-button type="danger" plain @click="onDelete">删除病历</el-button>
        </el-form-item>
      </el-form>
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
}

.title {
  font-size: 1.1rem;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}

.patient-line {
  margin: 0 0 16px;
  color: #606266;
}

.form-grid {
  max-width: 760px;
}

.field-with-voice {
  display: flex;
  gap: 8px;
  width: 100%;
  align-items: flex-start;
}

.field-with-voice :deep(.el-textarea) {
  flex: 1;
}

.field-stack {
  width: 100%;
}
</style>
