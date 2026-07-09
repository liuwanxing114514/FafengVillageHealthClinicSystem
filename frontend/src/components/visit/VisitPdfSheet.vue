<script setup lang="ts">
import { formatDisplayDate, genderLabel } from '@/utils/savePdf'

defineProps<{
  patientName: string
  patientGender?: string | null
  patientAge?: number | null
  visitTime?: string
  chiefComplaint?: string
  presentIllness?: string
  pastHistory?: string
  temperature?: number | null
  bloodPressure?: string
  spo2?: number | null
  etco2?: number | null
  heartRate?: number | null
  pulse?: string
  allergyHistory?: string
  diagnosis?: string
  treatment?: string
  remark?: string
  amountDue?: number | null
  amountPaid?: number | null
}>()

function display(value?: string | null) {
  return value?.trim() ? value.trim() : '—'
}

function displayNumber(value?: number | null, suffix = '') {
  if (value == null || Number.isNaN(value)) return '—'
  return `${value}${suffix}`
}

function displayMoney(value?: number | null) {
  if (value == null || Number.isNaN(value)) return '—'
  return `¥${value.toFixed(2)}`
}
</script>

<template>
  <section class="print-sheet visit-pdf-sheet">
    <h1 class="title">发凤村卫生室 门诊病历</h1>

    <div class="info-row">
      <span>姓名：{{ patientName || '—' }}</span>
      <span>性别：{{ genderLabel(patientGender) }}</span>
      <span>年龄：{{ patientAge != null ? `${patientAge}岁` : '—' }}</span>
      <span>就诊时间：{{ formatDisplayDate(visitTime) }}</span>
    </div>

    <div class="section">
      <div class="label">主诉</div>
      <div class="content">{{ display(chiefComplaint) }}</div>
    </div>
    <div class="section">
      <div class="label">现病史</div>
      <div class="content">{{ display(presentIllness) }}</div>
    </div>
    <div class="section">
      <div class="label">既往史</div>
      <div class="content">{{ display(pastHistory) }}</div>
    </div>

    <div class="info-row vitals">
      <span>体温：{{ displayNumber(temperature, '℃') }}</span>
      <span>血压：{{ display(bloodPressure) }}</span>
      <span>血氧：{{ displayNumber(spo2, '%') }}</span>
      <span>呼末CO₂：{{ displayNumber(etco2) }}</span>
      <span>心率：{{ displayNumber(heartRate, '次/分') }}</span>
      <span>脉象：{{ display(pulse) }}</span>
    </div>

    <div class="section">
      <div class="label">过敏史</div>
      <div class="content">{{ display(allergyHistory) }}</div>
    </div>
    <div class="section">
      <div class="label">诊断</div>
      <div class="content">{{ display(diagnosis) }}</div>
    </div>
    <div class="section">
      <div class="label">处理意见</div>
      <div class="content">{{ display(treatment) }}</div>
    </div>
    <div class="section">
      <div class="label">备注</div>
      <div class="content">{{ display(remark) }}</div>
    </div>

    <div class="fee-row">
      <span>应收：{{ displayMoney(amountDue) }}</span>
      <span>实收：{{ displayMoney(amountPaid) }}</span>
    </div>
  </section>
</template>

<style scoped>
.visit-pdf-sheet {
  width: 210mm;
  min-height: 297mm;
  padding: 18mm 16mm;
  background: #fff;
  color: #000;
  font-size: 14px;
  line-height: 1.6;
  box-sizing: border-box;
}

.title {
  text-align: center;
  font-size: 22px;
  margin: 0 0 20px;
  font-weight: 700;
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
  margin-bottom: 14px;
}

.vitals {
  padding: 8px 0;
  border-top: 1px solid #ddd;
  border-bottom: 1px solid #ddd;
  margin-bottom: 14px;
}

.section {
  margin-bottom: 12px;
}

.label {
  font-weight: 600;
  margin-bottom: 4px;
}

.content {
  white-space: pre-wrap;
  min-height: 1.2em;
}

.fee-row {
  margin-top: 24px;
  display: flex;
  gap: 32px;
  font-weight: 600;
}
</style>
