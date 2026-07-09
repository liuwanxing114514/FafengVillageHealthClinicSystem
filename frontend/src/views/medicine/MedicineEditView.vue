<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addBarcode,
  addConversion,
  createMedicine,
  deleteBarcode,
  deleteConversion,
  deleteMedicine,
  getMedicine,
  updateMedicine,
} from '@/api/medicine'
import { useTabTitle } from '@/composables/useTabTitle'
import type { BarcodeItem, ConversionItem, MedicineDetail } from '@/types/medicine'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const detail = ref<MedicineDetail | null>(null)

const isNew = computed(() => route.params.id === 'new')
const medicineId = computed(() => (isNew.value ? null : Number(route.params.id)))

const form = reactive({
  name: '',
  genericName: '',
  dosageForm: '',
  specification: '',
  baseUnit: '片',
  packageUnit: '盒',
  manufacturer: '',
  purchasePrice: 0,
  stockThreshold: null as number | null,
  remark: '',
})

useTabTitle(computed(() => {
  if (isNew.value) return '新建药品'
  return form.name ? `药品：${form.name}` : ''
}))

const conversionForm = reactive({
  fromUnit: '',
  toUnit: '',
  factor: 1,
})

const barcodeForm = reactive({
  barcode: '',
  remark: '',
})

async function loadDetail() {
  if (isNew.value || !medicineId.value) return
  loading.value = true
  try {
    detail.value = await getMedicine(medicineId.value)
    Object.assign(form, {
      name: detail.value.name,
      genericName: detail.value.genericName,
      dosageForm: detail.value.dosageForm,
      specification: detail.value.specification,
      baseUnit: detail.value.baseUnit,
      packageUnit: detail.value.packageUnit,
      manufacturer: detail.value.manufacturer,
      purchasePrice: Number(detail.value.purchasePrice),
      stockThreshold: Number(detail.value.stockThreshold),
      remark: detail.value.remark,
    })
    conversionForm.fromUnit = detail.value.packageUnit
    conversionForm.toUnit = detail.value.baseUnit
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.name.trim() || !form.baseUnit.trim()) {
    ElMessage.warning('请填写药品名称和基本单位')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      genericName: form.genericName.trim(),
      dosageForm: form.dosageForm.trim(),
      specification: form.specification.trim(),
      baseUnit: form.baseUnit.trim(),
      packageUnit: form.packageUnit.trim(),
      manufacturer: form.manufacturer.trim(),
      purchasePrice: form.purchasePrice,
      stockThreshold: form.stockThreshold,
      remark: form.remark.trim(),
    }
    if (isNew.value) {
      const created = await createMedicine(payload)
      ElMessage.success('药品已创建')
      await router.replace(`/medicine/${created.id}`)
    } else if (medicineId.value) {
      detail.value = await updateMedicine(medicineId.value, payload)
      ElMessage.success('已保存')
    }
  } finally {
    saving.value = false
  }
}

async function onAddConversion() {
  if (!medicineId.value) return
  if (!conversionForm.fromUnit || !conversionForm.toUnit || conversionForm.factor < 1) {
    ElMessage.warning('请填写完整的单位换算')
    return
  }
  await addConversion(medicineId.value, { ...conversionForm })
  ElMessage.success('换算已添加')
  await loadDetail()
}

async function onDeleteConversion(row: ConversionItem) {
  if (!medicineId.value) return
  await deleteConversion(medicineId.value, row.id)
  ElMessage.success('已删除')
  await loadDetail()
}

async function onAddBarcode() {
  if (!medicineId.value) return
  if (!barcodeForm.barcode.trim()) {
    ElMessage.warning('请填写条码')
    return
  }
  await addBarcode(medicineId.value, {
    barcode: barcodeForm.barcode.trim(),
    remark: barcodeForm.remark.trim(),
  })
  barcodeForm.barcode = ''
  barcodeForm.remark = ''
  ElMessage.success('条码已添加')
  await loadDetail()
}

async function onDeleteBarcode(row: BarcodeItem) {
  if (!medicineId.value) return
  await deleteBarcode(medicineId.value, row.id)
  ElMessage.success('已删除')
  await loadDetail()
}

async function onDelete() {
  if (!medicineId.value || !detail.value) return
  if (detail.value.status !== 'INACTIVE') {
    ElMessage.warning('请先停用药品后再删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除「${form.name}」吗？删除后不可恢复，条码将被释放。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteMedicine(medicineId.value)
    ElMessage.success('已删除')
    await router.replace('/medicine')
  } catch {
    // cancelled or failed
  }
}

const thresholdHint = computed(() => {
  if (!detail.value) {
    return '留空则默认按 5 盒等价量（基本单位）计算'
  }
  return `当前约 ${detail.value.stockThresholdInPackages} ${detail.value.packageUnit}`
})

onMounted(loadDetail)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="hover">
      <template #header>
        <div class="header-row">
          <span class="title">{{ isNew ? '新增药品' : '编辑药品' }}</span>
          <el-button link type="primary" @click="router.push('/medicine')">返回列表</el-button>
        </div>
      </template>

      <h3 class="section">基本信息</h3>
      <el-form label-width="110px" class="form-grid">
        <el-form-item label="药品名称" required>
          <el-input v-model="form.name" placeholder="如：阿莫西林胶囊" />
        </el-form-item>
        <el-form-item label="通用名">
          <el-input v-model="form.genericName" />
        </el-form-item>
        <el-form-item label="剂型">
          <el-input v-model="form.dosageForm" placeholder="如：胶囊剂" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.specification" placeholder="如：0.25g×24粒" />
        </el-form-item>
        <el-form-item label="基本单位" required>
          <el-input v-model="form.baseUnit" placeholder="片/粒/支" style="width: 120px" />
        </el-form-item>
        <el-form-item label="包装单位">
          <el-input v-model="form.packageUnit" placeholder="盒/瓶" style="width: 120px" />
        </el-form-item>
        <el-form-item label="生产厂家">
          <el-input v-model="form.manufacturer" />
        </el-form-item>
        <el-form-item label="进货单价">
          <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存下限">
          <el-input-number
            v-model="form.stockThreshold"
            :min="0"
            :precision="3"
            placeholder="基本单位"
          />
          <span class="hint">{{ thresholdHint }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>

      <template v-if="!isNew && detail">
        <el-divider />

        <h3 class="section">单位换算</h3>
        <p class="hint-block">整数换算，如 1 盒 = 12 片。液体容量只写在规格中，不按毫升拆零。</p>
        <el-table :data="detail.conversions" stripe size="small" style="margin-bottom: 12px">
          <el-table-column label="换算" min-width="200">
            <template #default="{ row }">
              1 {{ row.fromUnit }} = {{ row.factor }} {{ row.toUnit }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" @click="onDeleteConversion(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="inline-form">
          <el-input v-model="conversionForm.fromUnit" placeholder="从单位" style="width: 100px" />
          <span>=</span>
          <el-input-number v-model="conversionForm.factor" :min="1" />
          <el-input v-model="conversionForm.toUnit" placeholder="到单位" style="width: 100px" />
          <el-button @click="onAddConversion">添加换算</el-button>
        </div>

        <el-divider />

        <h3 class="section">条码</h3>
        <el-table :data="detail.barcodes" stripe size="small" style="margin-bottom: 12px">
          <el-table-column prop="barcode" label="条码" min-width="160" />
          <el-table-column prop="remark" label="备注" min-width="120" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" @click="onDeleteBarcode(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="inline-form">
          <el-input v-model="barcodeForm.barcode" placeholder="条码" style="width: 200px" />
          <el-input v-model="barcodeForm.remark" placeholder="备注" style="width: 160px" />
          <el-button @click="onAddBarcode">添加条码</el-button>
        </div>

        <p v-if="detail.pinyinAbbr" class="hint-block">拼音缩写：{{ detail.pinyinAbbr }}</p>

        <el-divider />
        <el-button
          type="danger"
          plain
          :disabled="detail.status !== 'INACTIVE'"
          @click="onDelete"
        >
          删除药品
        </el-button>
        <p v-if="detail.status !== 'INACTIVE'" class="hint-block">需先停用后才能删除。</p>
      </template>

      <p v-if="isNew" class="hint-block">保存后可继续维护单位换算和条码。</p>
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
  font-size: 1.25rem;
  font-weight: 600;
}

.section {
  margin: 0 0 12px;
  font-size: 1rem;
}

.form-grid {
  max-width: 640px;
}

.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 0.85rem;
}

.hint-block {
  margin: 0 0 12px;
  color: #909399;
  font-size: 0.9rem;
}

.inline-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>
