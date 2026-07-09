<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPrescriptionPrint } from '@/api/prescription'
import type { PrescriptionPrintData } from '@/types/prescription'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const printData = ref<PrescriptionPrintData | null>(null)

const prescriptionId = Number(route.params.id)

async function loadPrint() {
  if (!prescriptionId) {
    ElMessage.error('无效的处方')
    router.push('/patient')
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
    </div>

    <section v-loading="loading" class="print-sheet">
      <template v-if="printData">
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
}

.print-sheet {
  max-width: 210mm;
  min-height: 297mm;
  margin: 0 auto;
  padding: 20mm 18mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  color: #000;
  font-size: 14px;
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

@media print {
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
}
</style>
