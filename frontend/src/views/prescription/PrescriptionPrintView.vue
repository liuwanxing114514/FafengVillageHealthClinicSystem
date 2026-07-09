<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPrescriptionPrint } from '@/api/prescription'
import type { PrescriptionPrintConfig, PrescriptionPrintData } from '@/types/prescription'
import { buildPrescriptionPdfFilename, saveElementAsPdf } from '@/utils/savePdf'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const savingPdf = ref(false)
const printData = ref<PrescriptionPrintData | null>(null)
const printSheetRef = ref<HTMLElement | null>(null)

const prescriptionId = Number(route.params.id)

const templateConfig = computed<PrescriptionPrintConfig | null>(() => {
  if (!printData.value?.templateConfigJson) return null
  try {
    return JSON.parse(printData.value.templateConfigJson) as PrescriptionPrintConfig
  } catch {
    return null
  }
})

const activeTemplate = computed(() => {
  const id = printData.value?.activeTemplate ?? 'default-a4'
  return templateConfig.value?.templates?.[id] ?? null
})

const isOverlay = computed(() => activeTemplate.value?.type === 'overlay')

function fieldValue(key: string): string {
  const data = printData.value
  if (!data) return ''
  switch (key) {
    case 'patientName': return data.patientName
    case 'gender': return data.gender
    case 'age': return data.age != null ? String(data.age) : ''
    case 'visitRecordNo': return data.visitRecordNo != null ? String(data.visitRecordNo) : ''
    case 'department': return data.department || activeTemplate.value?.staticValues?.department || '全科'
    case 'address': return data.address || ''
    case 'phone': return data.phone || ''
    case 'diagnosis': return data.diagnosis || ''
    case 'dateYear': return data.prescriptionYear != null ? String(data.prescriptionYear) : ''
    case 'dateMonth': return data.prescriptionMonth != null ? String(data.prescriptionMonth) : ''
    case 'dateDay': return data.prescriptionDay != null ? String(data.prescriptionDay) : ''
    case 'doctorSignature': return ''
    default:
      return activeTemplate.value?.staticValues?.[key] ?? ''
  }
}

function itemLine(item: PrescriptionPrintData['items'][number], index: number): string {
  const spec = item.specification ? ` ${item.specification}` : ''
  const usage = item.usage ? ` ${item.usage}` : ''
  const form = item.dosageForm ? `${item.dosageForm} ` : ''
  return `${index + 1}. ${form}${item.medicineName}${spec} ×${item.quantity}${item.unit}${usage}`
}

async function loadPrint() {
  if (!prescriptionId) {
    ElMessage.error('无效的处方')
    router.push('/visit')
    return
  }
  loading.value = true
  try {
    printData.value = await getPrescriptionPrint(prescriptionId)
  } finally {
    loading.value = false
  }
}

function onPrint() {
  window.print()
}

async function onSavePdf() {
  if (!printSheetRef.value || !printData.value) {
    ElMessage.warning('处方内容尚未加载完成')
    return
  }
  savingPdf.value = true
  try {
    const filename = buildPrescriptionPdfFilename(
      printData.value.patientName,
      printData.value.prescriptionDate,
      prescriptionId,
    )
    await saveElementAsPdf(printSheetRef.value, filename)
    ElMessage.success('PDF 已保存到下载文件夹')
  } catch {
    ElMessage.error('保存 PDF 失败，请重试或使用浏览器打印另存为 PDF')
  } finally {
    savingPdf.value = false
  }
}

function goBack() {
  router.push(`/prescription/${prescriptionId}`)
}

onMounted(loadPrint)
</script>

<template>
  <main class="page">
    <div class="toolbar no-print">
      <el-button @click="goBack">返回编辑</el-button>
      <el-button type="primary" @click="onPrint">打印</el-button>
      <el-button :loading="savingPdf" @click="onSavePdf">保存 PDF</el-button>
      <span v-if="isOverlay" class="template-hint">当前：预印纸对齐模式（仅打印数据）</span>
    </div>

    <section ref="printSheetRef" v-loading="loading" class="print-sheet" :class="{ overlay: isOverlay }">
      <template v-if="printData && !isOverlay">
        <h1 class="title">{{ printData.title }}</h1>
        <div class="patient-info">
          <span>姓名：{{ printData.patientName }}</span>
          <span>性别：{{ printData.gender }}</span>
          <span>年龄：{{ printData.age ?? '—' }}岁</span>
          <span>电话：{{ printData.phone || '—' }}</span>
        </div>
        <div class="patient-info">
          <span>住址：{{ printData.address || '—' }}</span>
        </div>
        <div class="patient-info">
          <span>诊断：{{ printData.diagnosis || '—' }}</span>
          <span>日期：{{ printData.prescriptionDate }}</span>
        </div>
        <table class="items-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>药品名称</th>
              <th>规格</th>
              <th>数量</th>
              <th>单位</th>
              <th>用法</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in printData.items" :key="item.id ?? index">
              <td>{{ index + 1 }}</td>
              <td>{{ item.medicineName }}</td>
              <td>{{ item.specification || '—' }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ item.unit }}</td>
              <td>{{ item.usage || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <div class="signature">{{ printData.doctorSignatureLabel }}</div>
      </template>

      <template v-else-if="printData && isOverlay && activeTemplate">
        <div
          v-for="field in activeTemplate.fields"
          :key="field.key"
          class="overlay-field"
          :style="{
            top: `${field.topMm}mm`,
            left: `${field.leftMm}mm`,
            fontSize: `${field.fontSizePt}pt`,
          }"
        >
          {{ fieldValue(field.key) }}
        </div>
        <div
          v-if="activeTemplate.itemsArea"
          class="overlay-items"
          :style="{
            top: `${activeTemplate.itemsArea.topMm}mm`,
            left: `${activeTemplate.itemsArea.leftMm}mm`,
            fontSize: `${activeTemplate.itemsArea.fontSizePt}pt`,
            lineHeight: `${activeTemplate.itemsArea.lineHeightMm}mm`,
          }"
        >
          <div v-if="activeTemplate.itemsArea.rpLabel" class="rp-label">{{ activeTemplate.itemsArea.rpLabel }}</div>
          <div v-for="(item, index) in printData.items" :key="item.id ?? index" class="item-line">
            {{ itemLine(item, index) }}
          </div>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
  background: #f5f7fa;
}

.toolbar {
  max-width: 210mm;
  margin: 0 auto 16px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.template-hint {
  color: #909399;
  font-size: 13px;
}

.print-sheet {
  position: relative;
  width: 210mm;
  max-width: 210mm;
  min-height: 297mm;
  margin: 0 auto;
  padding: 20mm 18mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  color: #000;
  font-size: 14px;
}

.print-sheet.overlay {
  padding: 0;
  min-height: 297mm;
}

.title {
  text-align: center;
  font-size: 22px;
  margin: 0 0 20px;
  font-weight: 700;
}

.patient-info {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  margin-bottom: 10px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  margin: 20px 0;
}

.items-table th,
.items-table td {
  border: 1px solid #333;
  padding: 8px 6px;
  text-align: left;
}

.signature {
  margin-top: 48px;
  text-align: right;
  padding-right: 40px;
}

.overlay-field,
.overlay-items {
  position: absolute;
  white-space: nowrap;
}

.overlay-items {
  white-space: normal;
  max-width: 170mm;
}

.rp-label {
  margin-bottom: 2mm;
  font-weight: 600;
}

.item-line {
  margin-bottom: 1mm;
}

@media print {
  @page {
    size: A4;
    margin: 0;
  }

  .page {
    padding: 0;
    background: #fff;
  }

  .no-print {
    display: none !important;
  }

  .print-sheet {
    box-shadow: none;
    max-width: none;
    margin: 0;
    padding: 12mm 15mm;
  }

  .print-sheet.overlay {
    padding: 0;
  }
}
</style>
