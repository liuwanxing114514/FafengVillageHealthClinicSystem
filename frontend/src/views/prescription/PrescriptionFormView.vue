<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchMedicines } from '@/api/medicine'
import { getVisit } from '@/api/visit'
import {
  createPrescription,
  generateOutboundDraft,
  getPrescription,
  updatePrescription,
  voidPrescription,
} from '@/api/prescription'
import { useTabTitle } from '@/composables/useTabTitle'
import QuickPhraseChips from '@/components/visit/QuickPhraseChips.vue'
import type { MedicineListItem } from '@/types/medicine'
import type { PrescriptionItem } from '@/types/prescription'

interface EditableItem {
  medicineId: number | null
  medicineName: string
  specification: string
  dosageForm: string
  baseUnit: string
  packageUnit: string
  quantity: number | null
  unit: string
  usage: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const patientName = ref('')
const medicineKeyword = ref('')
const medicineOptions = ref<MedicineListItem[]>([])

const isNew = computed(() => route.params.id === 'new')
const prescriptionId = computed(() => (isNew.value ? null : Number(route.params.id)))

const form = reactive({
  patientId: 0,
  visitId: 0,
  prescriptionDate: new Date().toISOString().slice(0, 10),
  diagnosis: '',
  items: [] as EditableItem[],
})

useTabTitle(computed(() => {
  if (isNew.value) {
    return patientName.value ? `新建处方 · ${patientName.value}` : '新建处方'
  }
  return patientName.value ? `处方 · ${patientName.value}` : `处方 #${prescriptionId.value}`
}))

function emptyItem(): EditableItem {
  return {
    medicineId: null,
    medicineName: '',
    specification: '',
    dosageForm: '',
    baseUnit: '',
    packageUnit: '',
    quantity: null,
    unit: '',
    usage: '',
  }
}

function unitOptions(item: EditableItem) {
  const units = new Set<string>()
  if (item.baseUnit) units.add(item.baseUnit)
  if (item.packageUnit) units.add(item.packageUnit)
  return Array.from(units)
}

async function searchMedicineOptions(keyword: string) {
  if (!keyword.trim()) {
    medicineOptions.value = []
    return
  }
  const page = await searchMedicines({ keyword: keyword.trim(), page: 1, size: 20 })
  medicineOptions.value = page.records
}

function onMedicineSelect(item: EditableItem, medicine: MedicineListItem) {
  item.medicineId = medicine.id
  item.medicineName = medicine.name
  item.specification = medicine.specification ?? ''
  item.dosageForm = medicine.dosageForm ?? ''
  item.baseUnit = medicine.baseUnit
  item.packageUnit = medicine.packageUnit ?? ''
  item.unit = medicine.baseUnit
}

function addItem() {
  form.items.push(emptyItem())
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

function mapItemsFromDetail(items: PrescriptionItem[]): EditableItem[] {
  return items.map((item) => ({
    medicineId: item.medicineId,
    medicineName: item.medicineName,
    specification: item.specification ?? '',
    dosageForm: item.dosageForm ?? '',
    baseUnit: item.unit,
    packageUnit: '',
    quantity: Number(item.quantity),
    unit: item.unit,
    usage: item.usage ?? '',
  }))
}

async function loadForm() {
  if (isNew.value) {
    const visitId = Number(route.query.visitId)
    const patientId = Number(route.query.patientId)
    if (!visitId || !patientId) {
      ElMessage.warning('请从病历页开处方')
      router.replace('/patient')
      return
    }
    loading.value = true
    try {
      const visit = await getVisit(visitId)
      if (visit.patientId !== patientId) {
        ElMessage.error('患者与病历不匹配')
        router.replace('/patient')
        return
      }
      form.patientId = patientId
      form.visitId = visitId
      patientName.value = visit.patientName
      form.diagnosis = visit.diagnosis ?? ''
      if (form.items.length === 0) addItem()
    } finally {
      loading.value = false
    }
    return
  }

  if (!prescriptionId.value) return
  loading.value = true
  try {
    const detail = await getPrescription(prescriptionId.value)
    form.patientId = detail.patientId
    form.visitId = detail.visitId
    patientName.value = detail.patientName
    form.prescriptionDate = detail.prescriptionDate
    form.diagnosis = detail.diagnosis ?? ''
    form.items = mapItemsFromDetail(detail.items)
    if (form.items.length === 0) addItem()
  } finally {
    loading.value = false
  }
}

function buildPayload() {
  return {
    patientId: form.patientId,
    visitId: form.visitId,
    prescriptionDate: form.prescriptionDate || undefined,
    diagnosis: form.diagnosis.trim() || undefined,
    items: form.items
      .filter((item) => item.medicineId && item.quantity && item.unit)
      .map((item) => ({
        medicineId: item.medicineId!,
        quantity: item.quantity!,
        unit: item.unit,
        usage: item.usage.trim() || undefined,
      })),
  }
}

async function onSave() {
  const payload = buildPayload()
  if (payload.items.length === 0) {
    ElMessage.warning('请至少添加一种药品')
    return
  }
  saving.value = true
  try {
    if (isNew.value) {
      const created = await createPrescription(payload)
      ElMessage.success('处方已保存')
      router.replace(`/prescription/${created.id}`)
    } else if (prescriptionId.value) {
      await updatePrescription(prescriptionId.value, payload)
      ElMessage.success('已保存')
      await loadForm()
    }
  } finally {
    saving.value = false
  }
}

function goPrint() {
  if (prescriptionId.value) {
    const url = router.resolve(`/prescription/${prescriptionId.value}/print`).href
    window.open(url, '_blank')
  }
}

function goOutbound() {
  if (prescriptionId.value) {
    router.push(`/inventory/outbound?prescriptionId=${prescriptionId.value}`)
  }
}

async function onOutboundDraft() {
  if (!prescriptionId.value) return
  try {
    const draft = await generateOutboundDraft(prescriptionId.value)
    router.push({ name: 'outbound-draft', params: { id: draft.id } })
  } catch {
    // cancelled
  }
}

async function onVoid() {
  if (!prescriptionId.value) return
  try {
    await ElMessageBox.confirm('确定作废此处方吗？', '作废确认', { type: 'warning' })
    await voidPrescription(prescriptionId.value)
    ElMessage.success('处方已作废')
    router.push(`/visit/${form.visitId}`)
  } catch {
    // cancelled
  }
}

function goBack() {
  router.push(`/visit/${form.visitId}`)
}

onMounted(loadForm)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">{{ isNew ? '新建处方' : '编辑处方' }}</span>
          <div class="actions">
            <el-button @click="goBack">返回病历</el-button>
            <el-button v-if="!isNew" @click="onOutboundDraft">生成待出库清单</el-button>
            <el-button v-if="!isNew" @click="goOutbound">出库</el-button>
            <el-button v-if="!isNew" @click="goPrint">打印</el-button>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          </div>
        </div>
      </template>

      <p v-if="patientName" class="patient-line">患者：{{ patientName }}</p>

      <el-form label-width="96px" class="meta-form">
        <el-form-item label="处方日期">
          <el-date-picker v-model="form.prescriptionDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="诊断">
          <el-input v-model="form.diagnosis" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <div class="items-header">
        <span>药品明细</span>
        <el-button type="primary" link @click="addItem">添加药品</el-button>
      </div>

      <div v-for="(item, index) in form.items" :key="index" class="item-row">
        <el-select
          v-model="item.medicineId"
          filterable
          remote
          reserve-keyword
          placeholder="搜索药品"
          :remote-method="searchMedicineOptions"
          style="width: 220px"
          @change="(id: number) => {
            const med = medicineOptions.find(m => m.id === id)
            if (med) onMedicineSelect(item, med)
          }"
        >
          <el-option
            v-for="med in medicineOptions"
            :key="med.id"
            :label="`${med.name} ${med.specification ?? ''}`"
            :value="med.id"
          />
        </el-select>
        <el-input v-model="item.medicineName" placeholder="药品名称" style="width: 160px" disabled />
        <el-input-number v-model="item.quantity" :min="0.001" :precision="3" :step="1" />
        <el-select v-model="item.unit" placeholder="单位" style="width: 90px">
          <el-option v-for="u in unitOptions(item)" :key="u" :label="u" :value="u" />
        </el-select>
        <div class="usage-field" style="flex: 1">
          <el-input v-model="item.usage" placeholder="用法" />
          <QuickPhraseChips v-model="item.usage" field-key="prescription_usage" />
        </div>
        <el-button type="danger" link @click="removeItem(index)">删除</el-button>
      </div>

      <el-form-item v-if="!isNew" class="void-row">
        <el-button type="danger" plain @click="onVoid">作废处方</el-button>
      </el-form-item>
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
  flex-wrap: wrap;
}

.patient-line {
  margin: 0 0 16px;
  color: #606266;
}

.meta-form {
  max-width: 560px;
  margin-bottom: 16px;
}

.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}

.item-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.usage-field {
  min-width: 180px;
}

.void-row {
  margin-top: 24px;
}
</style>
