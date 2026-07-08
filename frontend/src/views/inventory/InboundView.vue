<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inbound } from '@/api/inventory'
import { searchMedicines } from '@/api/medicine'
import type { MedicineListItem } from '@/types/medicine'

const router = useRouter()
const saving = ref(false)
const medicineOptions = ref<MedicineListItem[]>([])

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
        <div class="header-row">
          <span>手动入库</span>
          <el-button @click="router.push('/')">返回首页</el-button>
        </div>
      </template>
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
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
</style>
