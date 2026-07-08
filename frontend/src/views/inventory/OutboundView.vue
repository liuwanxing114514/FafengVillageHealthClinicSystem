<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmOutbound, previewOutbound } from '@/api/inventory'
import { getPrescription } from '@/api/prescription'
import BarcodeScanPanel from '@/components/inventory/BarcodeScanPanel.vue'
import type { OutboundPreview, OutboundPreviewLine } from '@/types/inventory'
import type { MedicineListItem } from '@/types/medicine'
import type { PrescriptionDetail } from '@/types/prescription'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const prescription = ref<PrescriptionDetail | null>(null)
const preview = ref<OutboundPreview | null>(null)
const selectedLine = ref<OutboundPreviewLine | null>(null)

const allocations = reactive<Record<number, number>>({})

const prescriptionId = computed(() => Number(route.query.prescriptionId) || null)

async function load() {
  if (!prescriptionId.value) return
  loading.value = true
  try {
    prescription.value = await getPrescription(prescriptionId.value)
    preview.value = await previewOutbound({
      patientId: prescription.value.patientId,
      prescriptionId: prescription.value.id,
      items: prescription.value.items.map((i) => ({
        medicineId: i.medicineId,
        quantity: Number(i.quantity),
        unit: i.unit,
      })),
    })
    if (!preview.value.sufficient) {
      ElMessage.warning('部分药品库存不足，无法出库')
    }
  } finally {
    loading.value = false
  }
}

function selectLine(line: OutboundPreviewLine) {
  selectedLine.value = line
  for (const a of line.recommendedAllocations) {
    allocations[a.batchId] = Number(a.recommendedQuantity)
  }
}

function onBarcodeMatched(medicine: MedicineListItem) {
  if (!preview.value) {
    ElMessage.warning('请先加载处方出库预览')
    return
  }
  const line = preview.value.lines.find((l) => l.medicineId === medicine.id)
  if (!line) {
    ElMessage.warning('该药品不在当前处方中')
    return
  }
  if (!line.sufficient) {
    ElMessage.warning(`${line.medicineName} 库存不足`)
    return
  }
  selectLine(line)
  ElMessage.success(`已选中 ${line.medicineName}，请确认批次出库`)
}

async function confirmLine(line: OutboundPreviewLine) {
  if (!prescription.value || !line.sufficient) return
  const allocs = line.recommendedAllocations
    .filter((a) => (allocations[a.batchId] ?? 0) > 0)
    .map((a) => ({ batchId: a.batchId, quantity: allocations[a.batchId] ?? a.recommendedQuantity }))
  if (!allocs.length) {
    ElMessage.warning('请确认批次数量')
    return
  }
  saving.value = true
  try {
    await confirmOutbound({
      patientId: prescription.value.patientId,
      prescriptionId: prescription.value.id,
      medicineId: line.medicineId,
      allocations: allocs,
    })
    ElMessage.success(`${line.medicineName} 出库成功`)
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span>手动出库</span>
          <el-button @click="router.push('/')">返回首页</el-button>
        </div>
      </template>

      <p v-if="!prescriptionId" class="hint">请从处方页进入出库，或带参数 ?prescriptionId=</p>

      <template v-if="prescription">
        <p>患者：{{ prescription.patientName }} · 处方 #{{ prescription.id }}</p>

        <BarcodeScanPanel @matched="onBarcodeMatched" />

        <el-table :data="preview?.lines ?? []" @row-click="selectLine">
          <el-table-column prop="medicineName" label="药品" />
          <el-table-column label="需求">
            <template #default="{ row }">{{ row.requestedQuantity }} {{ row.unit }}</template>
          </el-table-column>
          <el-table-column label="库存">
            <template #default="{ row }">
              <el-tag :type="row.sufficient ? 'success' : 'danger'">
                {{ row.sufficient ? '充足' : '不足' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="!row.sufficient" @click.stop="selectLine(row)">选批次</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedLine" class="batch-panel">
          <h3>FEFO 推荐批次 — {{ selectedLine.medicineName }}</h3>
          <el-table :data="selectedLine.recommendedAllocations" size="small">
            <el-table-column prop="batchNo" label="批号" />
            <el-table-column prop="expiryDate" label="有效期" />
            <el-table-column label="可用">
              <template #default="{ row }">{{ row.availableQuantity }}</template>
            </el-table-column>
            <el-table-column label="出库数量">
              <template #default="{ row }">
                <el-input-number v-model="allocations[row.batchId]" :min="0" :max="row.availableQuantity" :precision="3" size="small" />
              </template>
            </el-table-column>
          </el-table>
          <el-button type="primary" :loading="saving" style="margin-top: 12px" @click="confirmLine(selectedLine)">
            确认出库
          </el-button>
        </div>
      </template>
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; }
.hint { color: #909399; }
.batch-panel { margin-top: 20px; }
</style>
