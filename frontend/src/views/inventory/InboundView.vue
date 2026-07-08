<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inbound } from '@/api/inventory'
import { searchMedicines } from '@/api/medicine'
import BarcodeScanPanel from '@/components/inventory/BarcodeScanPanel.vue'
import type { MedicineListItem } from '@/types/medicine'

const router = useRouter()
const saving = ref(false)
const medicineOptions = ref<MedicineListItem[]>([])
const scanDialogVisible = ref(false)
const scannedMedicine = ref<MedicineListItem | null>(null)

const form = reactive({
  medicineId: null as number | null,
  baseUnit: '',
  quantity: null as number | null,
  unit: '',
  batchNo: '',
  expiryDate: '',
  purchasePrice: null as number | null,
  supplier: '',
  remark: '',
})

const scanForm = reactive({
  quantity: null as number | null,
  unit: '',
  batchNo: '',
  expiryDate: '',
  purchasePrice: null as number | null,
  supplier: '',
  remark: '',
})

async function searchMedicineOptions(keyword: string) {
  if (!keyword.trim()) return
  const page = await searchMedicines({ keyword: keyword.trim(), page: 1, size: 20 })
  medicineOptions.value = page.records
}

function onMedicineSelect(id: number) {
  const med = medicineOptions.value.find((m) => m.id === id)
  if (med) {
    form.baseUnit = med.baseUnit
    form.unit = med.baseUnit
  }
}

function onBarcodeMatched(medicine: MedicineListItem) {
  scannedMedicine.value = medicine
  scanForm.quantity = null
  scanForm.unit = medicine.baseUnit
  scanForm.batchNo = ''
  scanForm.expiryDate = ''
  scanForm.purchasePrice = null
  scanForm.supplier = ''
  scanForm.remark = ''
  scanDialogVisible.value = true
}

async function onScanSubmit() {
  if (!scannedMedicine.value || !scanForm.quantity || !scanForm.batchNo.trim()) {
    ElMessage.warning('请填写数量、批号')
    return
  }
  saving.value = true
  try {
    await inbound({
      medicineId: scannedMedicine.value.id,
      quantity: scanForm.quantity,
      unit: scanForm.unit || scannedMedicine.value.baseUnit,
      batchNo: scanForm.batchNo.trim(),
      expiryDate: scanForm.expiryDate || undefined,
      purchasePrice: scanForm.purchasePrice ?? undefined,
      supplier: scanForm.supplier.trim() || undefined,
      remark: scanForm.remark.trim() || undefined,
    })
    ElMessage.success('入库成功')
    scanDialogVisible.value = false
    router.push('/inventory/flows')
  } finally {
    saving.value = false
  }
}

async function onSubmit() {
  if (!form.medicineId || !form.quantity || !form.batchNo.trim()) {
    ElMessage.warning('请填写药品、数量、批号')
    return
  }
  saving.value = true
  try {
    await inbound({
      medicineId: form.medicineId,
      quantity: form.quantity,
      unit: form.unit || form.baseUnit,
      batchNo: form.batchNo.trim(),
      expiryDate: form.expiryDate || undefined,
      purchasePrice: form.purchasePrice ?? undefined,
      supplier: form.supplier.trim() || undefined,
      remark: form.remark.trim() || undefined,
    })
    ElMessage.success('入库成功')
    router.push('/inventory/flows')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="page">
    <el-card shadow="never">
      <template #header>
        <span>手动入库</span>
      </template>

      <BarcodeScanPanel @matched="onBarcodeMatched" />

      <el-form label-width="96px" style="max-width: 520px">
        <el-form-item label="药品">
          <el-select
            v-model="form.medicineId"
            filterable
            remote
            :remote-method="searchMedicineOptions"
            placeholder="搜索药品"
            style="width: 100%"
            @change="onMedicineSelect"
          >
            <el-option
              v-for="m in medicineOptions"
              :key="m.id"
              :label="`${m.name} ${m.specification ?? ''}`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批号">
          <el-input v-model="form.batchNo" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.quantity" :min="0.001" :precision="3" />
          <el-input v-model="form.unit" style="width: 80px; margin-left: 8px" :placeholder="form.baseUnit || '单位'" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="form.expiryDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="进货单价">
          <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.supplier" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSubmit">确认入库</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog
      v-model="scanDialogVisible"
      :title="scannedMedicine ? `扫码入库 — ${scannedMedicine.name}` : '扫码入库'"
      width="480px"
    >
      <el-form v-if="scannedMedicine" label-width="96px">
        <el-form-item label="规格">
          <span>{{ scannedMedicine.specification ?? '—' }}</span>
        </el-form-item>
        <el-form-item label="批号" required>
          <el-input v-model="scanForm.batchNo" placeholder="请输入批号" />
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="scanForm.quantity" :min="0.001" :precision="3" />
          <el-input
            v-model="scanForm.unit"
            style="width: 80px; margin-left: 8px"
            :placeholder="scannedMedicine.baseUnit"
          />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="scanForm.expiryDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="进货单价">
          <el-input-number v-model="scanForm.purchasePrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="scanForm.supplier" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scanForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scanDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onScanSubmit">确认入库</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
</style>
