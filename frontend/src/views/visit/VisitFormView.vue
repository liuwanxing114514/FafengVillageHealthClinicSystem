<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPatient, searchPatients } from '@/api/patient'
import { listPrescriptionsByVisit } from '@/api/prescription'
import { createVisit, deleteVisit, getVisit, getVisitFeeSummary, updateVisit } from '@/api/visit'
import { getVoiceStatus, getAiStatus, structureVisit } from '@/api/ai'
import QuickPatientDialog from '@/components/visit/QuickPatientDialog.vue'
import VoiceInputButton from '@/components/visit/VoiceInputButton.vue'
import QuickPhraseChips from '@/components/visit/QuickPhraseChips.vue'
import { useTabTitle } from '@/composables/useTabTitle'
import type { PatientListItem } from '@/types/patient'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const patientName = ref('')
const voiceAvailable = ref(false)
const aiAvailable = ref(false)
const structuring = ref(false)
const showQuickPatient = ref(false)
const patientSearching = ref(false)
const patientOptions = ref<PatientListItem[]>([])
const amountDueManuallyEdited = ref(false)
const referencePurchaseCost = ref(0)
const patientTotalArrears = ref(0)

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
  amountDue: 0 as number | null,
  amountPaid: 0 as number | null,
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

async function searchPatientOptions(keyword: string) {
  if (!keyword.trim()) {
    patientOptions.value = []
    return
  }
  patientSearching.value = true
  try {
    const result = await searchPatients({ keyword: keyword.trim(), page: 1, size: 20 })
    patientOptions.value = result.records
  } finally {
    patientSearching.value = false
  }
}

function onPatientSelected(id: number | null) {
  if (!id) {
    patientName.value = ''
    patientTotalArrears.value = 0
    return
  }
  const found = patientOptions.value.find((p) => p.id === id)
  if (found) patientName.value = found.name
  void loadPatientArrears(id)
}

async function loadPatientArrears(id: number) {
  try {
    const patient = await getPatient(id)
    patientName.value = patient.name
    patientTotalArrears.value = patient.totalArrears ?? 0
  } catch {
    patientTotalArrears.value = 0
  }
}

function onQuickPatientCreated(patient: { id: number; name: string }) {
  form.patientId = patient.id
  patientName.value = patient.name
  void loadPatientArrears(patient.id)
}

async function applySuggestedFee(options?: { force?: boolean; confirmIfEdited?: boolean }) {
  if (!visitId.value) return
  const summary = await getVisitFeeSummary(visitId.value)
  referencePurchaseCost.value = summary.referencePurchaseCost
  if (options?.confirmIfEdited && amountDueManuallyEdited.value) {
    try {
      await ElMessageBox.confirm('是否按处方更新应收？', '更新应收', {
        type: 'warning',
        confirmButtonText: '更新',
        cancelButtonText: '保留当前',
      })
      form.amountDue = summary.suggestedAmountDue
      amountDueManuallyEdited.value = false
    } catch {
      // keep current
    }
    return
  }
  if (!amountDueManuallyEdited.value || options?.force) {
    form.amountDue = summary.suggestedAmountDue
  }
}

function onAmountDueInput() {
  amountDueManuallyEdited.value = true
}

async function loadVisit() {
  if (isNew.value) {
    if (patientId.value) {
      form.patientId = patientId.value
      await loadPatientArrears(patientId.value)
      patientOptions.value = [{ id: patientId.value, name: patientName.value } as PatientListItem]
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
    form.amountDue = detail.amountDue
    form.amountPaid = detail.amountPaid
    referencePurchaseCost.value = detail.referencePurchaseCost
    patientTotalArrears.value = detail.patientTotalArrears
    patientOptions.value = [{ id: detail.patientId, name: detail.patientName } as PatientListItem]
    amountDueManuallyEdited.value = false
  } finally {
    loading.value = false
  }
}

onActivated(async () => {
  if (!isNew.value && visitId.value) {
    await applySuggestedFee({ confirmIfEdited: true })
  }
})

async function onSave() {
  if (!form.patientId) {
    ElMessage.warning('请选择或新建患者')
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
      amountDue: form.amountDue ?? 0,
      amountPaid: form.amountPaid ?? 0,
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

function goBack() {
  if (form.patientId) {
    router.push(`/patient/${form.patientId}`)
  } else {
    router.push('/visit')
  }
}

async function goPrescription() {
  if (!visitId.value || !form.patientId) {
    ElMessage.warning('请先保存病历')
    return
  }
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

async function onAiStructure() {
  const text = [form.chiefComplaint, form.presentIllness, form.pastHistory, form.diagnosis, form.treatment, form.remark]
    .filter((v) => v && v.trim())
    .join('\n')
  if (!text.trim()) {
    ElMessage.warning('请先输入或语音录入一些内容')
    return
  }
  if (!patientId.value) {
    ElMessage.warning('缺少患者信息')
    return
  }
  structuring.value = true
  try {
    const draft = await structureVisit(text, patientId.value)
    ElMessage.success('AI 整理完成，请确认草稿')
    router.push({ name: 'visit-draft', params: { id: draft.id }, query: { patientId: patientId.value } })
  } finally {
    structuring.value = false
  }
}

onMounted(async () => {
  await loadVisit()
  if (!isNew.value && visitId.value) {
    const prescriptions = await listPrescriptionsByVisit(visitId.value)
    if (prescriptions.length > 0) {
      await applySuggestedFee({ force: !amountDueManuallyEdited.value })
    }
  }
  try {
    const [voiceStatus, aiStatus] = await Promise.all([getVoiceStatus(), getAiStatus()])
    voiceAvailable.value = voiceStatus.available
    aiAvailable.value = aiStatus.enabled && aiStatus.providerAvailable
  } catch {
    voiceAvailable.value = false
    aiAvailable.value = false
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
            <el-button @click="goBack">返回患者</el-button>
            <el-button v-if="!isNew" @click="goPrescription">开处方</el-button>
            <el-button v-if="aiAvailable" :loading="structuring" @click="onAiStructure">AI 整理</el-button>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="patientTotalArrears > 0"
        type="warning"
        :closable="false"
        show-icon
        class="arrears-alert"
        :title="`该患者累计欠款 ¥${patientTotalArrears.toFixed(2)}，请注意收款`"
      />

      <el-form label-width="108px" class="form-grid">
        <el-form-item label="患者" required>
          <div class="patient-picker">
            <el-select
              v-model="form.patientId"
              filterable
              remote
              reserve-keyword
              placeholder="搜索姓名或电话"
              :remote-method="searchPatientOptions"
              :loading="patientSearching"
              style="flex: 1"
              @change="onPatientSelected"
            >
              <el-option
                v-for="p in patientOptions"
                :key="p.id"
                :label="`${p.name} ${p.phone || ''}`"
                :value="p.id"
              />
            </el-select>
            <el-button @click="showQuickPatient = true">新建患者</el-button>
          </div>
        </el-form-item>
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
        <el-divider content-position="left">收费</el-divider>
        <el-form-item label="应收(元)">
          <div class="fee-field">
            <el-input-number v-model="form.amountDue" :min="0" :precision="2" :step="1" @change="onAmountDueInput" />
            <span class="fee-hint">以下为按处方计算的默认应收，可手工修改</span>
          </div>
        </el-form-item>
        <el-form-item label="实收(元)">
          <el-input-number v-model="form.amountPaid" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item v-if="referencePurchaseCost > 0" label="参考成本">
          <span class="cost-hint">参考进货成本：¥{{ referencePurchaseCost.toFixed(2) }}（仅内部参考，不打印）</span>
        </el-form-item>
        <el-form-item v-if="!isNew">
          <el-button type="danger" plain @click="onDelete">删除病历</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <QuickPatientDialog v-model:visible="showQuickPatient" @created="onQuickPatientCreated" />
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

.arrears-alert {
  margin-bottom: 16px;
}

.patient-picker {
  display: flex;
  gap: 8px;
  width: 100%;
}

.fee-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fee-hint,
.cost-hint {
  font-size: 12px;
  color: #909399;
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
